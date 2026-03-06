package com.examples.deposit.integration

import com.examples.deposit.domain.AccountBlockStatus
import com.examples.deposit.domain.BlockCode
import com.examples.deposit.domain.BlockRequestedBy
import com.examples.deposit.domain.DemandDepositAccountBlock
import com.examples.deposit.domain.exception.TransactionNotAllowedForAccountStatusException
import com.examples.deposit.repository.AccountCreationIdempotencyRepository
import com.examples.deposit.repository.AccountTransactionIdempotencyRepository
import com.examples.deposit.repository.DemandDepositAccountBlockRepository
import com.examples.deposit.repository.DemandDepositAccountRepository
import com.examples.deposit.repository.DemandDepositAccountTransactionRepository
import com.examples.deposit.service.DemandDepositAccountTransactionService
import com.examples.deposit.service.dto.PostCreditTransactionCommand
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import spock.lang.Specification

import java.time.LocalDate
import java.util.UUID

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class DemandDepositAccountTransactionIntegrationSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    DemandDepositAccountRepository demandDepositAccountRepository

    @Autowired
    DemandDepositAccountBlockRepository demandDepositAccountBlockRepository

    @Autowired
    DemandDepositAccountTransactionRepository demandDepositAccountTransactionRepository

    @Autowired
    AccountTransactionIdempotencyRepository accountTransactionIdempotencyRepository

    @Autowired
    AccountCreationIdempotencyRepository accountCreationIdempotencyRepository

    @Autowired
    DemandDepositAccountTransactionService demandDepositAccountTransactionService

    def setup() {
        accountTransactionIdempotencyRepository.deleteAll()
        demandDepositAccountTransactionRepository.deleteAll()
        demandDepositAccountBlockRepository.deleteAll()
        accountCreationIdempotencyRepository.deleteAll()
        demandDepositAccountRepository.deleteAll()
    }

    def cleanup() {
        accountTransactionIdempotencyRepository.deleteAll()
        demandDepositAccountTransactionRepository.deleteAll()
        demandDepositAccountBlockRepository.deleteAll()
        accountCreationIdempotencyRepository.deleteAll()
        demandDepositAccountRepository.deleteAll()
    }

    def "active debit-disabled block with validation-required code rejects debit and keeps balances unchanged"() {
        given:
        UUID customerId = UUID.fromString("31000000-0000-0000-0000-000000000001")
        UUID accountId = createAndActivateAccount(customerId, "it-tx-block-debit-create-001")

        postCredit(customerId, accountId, "50.00", "SAL", "it-seed-credit-ref-001", "it-seed-credit-idem-001")
            .andExpect(status().isCreated())

        demandDepositAccountBlockRepository.saveAndFlush(
            DemandDepositAccountBlock.create(
                accountId,
                BlockCode.ADB,
                BlockRequestedBy.BANK,
                AccountBlockStatus.ACTIVE,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1),
                "active debit restriction"
            )
        )

        long transactionRowsBefore = demandDepositAccountTransactionRepository.count()
        long idempotencyRowsBefore = accountTransactionIdempotencyRepository.count()

        when:
        def result = postDebit(customerId, accountId, "20.00", "ATM", "it-blocked-debit-ref-001", "it-blocked-debit-idem-001")

        then:
        result.andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value('deposit/transaction-blocked'))
            .andExpect(jsonPath('$.status').value(422))

        and:
        def reloaded = demandDepositAccountRepository.findById(accountId).orElseThrow()
        reloaded.currentBalance.compareTo(new BigDecimal("50.00")) == 0
        reloaded.availableBalance.compareTo(new BigDecimal("50.00")) == 0
        demandDepositAccountTransactionRepository.count() == transactionRowsBefore
        accountTransactionIdempotencyRepository.count() == idempotencyRowsBefore
    }

    def "active credit-disabled block with validation-required code rejects credit and keeps balances unchanged"() {
        given:
        UUID customerId = UUID.fromString("31000000-0000-0000-0000-000000000002")
        UUID accountId = createAndActivateAccount(customerId, "it-tx-block-credit-create-001")

        demandDepositAccountBlockRepository.saveAndFlush(
            DemandDepositAccountBlock.create(
                accountId,
                BlockCode.ACB,
                BlockRequestedBy.BANK,
                AccountBlockStatus.ACTIVE,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1),
                "active credit restriction"
            )
        )

        long transactionRowsBefore = demandDepositAccountTransactionRepository.count()
        long idempotencyRowsBefore = accountTransactionIdempotencyRepository.count()

        when:
        def result = postCredit(customerId, accountId, "35.00", "SAL", "it-blocked-credit-ref-001", "it-blocked-credit-idem-001")

        then:
        result.andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value('deposit/transaction-blocked'))
            .andExpect(jsonPath('$.status').value(422))

        and:
        def reloaded = demandDepositAccountRepository.findById(accountId).orElseThrow()
        reloaded.currentBalance.compareTo(BigDecimal.ZERO) == 0
        reloaded.availableBalance.compareTo(BigDecimal.ZERO) == 0
        demandDepositAccountTransactionRepository.count() == transactionRowsBefore
        accountTransactionIdempotencyRepository.count() == idempotencyRowsBefore
    }

    def "insufficient available balance rejects debit and keeps balances unchanged"() {
        given:
        UUID customerId = UUID.fromString("31000000-0000-0000-0000-000000000003")
        UUID accountId = createAndActivateAccount(customerId, "it-tx-insufficient-create-001")

        postCredit(customerId, accountId, "10.00", "SAL", "it-insufficient-seed-ref-001", "it-insufficient-seed-idem-001")
            .andExpect(status().isCreated())

        long transactionRowsBefore = demandDepositAccountTransactionRepository.count()
        long idempotencyRowsBefore = accountTransactionIdempotencyRepository.count()

        when:
        def result = postDebit(customerId, accountId, "11.00", "ATM", "it-insufficient-debit-ref-001", "it-insufficient-debit-idem-001")

        then:
        result.andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value('deposit/insufficient-available-balance'))
            .andExpect(jsonPath('$.status').value(422))

        and:
        def reloaded = demandDepositAccountRepository.findById(accountId).orElseThrow()
        reloaded.currentBalance.compareTo(new BigDecimal("10.00")) == 0
        reloaded.availableBalance.compareTo(new BigDecimal("10.00")) == 0
        demandDepositAccountTransactionRepository.count() == transactionRowsBefore
        accountTransactionIdempotencyRepository.count() == idempotencyRowsBefore
    }

    def "pending verification debit is rejected with status-not-allowed problem and keeps transaction state unchanged"() {
        given:
        UUID customerId = UUID.fromString("31000000-0000-0000-0000-000000000006")
        UUID accountId = createAccount(customerId, "it-tx-status-pending-create-001")

        long transactionRowsBefore = demandDepositAccountTransactionRepository.count()
        long idempotencyRowsBefore = accountTransactionIdempotencyRepository.count()
        def beforeAccount = demandDepositAccountRepository.findById(accountId).orElseThrow()

        when:
        def result = postDebit(customerId, accountId, "5.00", "ATM", "it-pending-debit-ref-001", "it-pending-debit-idem-001")

        then:
        result.andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value('deposit/transaction-not-allowed-for-account-status'))
            .andExpect(jsonPath('$.status').value(422))

        and:
        def reloaded = demandDepositAccountRepository.findById(accountId).orElseThrow()
        reloaded.currentBalance.compareTo(beforeAccount.currentBalance) == 0
        reloaded.availableBalance.compareTo(beforeAccount.availableBalance) == 0
        demandDepositAccountTransactionRepository.count() == transactionRowsBefore
        accountTransactionIdempotencyRepository.count() == idempotencyRowsBefore
    }

    def "pending verification allows allowlisted credit code and rejects non-allowlisted credit code"() {
        given:
        UUID customerId = UUID.fromString("31000000-0000-0000-0000-000000000007")
        UUID accountId = createAccount(customerId, "it-tx-pending-code-create-001")
        long transactionRowsBefore = demandDepositAccountTransactionRepository.count()
        long idempotencyRowsBefore = accountTransactionIdempotencyRepository.count()

        when:
        def allowedResult = demandDepositAccountTransactionService.postCredit(
            new PostCreditTransactionCommand(
                accountId,
                customerId,
                new BigDecimal("15.00"),
                "CASH_DEPOSIT",
                "it-pending-allowed-credit-ref-001",
                "it-pending-allowed-credit-idem-001"
            )
        )

        then:
        allowedResult.transactionCode() == "CASH_DEPOSIT"
        allowedResult.currentBalance().compareTo(new BigDecimal("15.00")) == 0
        allowedResult.availableBalance().compareTo(new BigDecimal("15.00")) == 0
        demandDepositAccountTransactionRepository.count() == transactionRowsBefore + 1
        accountTransactionIdempotencyRepository.count() == idempotencyRowsBefore + 1

        when:
        demandDepositAccountTransactionService.postCredit(
            new PostCreditTransactionCommand(
                accountId,
                customerId,
                new BigDecimal("5.00"),
                "FAST_TRANSFER",
                "it-pending-disallowed-credit-ref-001",
                "it-pending-disallowed-credit-idem-001"
            )
        )

        then:
        thrown(TransactionNotAllowedForAccountStatusException)

        and:
        def reloaded = demandDepositAccountRepository.findById(accountId).orElseThrow()
        reloaded.currentBalance.compareTo(new BigDecimal("15.00")) == 0
        reloaded.availableBalance.compareTo(new BigDecimal("15.00")) == 0
        demandDepositAccountTransactionRepository.count() == transactionRowsBefore + 1
        accountTransactionIdempotencyRepository.count() == idempotencyRowsBefore + 1
    }

    def "eligible account valid credit and debit mutate balances correctly"() {
        given:
        UUID customerId = UUID.fromString("31000000-0000-0000-0000-000000000004")
        UUID accountId = createAndActivateAccount(customerId, "it-tx-happy-create-001")

        when:
        def creditResult = postCredit(customerId, accountId, "100.00", "SAL", "it-happy-credit-ref-001", "it-happy-credit-idem-001")
        def debitResult = postDebit(customerId, accountId, "40.00", "ATM", "it-happy-debit-ref-001", "it-happy-debit-idem-001")

        then:
        creditResult.andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.transactionType').value('CREDIT'))
            .andExpect(jsonPath('$.currentBalance').value(100.0))
            .andExpect(jsonPath('$.availableBalance').value(100.0))

        debitResult.andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.transactionType').value('DEBIT'))
            .andExpect(jsonPath('$.currentBalance').value(60.0))
            .andExpect(jsonPath('$.availableBalance').value(60.0))

        and:
        def reloaded = demandDepositAccountRepository.findById(accountId).orElseThrow()
        reloaded.currentBalance.compareTo(new BigDecimal("60.00")) == 0
        reloaded.availableBalance.compareTo(new BigDecimal("60.00")) == 0
        demandDepositAccountTransactionRepository.count() == 2
        accountTransactionIdempotencyRepository.count() == 2
    }

    def "duplicate idempotency replay does not create duplicate posting or duplicate balance mutation"() {
        given:
        UUID customerId = UUID.fromString("31000000-0000-0000-0000-000000000005")
        UUID accountId = createAndActivateAccount(customerId, "it-tx-replay-create-001")

        String referenceId = "it-replay-credit-ref-001"
        String idempotencyKey = "it-replay-credit-idem-001"

        when:
        def first = postCredit(customerId, accountId, "25.00", "SAL", referenceId, idempotencyKey)
        def replay = postCredit(customerId, accountId, "25.00", "SAL", referenceId, idempotencyKey)

        then:
        first.andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.currentBalance').value(25.0))
            .andExpect(jsonPath('$.availableBalance').value(25.0))

        replay.andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.currentBalance').value(25.0))
            .andExpect(jsonPath('$.availableBalance').value(25.0))

        and:
        demandDepositAccountTransactionRepository.count() == 1
        accountTransactionIdempotencyRepository.count() == 1
        def reloaded = demandDepositAccountRepository.findById(accountId).orElseThrow()
        reloaded.currentBalance.compareTo(new BigDecimal("25.00")) == 0
        reloaded.availableBalance.compareTo(new BigDecimal("25.00")) == 0
    }

    private UUID createAndActivateAccount(UUID customerId, String idempotencyKey) {
        createAccount(customerId, idempotencyKey)
        def account = demandDepositAccountRepository.findByCustomerId(customerId).orElseThrow()
        account.activate()
        return demandDepositAccountRepository.saveAndFlush(account).id
    }

    private UUID createAccount(UUID customerId, String idempotencyKey) {
        mockMvc.perform(post('/demand-deposit-accounts')
            .contentType(MediaType.APPLICATION_JSON)
            .header('x-customer-id', customerId.toString())
            .content('{"idempotencyKey":"' + idempotencyKey + '"}'))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.customerId').value(customerId.toString()))
        return demandDepositAccountRepository.findByCustomerId(customerId).orElseThrow().id
    }

    private ResultActions postCredit(
        UUID customerId,
        UUID accountId,
        String amount,
        String transactionCode,
        String referenceId,
        String idempotencyKey
    ) {
        return mockMvc.perform(post('/transactions/credit')
            .contentType(MediaType.APPLICATION_JSON)
            .header('x-customer-id', customerId.toString())
            .content('''
                {
                  "accountId": "''' + accountId + '''",
                  "amount": ''' + amount + ''',
                  "transactionCode": "''' + transactionCode + '''",
                  "referenceId": "''' + referenceId + '''",
                  "idempotencyKey": "''' + idempotencyKey + '''"
                }
            '''))
    }

    private ResultActions postDebit(
        UUID customerId,
        UUID accountId,
        String amount,
        String transactionCode,
        String referenceId,
        String idempotencyKey
    ) {
        return mockMvc.perform(post('/transactions/debit')
            .contentType(MediaType.APPLICATION_JSON)
            .header('x-customer-id', customerId.toString())
            .content('''
                {
                  "accountId": "''' + accountId + '''",
                  "amount": ''' + amount + ''',
                  "transactionCode": "''' + transactionCode + '''",
                  "referenceId": "''' + referenceId + '''",
                  "idempotencyKey": "''' + idempotencyKey + '''"
                }
            '''))
    }
}
