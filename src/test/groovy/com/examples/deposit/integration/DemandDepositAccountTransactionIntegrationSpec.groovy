package com.examples.deposit.integration

import com.examples.deposit.domain.AccountBlockStatus
import com.examples.deposit.domain.BlockCode
import com.examples.deposit.domain.BlockRequestedBy
import com.examples.deposit.domain.DemandDepositAccountBlock
import com.examples.deposit.repository.AccountCreationIdempotencyRepository
import com.examples.deposit.repository.AccountTransactionIdempotencyRepository
import com.examples.deposit.repository.DemandDepositAccountBlockRepository
import com.examples.deposit.repository.DemandDepositAccountRepository
import com.examples.deposit.repository.DemandDepositAccountTransactionRepository
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
            .andExpect(jsonPath('$.title').value('Transaction blocked'))
            .andExpect(jsonPath('$.status').value(422))
            .andExpect(jsonPath('$.detail').value('Transaction is blocked by active account restrictions'))

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
            .andExpect(jsonPath('$.title').value('Transaction blocked'))
            .andExpect(jsonPath('$.status').value(422))
            .andExpect(jsonPath('$.detail').value('Transaction is blocked by active account restrictions'))

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
            .andExpect(jsonPath('$.title').value('Insufficient available balance'))
            .andExpect(jsonPath('$.status').value(422))
            .andExpect(jsonPath('$.detail').value('Available balance is insufficient for the requested debit'))

        and:
        def reloaded = demandDepositAccountRepository.findById(accountId).orElseThrow()
        reloaded.currentBalance.compareTo(new BigDecimal("10.00")) == 0
        reloaded.availableBalance.compareTo(new BigDecimal("10.00")) == 0
        demandDepositAccountTransactionRepository.count() == transactionRowsBefore
        accountTransactionIdempotencyRepository.count() == idempotencyRowsBefore
    }

    def "foreign customer cannot post credit or debit to another customer's account"() {
        given:
        UUID ownerCustomerId = UUID.fromString("31000000-0000-0000-0000-000000000008")
        UUID foreignCustomerId = UUID.fromString("31000000-0000-0000-0000-000000000009")
        UUID accountId = createAndActivateAccount(ownerCustomerId, "it-tx-foreign-owner-create-001")

        when:
        def creditResult = postCredit(
            foreignCustomerId,
            accountId,
            "10.00",
            "SAL",
            "it-foreign-credit-ref-001",
            "it-foreign-credit-idem-001"
        )
        def debitResult = postDebit(
            foreignCustomerId,
            accountId,
            "5.00",
            "ATM",
            "it-foreign-debit-ref-001",
            "it-foreign-debit-idem-001"
        )

        then:
        creditResult.andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value('deposit/account-not-found'))
            .andExpect(jsonPath('$.title').value('Account not found'))
            .andExpect(jsonPath('$.status').value(404))
            .andExpect(jsonPath('$.detail').value('Demand deposit account was not found'))

        debitResult.andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value('deposit/account-not-found'))
            .andExpect(jsonPath('$.title').value('Account not found'))
            .andExpect(jsonPath('$.status').value(404))
            .andExpect(jsonPath('$.detail').value('Demand deposit account was not found'))
    }

    def "credit and debit to non-existent account return 404 account-not-found"() {
        given:
        UUID customerId = UUID.fromString("31000000-0000-0000-0000-000000000010")
        UUID missingAccountId = UUID.fromString("41000000-0000-0000-0000-000000000001")

        when:
        def creditResult = postCredit(
            customerId,
            missingAccountId,
            "7.00",
            "SAL",
            "it-missing-credit-ref-001",
            "it-missing-credit-idem-001"
        )
        def debitResult = postDebit(
            customerId,
            missingAccountId,
            "3.00",
            "ATM",
            "it-missing-debit-ref-001",
            "it-missing-debit-idem-001"
        )

        then:
        creditResult.andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value('deposit/account-not-found'))
            .andExpect(jsonPath('$.title').value('Account not found'))
            .andExpect(jsonPath('$.status').value(404))
            .andExpect(jsonPath('$.detail').value('Demand deposit account was not found'))

        debitResult.andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value('deposit/account-not-found'))
            .andExpect(jsonPath('$.title').value('Account not found'))
            .andExpect(jsonPath('$.status').value(404))
            .andExpect(jsonPath('$.detail').value('Demand deposit account was not found'))
    }

    def "same idempotency identity with changed payload returns 409 transaction-idempotency-conflict"() {
        given:
        UUID customerId = UUID.fromString("31000000-0000-0000-0000-000000000011")
        UUID accountId = createAndActivateAccount(customerId, "it-tx-idempotency-conflict-create-001")

        String referenceId = "it-idempotency-conflict-ref-001"
        String idempotencyKey = "it-idempotency-conflict-key-001"

        postCredit(customerId, accountId, "12.00", "SAL", referenceId, idempotencyKey)
            .andExpect(status().isCreated())

        def transactionBeforeConflict = demandDepositAccountTransactionRepository
            .findByAccountIdAndReferenceId(accountId, referenceId)
            .orElseThrow()
        def idempotencyBeforeConflict = accountTransactionIdempotencyRepository
            .findByCustomerIdAndIdempotencyKeyAndReferenceId(customerId, idempotencyKey, referenceId)
            .orElseThrow()

        long transactionRowsBeforeConflict = demandDepositAccountTransactionRepository.count()
        long idempotencyRowsBeforeConflict = accountTransactionIdempotencyRepository.count()
        def accountBeforeConflict = demandDepositAccountRepository.findById(accountId).orElseThrow()

        when:
        def conflictResult = postCredit(
            customerId,
            accountId,
            "12.00",
            "BON",
            referenceId,
            idempotencyKey
        )

        then:
        conflictResult.andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value('deposit/transaction-idempotency-conflict'))
            .andExpect(jsonPath('$.title').value('Transaction idempotency conflict'))
            .andExpect(jsonPath('$.status').value(409))
            .andExpect(jsonPath('$.detail').value('Unable to resolve idempotent transaction request'))

        and:
        def accountAfterConflict = demandDepositAccountRepository.findById(accountId).orElseThrow()
        def transactionAfterConflict = demandDepositAccountTransactionRepository
            .findByAccountIdAndReferenceId(accountId, referenceId)
            .orElseThrow()
        def idempotencyAfterConflict = accountTransactionIdempotencyRepository
            .findByCustomerIdAndIdempotencyKeyAndReferenceId(customerId, idempotencyKey, referenceId)
            .orElseThrow()

        accountAfterConflict.currentBalance.compareTo(accountBeforeConflict.currentBalance) == 0
        accountAfterConflict.availableBalance.compareTo(accountBeforeConflict.availableBalance) == 0
        demandDepositAccountTransactionRepository.count() == transactionRowsBeforeConflict
        accountTransactionIdempotencyRepository.count() == idempotencyRowsBeforeConflict
        transactionAfterConflict.id == transactionBeforeConflict.id
        transactionAfterConflict.transactionCode == 'SAL'
        transactionAfterConflict.amount.compareTo(new BigDecimal("12.00")) == 0
        idempotencyAfterConflict.id == idempotencyBeforeConflict.id
        idempotencyAfterConflict.transactionId == transactionAfterConflict.id
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

    def "pending verification account accepts allowlisted credit code via HTTP"() {
        given:
        UUID customerId = UUID.fromString("31000000-0000-0000-0000-000000000007")
        UUID accountId = createAccount(customerId, "it-tx-pending-code-create-001")
        long transactionRowsBefore = demandDepositAccountTransactionRepository.count()
        long idempotencyRowsBefore = accountTransactionIdempotencyRepository.count()

        when:
        def result = postCredit(
            customerId,
            accountId,
            "15.00",
            "CASH_DEPOSIT",
            "it-pending-allowed-credit-ref-001",
            "it-pending-allowed-credit-idem-001"
        )

        then:
        result.andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.transactionType').value('CREDIT'))
            .andExpect(jsonPath('$.transactionCode').value('CASH_DEPOSIT'))
            .andExpect(jsonPath('$.currentBalance').value(15.0))
            .andExpect(jsonPath('$.availableBalance').value(15.0))

        and:
        demandDepositAccountTransactionRepository.count() == transactionRowsBefore + 1
        accountTransactionIdempotencyRepository.count() == idempotencyRowsBefore + 1
    }

    def "pending verification account rejects non-allowlisted code via HTTP"() {
        given:
        UUID customerId = UUID.fromString("31000000-0000-0000-0000-000000000017")
        UUID accountId = createAccount(customerId, "it-tx-pending-code-create-002")

        postCredit(
            customerId,
            accountId,
            "15.00",
            "CASH_DEPOSIT",
            "it-pending-seed-credit-ref-001",
            "it-pending-seed-credit-idem-001"
        ).andExpect(status().isCreated())

        long transactionRowsBefore = demandDepositAccountTransactionRepository.count()
        long idempotencyRowsBefore = accountTransactionIdempotencyRepository.count()

        when:
        def result = postCredit(
            customerId,
            accountId,
            "5.00",
            "FAST_TRANSFER",
            "it-pending-disallowed-credit-ref-001",
            "it-pending-disallowed-credit-idem-001"
        )

        then:
        result.andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value('deposit/transaction-not-allowed-for-account-status'))
            .andExpect(jsonPath('$.status').value(422))

        and:
        def reloaded = demandDepositAccountRepository.findById(accountId).orElseThrow()
        reloaded.currentBalance.compareTo(new BigDecimal("15.00")) == 0
        reloaded.availableBalance.compareTo(new BigDecimal("15.00")) == 0
        demandDepositAccountTransactionRepository.count() == transactionRowsBefore
        accountTransactionIdempotencyRepository.count() == idempotencyRowsBefore
    }

    def "bypass-validation transaction codes accepted through API when intended"() {
        given:
        UUID customerId = UUID.fromString("31000000-0000-0000-0000-000000000018")
        UUID accountId = createAndActivateAccount(customerId, "it-tx-bypass-create-001")

        postCredit(customerId, accountId, "40.00", "SAL", "it-bypass-seed-credit-ref-001", "it-bypass-seed-credit-idem-001")
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
        def debitResult = postDebit(
            customerId,
            accountId,
            "10.00",
            "FUND_HOLD_DEBIT",
            "it-bypass-debit-ref-001",
            "it-bypass-debit-idem-001"
        )
        def creditResult = postCredit(
            customerId,
            accountId,
            "5.00",
            "FUND_RELEASE_CR",
            "it-bypass-credit-ref-001",
            "it-bypass-credit-idem-001"
        )

        then:
        debitResult.andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.transactionType').value('DEBIT'))
            .andExpect(jsonPath('$.transactionCode').value('FUND_HOLD_DEBIT'))

        creditResult.andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.transactionType').value('CREDIT'))
            .andExpect(jsonPath('$.transactionCode').value('FUND_RELEASE_CR'))

        and:
        def reloaded = demandDepositAccountRepository.findById(accountId).orElseThrow()
        reloaded.currentBalance.compareTo(new BigDecimal("35.00")) == 0
        reloaded.availableBalance.compareTo(new BigDecimal("35.00")) == 0
        demandDepositAccountTransactionRepository.count() == transactionRowsBefore + 2
        accountTransactionIdempotencyRepository.count() == idempotencyRowsBefore + 2
    }

    def "malformed transaction codes return 400 for credit and debit"() {
        given:
        UUID customerId = UUID.fromString("31000000-0000-0000-0000-000000000019")
        UUID accountId = createAndActivateAccount(customerId, "it-tx-malformed-code-create-001")
        String tooLongCode = "A" * 65
        String codeWithHyphen = "FAST-TRANSFER"

        long transactionRowsBefore = demandDepositAccountTransactionRepository.count()
        long idempotencyRowsBefore = accountTransactionIdempotencyRepository.count()

        when:
        def creditTooLong = postCredit(
            customerId,
            accountId,
            "1.00",
            tooLongCode,
            "it-malformed-credit-long-ref-001",
            "it-malformed-credit-long-idem-001"
        )
        def creditWithHyphen = postCredit(
            customerId,
            accountId,
            "1.00",
            codeWithHyphen,
            "it-malformed-credit-hyphen-ref-001",
            "it-malformed-credit-hyphen-idem-001"
        )
        def debitTooLong = postDebit(
            customerId,
            accountId,
            "1.00",
            tooLongCode,
            "it-malformed-debit-long-ref-001",
            "it-malformed-debit-long-idem-001"
        )
        def debitWithHyphen = postDebit(
            customerId,
            accountId,
            "1.00",
            codeWithHyphen,
            "it-malformed-debit-hyphen-ref-001",
            "it-malformed-debit-hyphen-idem-001"
        )

        then:
        creditTooLong.andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.status').value(400))
            .andExpect(jsonPath('$.title').value('Validation failed'))

        creditWithHyphen.andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.status').value(400))
            .andExpect(jsonPath('$.title').value('Validation failed'))

        debitTooLong.andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.status').value(400))
            .andExpect(jsonPath('$.title').value('Validation failed'))

        debitWithHyphen.andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.status').value(400))
            .andExpect(jsonPath('$.title').value('Validation failed'))

        and:
        demandDepositAccountTransactionRepository.count() == transactionRowsBefore
        accountTransactionIdempotencyRepository.count() == idempotencyRowsBefore
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
        def transactionAfterReplay = demandDepositAccountTransactionRepository
            .findByAccountIdAndReferenceId(accountId, referenceId)
            .orElseThrow()
        def idempotencyAfterReplay = accountTransactionIdempotencyRepository
            .findByCustomerIdAndIdempotencyKeyAndReferenceId(customerId, idempotencyKey, referenceId)
            .orElseThrow()

        demandDepositAccountTransactionRepository.count() == 1
        accountTransactionIdempotencyRepository.count() == 1
        def reloaded = demandDepositAccountRepository.findById(accountId).orElseThrow()
        reloaded.currentBalance.compareTo(new BigDecimal("25.00")) == 0
        reloaded.availableBalance.compareTo(new BigDecimal("25.00")) == 0
        transactionAfterReplay.amount.compareTo(new BigDecimal("25.00")) == 0
        transactionAfterReplay.transactionCode == 'SAL'
        idempotencyAfterReplay.transactionId == transactionAfterReplay.id

        and:
        def transactionSnapshot = transactionAfterReplay
        def idempotencySnapshot = idempotencyAfterReplay

        when:
        def replayAgain = postCredit(customerId, accountId, "25.00", "SAL", referenceId, idempotencyKey)

        then:
        replayAgain.andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.currentBalance').value(25.0))
            .andExpect(jsonPath('$.availableBalance').value(25.0))

        and:
        def transactionAfterThirdReplay = demandDepositAccountTransactionRepository
            .findByAccountIdAndReferenceId(accountId, referenceId)
            .orElseThrow()
        def idempotencyAfterThirdReplay = accountTransactionIdempotencyRepository
            .findByCustomerIdAndIdempotencyKeyAndReferenceId(customerId, idempotencyKey, referenceId)
            .orElseThrow()

        demandDepositAccountTransactionRepository.count() == 1
        accountTransactionIdempotencyRepository.count() == 1
        transactionAfterThirdReplay.id == transactionSnapshot.id
        transactionAfterThirdReplay.version == transactionSnapshot.version
        idempotencyAfterThirdReplay.id == idempotencySnapshot.id
        idempotencyAfterThirdReplay.transactionId == transactionSnapshot.id
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
