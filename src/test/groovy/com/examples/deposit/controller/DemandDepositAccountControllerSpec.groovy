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
import com.examples.deposit.service.DemandDepositAccountBlockService
import com.examples.deposit.service.DemandDepositAccountService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import java.util.UUID

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
}
