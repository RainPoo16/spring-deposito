package com.examples.deposit.controller

import com.examples.deposit.domain.DemandDepositAccount
import com.examples.deposit.domain.DemandDepositAccountBlock
import com.examples.deposit.domain.DemandDepositAccountStatus
import com.examples.deposit.domain.AccountBlockStatus
import com.examples.deposit.domain.BlockCode
import com.examples.deposit.domain.BlockRequestedBy
import com.examples.deposit.domain.exception.AccountNotFoundException
import com.examples.deposit.domain.exception.BlockNotFoundException
import com.examples.deposit.domain.exception.BlockNotEligibleForOperationException
import com.examples.deposit.domain.exception.CustomerNotEligibleForAccountCreationException
import com.examples.deposit.domain.exception.DuplicateOrOverlappingBlockException
import com.examples.deposit.domain.exception.IdempotencyConflictException
import com.examples.deposit.domain.exception.InsufficientAvailableBalanceException
import com.examples.deposit.domain.exception.TransactionBlockedException
import com.examples.deposit.domain.exception.TransactionIdempotencyConflictException
import com.examples.deposit.domain.exception.TransactionNotAllowedForAccountStatusException
import com.examples.deposit.domain.TransactionType
import com.examples.deposit.service.DemandDepositAccountBlockService
import com.examples.deposit.service.DemandDepositAccountService
import com.examples.deposit.service.DemandDepositAccountTransactionService
import com.examples.deposit.service.dto.PostedTransactionResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import java.util.UUID
import java.lang.reflect.Method

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(DemandDepositAccountController)
@Import([
    com.examples.deposit.controller.exception.GlobalExceptionHandler,
    com.examples.deposit.controller.exception.ApiProblemFactory
])
class DemandDepositAccountControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @org.spockframework.spring.SpringBean
    DemandDepositAccountService demandDepositAccountService = Mock()

    @org.spockframework.spring.SpringBean
    DemandDepositAccountBlockService demandDepositAccountBlockService = Mock()

    @org.spockframework.spring.SpringBean
    DemandDepositAccountTransactionService demandDepositAccountTransactionService = Mock()

    def "documents all mutation endpoints with explicit operation and api responses"() {
        expect:
        hasOperationAndApiResponses('createDemandDepositAccount', UUID, com.examples.deposit.controller.dto.CreateDemandDepositAccountReq)
        hasOperationAndApiResponses('createDemandDepositAccountBlock', UUID, UUID, com.examples.deposit.controller.dto.CreateDemandDepositAccountBlockReq)
        hasOperationAndApiResponses('postCreditTransaction', UUID, com.examples.deposit.controller.dto.PostCreditTransactionReq)
        hasOperationAndApiResponses('postDebitTransaction', UUID, com.examples.deposit.controller.dto.PostDebitTransactionReq)
    }

    def "documents 4xx and 5xx responses with problem detail schema and examples"() {
        expect:
        hasProblemDetailSchemaAndExampleForErrorResponses('createDemandDepositAccount', ["400", "409", "422", "500"], UUID, com.examples.deposit.controller.dto.CreateDemandDepositAccountReq)
        hasProblemDetailSchemaAndExampleForErrorResponses('createDemandDepositAccountBlock', ["400", "404", "422", "500"], UUID, UUID, com.examples.deposit.controller.dto.CreateDemandDepositAccountBlockReq)
        hasProblemDetailSchemaAndExampleForErrorResponses('postCreditTransaction', ["400", "404", "409", "422", "500"], UUID, com.examples.deposit.controller.dto.PostCreditTransactionReq)
        hasProblemDetailSchemaAndExampleForErrorResponses('postDebitTransaction', ["400", "404", "409", "422", "500"], UUID, com.examples.deposit.controller.dto.PostDebitTransactionReq)
    }

    def "documents representative success and error examples for mutation endpoints"() {
        expect:
        hasExamplesForResponses('createDemandDepositAccount', ["201", "400"], UUID, com.examples.deposit.controller.dto.CreateDemandDepositAccountReq)
        hasExamplesForResponses('createDemandDepositAccountBlock', ["201", "404", "422"], UUID, UUID, com.examples.deposit.controller.dto.CreateDemandDepositAccountBlockReq)
        hasExamplesForResponses('postCreditTransaction', ["201", "422"], UUID, com.examples.deposit.controller.dto.PostCreditTransactionReq)
        hasExamplesForResponses('postDebitTransaction', ["201", "422"], UUID, com.examples.deposit.controller.dto.PostDebitTransactionReq)
    }

    def "documents canonical problem detail fields for selected shared error examples"() {
        expect:
        hasProblemDetailExampleFields(
            'createDemandDepositAccount',
            '409',
            'idempotency-conflict',
            'deposit/idempotency-conflict',
            'Idempotency conflict',
            409,
            'Unable to resolve idempotent account creation request',
            UUID,
            com.examples.deposit.controller.dto.CreateDemandDepositAccountReq
        )
        hasProblemDetailExampleFields(
            'postCreditTransaction',
            '404',
            'account-not-found',
            'deposit/account-not-found',
            'Account not found',
            404,
            'Demand deposit account was not found',
            UUID,
            com.examples.deposit.controller.dto.PostCreditTransactionReq
        )
        hasProblemDetailExampleFields(
            'postCreditTransaction',
            '409',
            'transaction-idempotency-conflict',
            'deposit/transaction-idempotency-conflict',
            'Transaction idempotency conflict',
            409,
            'Unable to resolve idempotent transaction request',
            UUID,
            com.examples.deposit.controller.dto.PostCreditTransactionReq
        )
        hasProblemDetailExampleFields(
            'postDebitTransaction',
            '404',
            'account-not-found',
            'deposit/account-not-found',
            'Account not found',
            404,
            'Demand deposit account was not found',
            UUID,
            com.examples.deposit.controller.dto.PostDebitTransactionReq
        )
        hasProblemDetailExampleFields(
            'postDebitTransaction',
            '409',
            'transaction-idempotency-conflict',
            'deposit/transaction-idempotency-conflict',
            'Transaction idempotency conflict',
            409,
            'Unable to resolve idempotent transaction request',
            UUID,
            com.examples.deposit.controller.dto.PostDebitTransactionReq
        )
        hasProblemDetailExampleFields(
            'createDemandDepositAccountBlock',
            '422',
            'duplicate-or-overlapping-block',
            'deposit/duplicate-or-overlapping-block',
            'Duplicate or overlapping block',
            422,
            'An active or pending block with the same code overlaps the requested period',
            UUID,
            UUID,
            com.examples.deposit.controller.dto.CreateDemandDepositAccountBlockReq
        )
        hasProblemDetailExampleFields(
            'createDemandDepositAccountBlock',
            '422',
            'block-not-eligible',
            'deposit/block-not-eligible-for-operation',
            'Block not eligible for operation',
            422,
            'Block state or initiator is not eligible for the requested operation',
            UUID,
            UUID,
            com.examples.deposit.controller.dto.CreateDemandDepositAccountBlockReq
        )
    }

    def "posts credit transaction and returns 201 with JSON payload"() {
        given:
        UUID customerId = UUID.fromString("21212121-2121-2121-2121-212121212121")
        UUID accountId = UUID.fromString("31313131-3131-3131-3131-313131313131")
        UUID transactionId = UUID.fromString("41414141-4141-4141-4141-414141414141")
        def postedAt = java.time.Instant.parse("2026-03-06T10:15:30Z")

        demandDepositAccountTransactionService.postCredit(_ as com.examples.deposit.service.dto.PostCreditTransactionCommand) >>
            new PostedTransactionResult(
                transactionId,
                accountId,
                customerId,
                TransactionType.CREDIT,
                new BigDecimal("250.50"),
                "SAL",
                "ref-credit-001",
                "idem-credit-001",
                postedAt,
                new BigDecimal("1250.50"),
                new BigDecimal("1250.50")
            )

        when:
        def result = mockMvc.perform(post('/transactions/credit')
            .contentType(MediaType.APPLICATION_JSON)
            .header('x-customer-id', customerId.toString())
            .content('''
                {
                  "accountId": "31313131-3131-3131-3131-313131313131",
                  "amount": 250.50,
                  "transactionCode": "SAL",
                  "referenceId": "ref-credit-001",
                  "idempotencyKey": "idem-credit-001"
                }
            '''))

        then:
        result.andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.transactionId').value(transactionId.toString()))
            .andExpect(jsonPath('$.accountId').value(accountId.toString()))
            .andExpect(jsonPath('$.customerId').value(customerId.toString()))
            .andExpect(jsonPath('$.transactionType').value('CREDIT'))
            .andExpect(jsonPath('$.amount').value(250.5))
            .andExpect(jsonPath('$.currentBalance').value(1250.5))
            .andExpect(jsonPath('$.availableBalance').value(1250.5))
    }

    def "posts debit transaction and returns 201 with JSON payload"() {
        given:
        UUID customerId = UUID.fromString("22222222-3333-4444-5555-666666666666")
        UUID accountId = UUID.fromString("77777777-8888-9999-aaaa-bbbbbbbbbbbb")
        UUID transactionId = UUID.fromString("cccccccc-dddd-eeee-ffff-000000000000")
        def postedAt = java.time.Instant.parse("2026-03-06T11:15:30Z")

        demandDepositAccountTransactionService.postDebit(_ as com.examples.deposit.service.dto.PostDebitTransactionCommand) >>
            new PostedTransactionResult(
                transactionId,
                accountId,
                customerId,
                TransactionType.DEBIT,
                new BigDecimal("100.00"),
                "ATM",
                "ref-debit-001",
                "idem-debit-001",
                postedAt,
                new BigDecimal("900.00"),
                new BigDecimal("900.00")
            )

        when:
        def result = mockMvc.perform(post('/transactions/debit')
            .contentType(MediaType.APPLICATION_JSON)
            .header('x-customer-id', customerId.toString())
            .content('''
                {
                  "accountId": "77777777-8888-9999-aaaa-bbbbbbbbbbbb",
                  "amount": 100.00,
                  "transactionCode": "ATM",
                  "referenceId": "ref-debit-001",
                  "idempotencyKey": "idem-debit-001"
                }
            '''))

        then:
        result.andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.transactionId').value(transactionId.toString()))
            .andExpect(jsonPath('$.accountId').value(accountId.toString()))
            .andExpect(jsonPath('$.customerId').value(customerId.toString()))
            .andExpect(jsonPath('$.transactionType').value('DEBIT'))
            .andExpect(jsonPath('$.amount').value(100.0))
            .andExpect(jsonPath('$.currentBalance').value(900.0))
            .andExpect(jsonPath('$.availableBalance').value(900.0))
    }

    def "returns 400 problem detail when credit request fails validation"() {
        given:
        UUID customerId = UUID.fromString("99999999-1111-2222-3333-444444444444")

        when:
        def result = mockMvc.perform(post('/transactions/credit')
            .contentType(MediaType.APPLICATION_JSON)
            .header('x-customer-id', customerId.toString())
            .content('{"accountId":null,"amount":0,"transactionCode":"","referenceId":"","idempotencyKey":""}'))

        then:
        result.andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.status').value(400))
            .andExpect(jsonPath('$.title').value('Validation failed'))
        0 * demandDepositAccountTransactionService._
    }

    def "maps transaction failures to problem details with expected 4xx statuses"() {
        given:
        UUID customerId = UUID.fromString("abababab-abab-abab-abab-abababababab")
        UUID accountId = UUID.fromString("bcbcbcbc-bcbc-bcbc-bcbc-bcbcbcbcbcbc")

        demandDepositAccountTransactionService.postCredit(_ as com.examples.deposit.service.dto.PostCreditTransactionCommand) >> {
            throw new TransactionBlockedException(accountId, TransactionType.CREDIT, "SAL", "ACC")
        }
        demandDepositAccountTransactionService.postDebit(_ as com.examples.deposit.service.dto.PostDebitTransactionCommand) >> {
            throw new TransactionNotAllowedForAccountStatusException(accountId, DemandDepositAccountStatus.DORMANT, TransactionType.DEBIT, "ATM")
        } >> {
            throw new InsufficientAvailableBalanceException(accountId, new BigDecimal("10.00"), new BigDecimal("100.00"))
        } >> {
            throw new TransactionIdempotencyConflictException(customerId, "idem-debit-fail", "ref-debit-fail")
        }

        when:
        def blockedResult = mockMvc.perform(post('/transactions/credit')
            .contentType(MediaType.APPLICATION_JSON)
            .header('x-customer-id', customerId.toString())
            .content('''
                {
                  "accountId": "bcbcbcbc-bcbc-bcbc-bcbc-bcbcbcbcbcbc",
                  "amount": 50.00,
                  "transactionCode": "SAL",
                  "referenceId": "ref-credit-fail",
                  "idempotencyKey": "idem-credit-fail"
                }
            '''))
        def statusResult = mockMvc.perform(post('/transactions/debit')
            .contentType(MediaType.APPLICATION_JSON)
            .header('x-customer-id', customerId.toString())
            .content('''
                {
                  "accountId": "bcbcbcbc-bcbc-bcbc-bcbc-bcbcbcbcbcbc",
                  "amount": 50.00,
                  "transactionCode": "ATM",
                  "referenceId": "ref-debit-status",
                  "idempotencyKey": "idem-debit-status"
                }
            '''))
        def balanceResult = mockMvc.perform(post('/transactions/debit')
            .contentType(MediaType.APPLICATION_JSON)
            .header('x-customer-id', customerId.toString())
            .content('''
                {
                  "accountId": "bcbcbcbc-bcbc-bcbc-bcbc-bcbcbcbcbcbc",
                  "amount": 100.00,
                  "transactionCode": "ATM",
                  "referenceId": "ref-debit-balance",
                  "idempotencyKey": "idem-debit-balance"
                }
            '''))
        def idempotencyResult = mockMvc.perform(post('/transactions/debit')
            .contentType(MediaType.APPLICATION_JSON)
            .header('x-customer-id', customerId.toString())
            .content('''
                {
                  "accountId": "bcbcbcbc-bcbc-bcbc-bcbc-bcbcbcbcbcbc",
                  "amount": 100.00,
                  "transactionCode": "ATM",
                  "referenceId": "ref-debit-fail",
                  "idempotencyKey": "idem-debit-fail"
                }
            '''))

        then:
        blockedResult.andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.status').value(422))
            .andExpect(jsonPath('$.type').value('deposit/transaction-blocked'))
            .andExpect(jsonPath('$.title').value('Transaction blocked'))
            .andExpect(jsonPath('$.detail').value('Transaction is blocked by active account restrictions'))
        statusResult.andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.status').value(422))
            .andExpect(jsonPath('$.type').value('deposit/transaction-not-allowed-for-account-status'))
        balanceResult.andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.status').value(422))
            .andExpect(jsonPath('$.type').value('deposit/insufficient-available-balance'))
            .andExpect(jsonPath('$.title').value('Insufficient available balance'))
            .andExpect(jsonPath('$.detail').value('Available balance is insufficient for the requested debit'))
        idempotencyResult.andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.status').value(409))
            .andExpect(jsonPath('$.type').value('deposit/transaction-idempotency-conflict'))
    }

    def "creates block and returns 201 with JSON payload"() {
        given:
        UUID customerId = UUID.fromString("10101010-1010-1010-1010-101010101010")
        UUID accountId = UUID.fromString("20202020-2020-2020-2020-202020202020")
        def createdBlock = DemandDepositAccountBlock.create(
            accountId,
            BlockCode.ACC,
            BlockRequestedBy.CUSTOMER,
            AccountBlockStatus.PENDING,
            java.time.LocalDate.of(2026, 3, 12),
            java.time.LocalDate.of(2026, 3, 22),
            "customer request"
        )

        demandDepositAccountBlockService.createBlock(_ as com.examples.deposit.service.dto.CreateDemandDepositAccountBlockCommand) >> createdBlock

        when:
        def result = mockMvc.perform(post("/demand-deposit-accounts/${accountId}/blocks")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-customer-id", customerId.toString())
            .content('''
                {
                  "blockCode": "ACC",
                  "effectiveDate": "2026-03-12",
                  "expiryDate": "2026-03-22",
                  "remark": "customer request"
                }
            '''))

        then:
        result.andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.blockId').value(createdBlock.id.toString()))
            .andExpect(jsonPath('$.accountId').value(accountId.toString()))
            .andExpect(jsonPath('$.blockCode').value('ACC'))
            .andExpect(jsonPath('$.status').value('PENDING'))
    }

    def "updates block and returns 200 with JSON payload"() {
        given:
        UUID customerId = UUID.fromString("30303030-3030-3030-3030-303030303030")
        UUID accountId = UUID.fromString("40404040-4040-4040-4040-404040404040")
        UUID blockId = UUID.fromString("50505050-5050-5050-5050-505050505050")
        def updatedBlock = DemandDepositAccountBlock.create(
            accountId,
            BlockCode.ACC,
            BlockRequestedBy.CUSTOMER,
            AccountBlockStatus.ACTIVE,
            java.time.LocalDate.of(2026, 3, 15),
            java.time.LocalDate.of(2026, 3, 25),
            "updated remark"
        )

        demandDepositAccountBlockService.updateBlock(_ as com.examples.deposit.service.dto.UpdateDemandDepositAccountBlockCommand) >> updatedBlock

        when:
        def result = mockMvc.perform(put("/demand-deposit-accounts/${accountId}/blocks/${blockId}")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-customer-id", customerId.toString())
            .content('''
                {
                  "effectiveDate": "2026-03-15",
                  "expiryDate": "2026-03-25",
                  "remark": "updated remark"
                }
            '''))

        then:
        result.andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.blockId').value(updatedBlock.id.toString()))
            .andExpect(jsonPath('$.accountId').value(accountId.toString()))
            .andExpect(jsonPath('$.status').value('ACTIVE'))
    }

    def "cancels block and returns 204 no content"() {
        given:
        UUID customerId = UUID.fromString("60606060-6060-6060-6060-606060606060")
        UUID accountId = UUID.fromString("70707070-7070-7070-7070-707070707070")
        UUID blockId = UUID.fromString("80808080-8080-8080-8080-808080808080")

        demandDepositAccountBlockService.cancelBlock(_ as com.examples.deposit.service.dto.UpdateDemandDepositAccountBlockCommand) >> Mock(DemandDepositAccountBlock)

        when:
        def result = mockMvc.perform(patch("/demand-deposit-accounts/${accountId}/blocks/${blockId}/cancel")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-customer-id", customerId.toString()))

        then:
        result.andExpect(status().isNoContent())
            .andExpect(content().string(""))
    }

    def "returns 400 problem detail when create block request fails validation"() {
        given:
        UUID customerId = UUID.fromString("90909090-9090-9090-9090-909090909090")
        UUID accountId = UUID.fromString("91919191-9191-9191-9191-919191919191")

        when:
        def result = mockMvc.perform(post("/demand-deposit-accounts/${accountId}/blocks")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-customer-id", customerId.toString())
            .content('{"blockCode":"","effectiveDate":null,"expiryDate":null,"remark":"x"}'))

        then:
        result.andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.status').value(400))
            .andExpect(jsonPath('$.title').value('Validation failed'))
        0 * demandDepositAccountBlockService._
    }

    def "maps overlap and eligibility block failures to 422 problem detail"() {
        given:
        UUID customerId = UUID.fromString("12121212-3434-5656-7878-909090909090")
        UUID accountId = UUID.fromString("13131313-3535-5757-7979-919191919191")
        UUID blockId = UUID.fromString("14141414-3636-5858-8080-929292929292")

        demandDepositAccountBlockService.createBlock(_ as com.examples.deposit.service.dto.CreateDemandDepositAccountBlockCommand) >> {
            throw new DuplicateOrOverlappingBlockException(accountId, "ACC")
        }
        demandDepositAccountBlockService.updateBlock(_ as com.examples.deposit.service.dto.UpdateDemandDepositAccountBlockCommand) >> {
            throw new BlockNotEligibleForOperationException(blockId, "update")
        }

        when:
        def overlapResult = mockMvc.perform(post("/demand-deposit-accounts/${accountId}/blocks")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-customer-id", customerId.toString())
            .content('''
                {
                  "blockCode": "ACC",
                  "effectiveDate": "2026-03-12",
                  "expiryDate": "2026-03-22",
                  "remark": "dup"
                }
            '''))
        def ineligibleResult = mockMvc.perform(put("/demand-deposit-accounts/${accountId}/blocks/${blockId}")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-customer-id", customerId.toString())
            .content('''
                {
                  "effectiveDate": "2026-03-12",
                  "expiryDate": "2026-03-22",
                  "remark": "ineligible"
                }
            '''))

        then:
        overlapResult.andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.status').value(422))
        ineligibleResult.andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.status').value(422))
    }

    def "maps account-not-found during block operations to 404 problem detail"() {
        given:
        UUID customerId = UUID.fromString("15151515-3737-5959-8181-939393939393")
        UUID accountId = UUID.fromString("16161616-3838-6060-8282-949494949494")
        UUID blockId = UUID.fromString("17171717-3939-6161-8383-959595959595")

        demandDepositAccountBlockService.createBlock(_ as com.examples.deposit.service.dto.CreateDemandDepositAccountBlockCommand) >> {
            throw new AccountNotFoundException(accountId)
        }
        demandDepositAccountBlockService.updateBlock(_ as com.examples.deposit.service.dto.UpdateDemandDepositAccountBlockCommand) >> {
            throw new AccountNotFoundException(accountId)
        }

        when:
        def createResult = mockMvc.perform(post("/demand-deposit-accounts/${accountId}/blocks")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-customer-id", customerId.toString())
            .content('''
                {
                  "blockCode": "ACC",
                  "effectiveDate": "2026-03-12",
                  "expiryDate": "2026-03-22",
                  "remark": "missing account"
                }
            '''))
        def updateResult = mockMvc.perform(put("/demand-deposit-accounts/${accountId}/blocks/${blockId}")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-customer-id", customerId.toString())
            .content('''
                {
                  "effectiveDate": "2026-03-12",
                  "expiryDate": "2026-03-22",
                  "remark": "missing account"
                }
            '''))

        then:
        createResult.andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.status').value(404))
            .andExpect(jsonPath('$.title').value('Account not found'))
            .andExpect(jsonPath('$.type').value('deposit/account-not-found'))
        updateResult.andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.status').value(404))
            .andExpect(jsonPath('$.title').value('Account not found'))
            .andExpect(jsonPath('$.type').value('deposit/account-not-found'))
    }

    def "maps block-not-found or ownership-mismatch during block operations to 404 problem detail"() {
        given:
        UUID customerId = UUID.fromString("18181818-4040-6262-8484-969696969696")
        UUID accountId = UUID.fromString("19191919-4141-6363-8585-979797979797")
        UUID blockId = UUID.fromString("20202020-4242-6464-8686-989898989898")

        demandDepositAccountBlockService.updateBlock(_ as com.examples.deposit.service.dto.UpdateDemandDepositAccountBlockCommand) >> {
            throw new BlockNotFoundException(blockId)
        }

        when:
        def result = mockMvc.perform(put("/demand-deposit-accounts/${accountId}/blocks/${blockId}")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-customer-id", customerId.toString())
            .content('''
                {
                  "effectiveDate": "2026-03-12",
                  "expiryDate": "2026-03-22",
                  "remark": "missing or foreign block"
                }
            '''))

        then:
        result.andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.status').value(404))
            .andExpect(jsonPath('$.title').value('Block not found'))
            .andExpect(jsonPath('$.type').value('deposit/block-not-found'))
    }

    def "returns 201 and response body for successful account creation"() {
        given:
        UUID customerId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        UUID accountId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        String idempotencyKey = "idem-create-001"

        demandDepositAccountService.createMainAccount(customerId, idempotencyKey) >>
            DemandDepositAccount.createWithId(accountId, customerId, DemandDepositAccountStatus.PENDING_VERIFICATION)

        when:
        def result = mockMvc.perform(post("/demand-deposit-accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-customer-id", customerId.toString())
            .content('{"idempotencyKey":"idem-create-001"}'))

        then:
        result.andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.accountId').value(accountId.toString()))
            .andExpect(jsonPath('$.customerId').value(customerId.toString()))
            .andExpect(jsonPath('$.status').value('PENDING_VERIFICATION'))
    }

    def "returns 400 problem detail for missing idempotency key"() {
        given:
        UUID customerId = UUID.fromString("11111111-1111-1111-1111-111111111111")

        when:
        def result = mockMvc.perform(post("/demand-deposit-accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-customer-id", customerId.toString())
            .content('{}'))

        then:
        result.andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.status').value(400))
            .andExpect(jsonPath('$.title').value('Validation failed'))
        0 * demandDepositAccountService._
    }

    def "returns 400 problem detail for blank idempotency key"() {
        given:
        UUID customerId = UUID.fromString("12121212-1212-1212-1212-121212121212")

        when:
        def result = mockMvc.perform(post("/demand-deposit-accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-customer-id", customerId.toString())
            .content('{"idempotencyKey":"   "}'))

        then:
        result.andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.status').value(400))
            .andExpect(jsonPath('$.title').value('Validation failed'))
        0 * demandDepositAccountService._
    }

    def "maps ineligible customer to 422 problem detail"() {
        given:
        UUID customerId = UUID.fromString("33333333-3333-3333-3333-333333333333")
        String idempotencyKey = "idem-ineligible-001"

        demandDepositAccountService.createMainAccount(customerId, idempotencyKey) >> {
            throw new CustomerNotEligibleForAccountCreationException(customerId)
        }

        when:
        def result = mockMvc.perform(post('/demand-deposit-accounts')
            .contentType(MediaType.APPLICATION_JSON)
            .header('x-customer-id', customerId.toString())
            .content('{"idempotencyKey":"idem-ineligible-001"}'))

        then:
        result.andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.status').value(422))
            .andExpect(jsonPath('$.title').value('Customer not eligible'))
            .andExpect(jsonPath('$.type').value('deposit/customer-not-eligible'))
    }

    def "maps idempotency conflict to 409 problem detail"() {
        given:
        UUID customerId = UUID.fromString("66666666-6666-6666-6666-666666666666")
        String idempotencyKey = "idem-conflict-001"

        demandDepositAccountService.createMainAccount(customerId, idempotencyKey) >> {
            throw new IdempotencyConflictException(customerId, idempotencyKey)
        }

        when:
        def result = mockMvc.perform(post('/demand-deposit-accounts')
            .contentType(MediaType.APPLICATION_JSON)
            .header('x-customer-id', customerId.toString())
            .content('{"idempotencyKey":"idem-conflict-001"}'))

        then:
        result.andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.status').value(409))
            .andExpect(jsonPath('$.title').value('Idempotency conflict'))
            .andExpect(jsonPath('$.type').value('deposit/idempotency-conflict'))
    }

    def "returns 400 malformed request problem detail for malformed json body"() {
        given:
        UUID customerId = UUID.fromString("77777777-7777-7777-7777-777777777777")

        when:
        def result = mockMvc.perform(post('/demand-deposit-accounts')
            .contentType(MediaType.APPLICATION_JSON)
            .header('x-customer-id', customerId.toString())
            .content('{"idempotencyKey":'))

        then:
        assertMalformedRequestProblem(result)
        0 * demandDepositAccountService._
    }

    def "returns 400 malformed request problem detail for missing request body"() {
        given:
        UUID customerId = UUID.fromString("88888888-8888-8888-8888-888888888888")

        when:
        def result = mockMvc.perform(post('/demand-deposit-accounts')
            .contentType(MediaType.APPLICATION_JSON)
            .header('x-customer-id', customerId.toString()))

        then:
        assertMalformedRequestProblem(result)
        0 * demandDepositAccountService._
    }

    def "returns 400 malformed request problem detail for invalid x-customer-id header"() {
        when:
        def result = mockMvc.perform(post('/demand-deposit-accounts')
            .contentType(MediaType.APPLICATION_JSON)
            .header('x-customer-id', 'not-a-uuid')
            .content('{"idempotencyKey":"idem-invalid-customer-id-001"}'))

        then:
        assertMalformedRequestProblem(result)
        0 * demandDepositAccountService._
    }

    def "returns 400 malformed request problem detail for missing x-customer-id header"() {
        when:
        def result = mockMvc.perform(post('/demand-deposit-accounts')
            .contentType(MediaType.APPLICATION_JSON)
            .content('{"idempotencyKey":"idem-missing-customer-id-001"}'))

        then:
        assertMalformedRequestProblem(result)
        0 * demandDepositAccountService._
    }

    def "replay call returns success with stable response semantics"() {
        given:
        UUID customerId = UUID.fromString("44444444-4444-4444-4444-444444444444")
        UUID accountId = UUID.fromString("55555555-5555-5555-5555-555555555555")
        String idempotencyKey = "idem-replay-001"

        demandDepositAccountService.createMainAccount(customerId, idempotencyKey) >>> [
            DemandDepositAccount.createWithId(accountId, customerId, DemandDepositAccountStatus.PENDING_VERIFICATION),
            DemandDepositAccount.createWithId(accountId, customerId, DemandDepositAccountStatus.PENDING_VERIFICATION)
        ]

        when:
        def firstResult = mockMvc.perform(post('/demand-deposit-accounts')
            .contentType(MediaType.APPLICATION_JSON)
            .header('x-customer-id', customerId.toString())
            .content('{"idempotencyKey":"idem-replay-001"}'))
        def secondResult = mockMvc.perform(post('/demand-deposit-accounts')
            .contentType(MediaType.APPLICATION_JSON)
            .header('x-customer-id', customerId.toString())
            .content('{"idempotencyKey":"idem-replay-001"}'))

        then:
        firstResult.andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.accountId').value(accountId.toString()))
            .andExpect(jsonPath('$.status').value('PENDING_VERIFICATION'))
        secondResult.andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.accountId').value(accountId.toString()))
            .andExpect(jsonPath('$.status').value('PENDING_VERIFICATION'))
    }

    private static void assertMalformedRequestProblem(def result) {
        result.andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.status').value(400))
            .andExpect(jsonPath('$.title').value('Malformed request'))
            .andExpect(jsonPath('$.type').value('deposit/malformed-request'))
            .andExpect(jsonPath('$.detail').value('Request headers or payload are invalid'))
    }

    private static boolean hasOperationAndApiResponses(String methodName, Object... parameterTypes) {
        Method method = DemandDepositAccountController.getDeclaredMethod(methodName, parameterTypes as Class[])
        method.getAnnotation(Operation) != null && method.getAnnotation(ApiResponses) != null
    }

    private static boolean hasProblemDetailSchemaAndExampleForErrorResponses(String methodName, List<String> responseCodes, Object... parameterTypes) {
        Method method = DemandDepositAccountController.getDeclaredMethod(methodName, parameterTypes as Class[])
        def apiResponses = method.getAnnotation(ApiResponses)
        if (apiResponses == null) {
            return false
        }

        responseCodes.every { code ->
            def apiResponse = apiResponses.value().find { it.responseCode() == code }
            if (apiResponse == null || apiResponse.content().length == 0) {
                return false
            }

            apiResponse.content().any { content ->
                Schema schema = content.schema()
                schema?.implementation() == org.springframework.http.ProblemDetail && content.examples().length > 0
            }
        }
    }

    private static boolean hasExamplesForResponses(String methodName, List<String> responseCodes, Object... parameterTypes) {
        Method method = DemandDepositAccountController.getDeclaredMethod(methodName, parameterTypes as Class[])
        def apiResponses = method.getAnnotation(ApiResponses)
        if (apiResponses == null) {
            return false
        }

        responseCodes.every { code ->
            def apiResponse = apiResponses.value().find { it.responseCode() == code }
            apiResponse != null && apiResponse.content().length > 0 && apiResponse.content().any { it.examples().length > 0 }
        }
    }

    private static boolean hasProblemDetailExampleFields(
        String methodName,
        String responseCode,
        String exampleName,
        String expectedType,
        String expectedTitle,
        int expectedStatus,
        String expectedDetail,
        Object... parameterTypes
    ) {
        Method method = DemandDepositAccountController.getDeclaredMethod(methodName, parameterTypes as Class[])
        def apiResponses = method.getAnnotation(ApiResponses)
        if (apiResponses == null) {
            return false
        }

        def apiResponse = apiResponses.value().find { it.responseCode() == responseCode }
        if (apiResponse == null) {
            return false
        }

        def contentWithExample = apiResponse.content().find { content ->
            content.examples().any { it.name() == exampleName }
        }
        if (contentWithExample == null) {
            return false
        }

        def example = contentWithExample.examples().find { it.name() == exampleName }
        if (example == null || !example.value()) {
            return false
        }

        String value = example.value()
        value.contains("\"type\": \"${expectedType}\"") &&
            value.contains("\"title\": \"${expectedTitle}\"") &&
            value.contains("\"status\": ${expectedStatus}") &&
            value.contains("\"detail\": \"${expectedDetail}\"")
    }
}
