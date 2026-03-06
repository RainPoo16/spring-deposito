package com.examples.deposit.controller

import com.examples.deposit.domain.DemandDepositAccount
import com.examples.deposit.domain.DemandDepositAccountStatus
import com.examples.deposit.domain.exception.CustomerNotEligibleForAccountCreationException
import com.examples.deposit.domain.exception.IdempotencyConflictException
import com.examples.deposit.service.DemandDepositAccountService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import java.util.UUID

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
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
