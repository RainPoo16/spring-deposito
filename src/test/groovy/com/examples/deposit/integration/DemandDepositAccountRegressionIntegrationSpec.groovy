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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class DemandDepositAccountRegressionIntegrationSpec extends Specification {

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

    def "regression journey create activate block blocked transaction cancel block successful transaction replay without duplicates"() {
        given:
        UUID customerId = UUID.fromString("51000000-0000-0000-0000-000000000001")
        UUID accountId = createAndActivateAccount(customerId, "it-regression-create-001")
        LocalDate now = LocalDate.now()

        when:
        def blockCreatedResult = createBlock(
            customerId,
            accountId,
            "ACC",
            now.toString(),
            now.plusDays(3).toString(),
            "regression pending block"
        )

        then:
        blockCreatedResult.andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.accountId').value(accountId.toString()))
            .andExpect(jsonPath('$.blockCode').value('ACC'))
            .andExpect(jsonPath('$.status').value('PENDING'))

        when:
        def activeBlock = demandDepositAccountBlockRepository.saveAndFlush(
            DemandDepositAccountBlock.create(
                accountId,
                BlockCode.ACC,
                BlockRequestedBy.CUSTOMER,
                AccountBlockStatus.ACTIVE,
                now.minusDays(1),
                now.plusDays(1),
                "regression active block"
            )
        )

        long transactionsBeforeBlockedAttempt = demandDepositAccountTransactionRepository.count()
        long idempotencyBeforeBlockedAttempt = accountTransactionIdempotencyRepository.count()

        def blockedCreditResult = postCredit(
            customerId,
            accountId,
            "30.00",
            "SAL",
            "it-regression-blocked-credit-ref-001",
            "it-regression-blocked-credit-idem-001"
        )

        then:
        blockedCreditResult.andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value('deposit/transaction-blocked'))
            .andExpect(jsonPath('$.status').value(422))

        and:
        demandDepositAccountTransactionRepository.count() == transactionsBeforeBlockedAttempt
        accountTransactionIdempotencyRepository.count() == idempotencyBeforeBlockedAttempt

        when:
        def cancelBlockResult = cancelBlock(customerId, accountId, activeBlock.id)

        then:
        cancelBlockResult.andExpect(status().isNoContent())
            .andExpect(content().string(''))

        and:
        demandDepositAccountBlockRepository.findById(activeBlock.id).orElseThrow().status == AccountBlockStatus.CANCELLED

        when:
        long transactionsBeforeSuccess = demandDepositAccountTransactionRepository.count()
        long idempotencyBeforeSuccess = accountTransactionIdempotencyRepository.count()

        postCredit(
            customerId,
            accountId,
            "30.00",
            "SAL",
            "it-regression-success-credit-ref-001",
            "it-regression-success-credit-idem-001"
        ).andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.transactionType').value('CREDIT'))
            .andExpect(jsonPath('$.currentBalance').value(30.0))
            .andExpect(jsonPath('$.availableBalance').value(30.0))

        String firstTransactionId = demandDepositAccountTransactionRepository
            .findByAccountIdAndReferenceId(accountId, "it-regression-success-credit-ref-001")
            .orElseThrow()
            .id
            .toString()

        def replayResult = postCredit(
            customerId,
            accountId,
            "30.00",
            "SAL",
            "it-regression-success-credit-ref-001",
            "it-regression-success-credit-idem-001"
        )

        then:
        replayResult.andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.transactionId').value(firstTransactionId))
            .andExpect(jsonPath('$.currentBalance').value(30.0))
            .andExpect(jsonPath('$.availableBalance').value(30.0))

        and:
        demandDepositAccountTransactionRepository.count() == transactionsBeforeSuccess + 1
        accountTransactionIdempotencyRepository.count() == idempotencyBeforeSuccess + 1
        def reloaded = demandDepositAccountRepository.findById(accountId).orElseThrow()
        reloaded.currentBalance.compareTo(new BigDecimal("30.00")) == 0
        reloaded.availableBalance.compareTo(new BigDecimal("30.00")) == 0
    }

    def "negative path matrix returns problem detail with problem content-type - #scenario"() {
        when:
        ResultActions result = switch (scenario) {
            case 'malformed request' -> malformedRequestResult()
            case 'ownership mismatch' -> ownershipMismatchResult()
            case 'account not found' -> accountNotFoundResult()
            case 'restriction blocked' -> restrictionBlockedResult()
            case 'insufficient balance' -> insufficientBalanceResult()
            case 'idempotency conflict' -> idempotencyConflictResult()
            default -> throw new IllegalArgumentException("Unknown scenario: " + scenario)
        }

        then:
        result.andExpect(status().is(expectedStatus))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value(expectedType))
            .andExpect(jsonPath('$.status').value(expectedStatus))

        where:
        scenario                 | expectedStatus | expectedType
        'malformed request'      | 400            | 'deposit/malformed-request'
        'ownership mismatch'     | 404            | 'deposit/account-not-found'
        'account not found'      | 404            | 'deposit/account-not-found'
        'restriction blocked'    | 422            | 'deposit/transaction-blocked'
        'insufficient balance'   | 422            | 'deposit/insufficient-available-balance'
        'idempotency conflict'   | 409            | 'deposit/transaction-idempotency-conflict'
    }

    private ResultActions malformedRequestResult() {
        return mockMvc.perform(post('/transactions/credit')
            .contentType(MediaType.APPLICATION_JSON)
            .header('x-customer-id', UUID.fromString('51000000-0000-0000-0000-000000000010').toString())
            .content('{"accountId":'))
    }

    private ResultActions ownershipMismatchResult() {
        UUID ownerCustomerId = UUID.fromString('51000000-0000-0000-0000-000000000011')
        UUID foreignCustomerId = UUID.fromString('51000000-0000-0000-0000-000000000012')
        UUID accountId = createAndActivateAccount(ownerCustomerId, 'it-regression-ownership-create-001')

        return postCredit(
            foreignCustomerId,
            accountId,
            '5.00',
            'SAL',
            'it-regression-ownership-ref-001',
            'it-regression-ownership-idem-001'
        )
    }

    private ResultActions accountNotFoundResult() {
        return postCredit(
            UUID.fromString('51000000-0000-0000-0000-000000000013'),
            UUID.fromString('52000000-0000-0000-0000-000000000001'),
            '5.00',
            'SAL',
            'it-regression-missing-ref-001',
            'it-regression-missing-idem-001'
        )
    }

    private ResultActions restrictionBlockedResult() {
        UUID customerId = UUID.fromString('51000000-0000-0000-0000-000000000014')
        UUID accountId = createAndActivateAccount(customerId, 'it-regression-restriction-create-001')

        demandDepositAccountBlockRepository.saveAndFlush(
            DemandDepositAccountBlock.create(
                accountId,
                BlockCode.ACC,
                BlockRequestedBy.CUSTOMER,
                AccountBlockStatus.ACTIVE,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1),
                'regression restriction block'
            )
        )

        return postCredit(
            customerId,
            accountId,
            '5.00',
            'SAL',
            'it-regression-restriction-ref-001',
            'it-regression-restriction-idem-001'
        )
    }

    private ResultActions insufficientBalanceResult() {
        UUID customerId = UUID.fromString('51000000-0000-0000-0000-000000000015')
        UUID accountId = createAndActivateAccount(customerId, 'it-regression-insufficient-create-001')

        return postDebit(
            customerId,
            accountId,
            '1.00',
            'ATM',
            'it-regression-insufficient-ref-001',
            'it-regression-insufficient-idem-001'
        )
    }

    private ResultActions idempotencyConflictResult() {
        UUID customerId = UUID.fromString('51000000-0000-0000-0000-000000000016')
        UUID accountId = createAndActivateAccount(customerId, 'it-regression-idempotency-create-001')

        postCredit(
            customerId,
            accountId,
            '7.00',
            'SAL',
            'it-regression-idempotency-ref-001',
            'it-regression-idempotency-key-001'
        ).andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

        return postCredit(
            customerId,
            accountId,
            '7.00',
            'BON',
            'it-regression-idempotency-ref-001',
            'it-regression-idempotency-key-001'
        )
    }

    private UUID createAndActivateAccount(UUID customerId, String idempotencyKey) {
        UUID accountId = createAccount(customerId, idempotencyKey)
        def account = demandDepositAccountRepository.findById(accountId).orElseThrow()
        account.activate()
        demandDepositAccountRepository.saveAndFlush(account)
        return accountId
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

    private ResultActions createBlock(
        UUID customerId,
        UUID accountId,
        String blockCode,
        String effectiveDate,
        String expiryDate,
        String remark
    ) {
        return mockMvc.perform(post("/demand-deposit-accounts/${accountId}/blocks")
            .contentType(MediaType.APPLICATION_JSON)
            .header('x-customer-id', customerId.toString())
            .content('''
                {
                  "blockCode": "''' + blockCode + '''",
                  "effectiveDate": "''' + effectiveDate + '''",
                  "expiryDate": "''' + expiryDate + '''",
                  "remark": "''' + remark + '''"
                }
            '''))
    }

    private ResultActions cancelBlock(UUID customerId, UUID accountId, UUID blockId) {
        return mockMvc.perform(patch("/demand-deposit-accounts/${accountId}/blocks/${blockId}/cancel")
            .contentType(MediaType.APPLICATION_JSON)
            .header('x-customer-id', customerId.toString()))
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
