package com.examples.deposit.controller

import com.examples.deposit.controller.exception.GlobalExceptionHandler
import com.examples.deposit.domain.DemandDepositAccountStatus
import com.examples.deposit.exception.AccountCreationConflictException
import com.examples.deposit.exception.AccountLifecycleException
import com.examples.deposit.exception.AccountNotFoundException
import com.examples.deposit.service.DemandDepositAccountService
import com.examples.deposit.service.dto.CreateDemandDepositAccountCommand
import com.examples.deposit.service.dto.DemandDepositAccountResult
import com.github.f4b6a3.uuid.alt.GUID
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification
import spock.lang.Unroll

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(DemandDepositAccountController)
@Import(GlobalExceptionHandler)
class DemandDepositAccountControllerSpec extends Specification {

    @Autowired
    private MockMvc mockMvc

    @SpringBean
    private DemandDepositAccountService demandDepositAccountService = Mock()

    def "create demand deposit account returns 201 with application json for first create"() {
        given:
        def customerId = GUID.v7().toUUID()
        def accountId = GUID.v7().toUUID()

        when:
        def response = mockMvc.perform(post("/demand-deposit-accounts")
            .header("x-customer-id", customerId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content('{"idempotencyKey":"idem-controller-create-001"}'))

        then:
        1 * demandDepositAccountService.createMainAccount({ CreateDemandDepositAccountCommand command ->
            command.customerId() == customerId && command.idempotencyKey() == "idem-controller-create-001"
        }) >> new DemandDepositAccountResult(accountId, customerId, DemandDepositAccountStatus.PENDING_VERIFICATION, false)

        and:
        response.andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.accountId').value(accountId.toString()))
            .andExpect(jsonPath('$.customerId').value(customerId.toString()))
            .andExpect(jsonPath('$.status').value('PENDING_VERIFICATION'))
    }

    def "create demand deposit account returns 200 with application json for idempotent replay"() {
        given:
        def customerId = GUID.v7().toUUID()
        def accountId = GUID.v7().toUUID()

        when:
        def response = mockMvc.perform(post("/demand-deposit-accounts")
            .header("x-customer-id", customerId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content('{"idempotencyKey":"idem-controller-replay-001"}'))

        then:
        1 * demandDepositAccountService.createMainAccount({ CreateDemandDepositAccountCommand command ->
            command.customerId() == customerId && command.idempotencyKey() == "idem-controller-replay-001"
        }) >> new DemandDepositAccountResult(accountId, customerId, DemandDepositAccountStatus.PENDING_VERIFICATION, true)

        and:
        response.andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.accountId').value(accountId.toString()))
            .andExpect(jsonPath('$.customerId').value(customerId.toString()))
            .andExpect(jsonPath('$.status').value('PENDING_VERIFICATION'))
    }

    def "create demand deposit account replay sequence returns 201 then 200 with stable accountId"() {
        given:
        def customerId = GUID.v7().toUUID()
        def accountId = GUID.v7().toUUID()
        def requestBody = '{"idempotencyKey":"idem-controller-sequence-001"}'

        when:
        def firstResponse = mockMvc.perform(post("/demand-deposit-accounts")
            .header("x-customer-id", customerId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(requestBody))

        def secondResponse = mockMvc.perform(post("/demand-deposit-accounts")
            .header("x-customer-id", customerId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(requestBody))

        then:
        2 * demandDepositAccountService.createMainAccount({ CreateDemandDepositAccountCommand command ->
            command.customerId() == customerId && command.idempotencyKey() == "idem-controller-sequence-001"
        }) >>> [
            new DemandDepositAccountResult(accountId, customerId, DemandDepositAccountStatus.PENDING_VERIFICATION, false),
            new DemandDepositAccountResult(accountId, customerId, DemandDepositAccountStatus.PENDING_VERIFICATION, true)
        ]

        and:
        firstResponse.andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.accountId').value(accountId.toString()))
            .andExpect(jsonPath('$.customerId').value(customerId.toString()))
            .andExpect(jsonPath('$.status').value('PENDING_VERIFICATION'))

        and:
        secondResponse.andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.accountId').value(accountId.toString()))
            .andExpect(jsonPath('$.customerId').value(customerId.toString()))
            .andExpect(jsonPath('$.status').value('PENDING_VERIFICATION'))
    }

    def "create demand deposit account returns 400 problem detail for invalid request body"() {
        given:
        def customerId = GUID.v7().toUUID()

        when:
        def response = mockMvc.perform(post("/demand-deposit-accounts")
            .header("x-customer-id", customerId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content('{"idempotencyKey":""}'))

        then:
        0 * demandDepositAccountService._
        response.andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value('deposit/invalid-request'))
            .andExpect(jsonPath('$.detail').value('Request validation failed.'))
    }

    @Unroll
    def "create demand deposit account maps #exception.class.simpleName to #expectedStatus"() {
        given:
        def customerId = GUID.v7().toUUID()

        when:
        def response = mockMvc.perform(post("/demand-deposit-accounts")
            .header("x-customer-id", customerId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content('{"idempotencyKey":"idem-controller-error-001"}'))

        then:
        1 * demandDepositAccountService.createMainAccount(_ as CreateDemandDepositAccountCommand) >> { throw exception }
        response.andExpect(status().is(expectedStatus))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value(expectedType))
            .andExpect(jsonPath('$.detail').value(expectedDetail))

        where:
        exception                                                                            | expectedStatus | expectedType                           | expectedDetail
        new AccountCreationConflictException('conflict', new RuntimeException('cause'))     | 409            | 'deposit/account-creation-conflict'    | 'Account creation request conflicts with current state.'
        new AccountLifecycleException('lifecycle violation', new RuntimeException('cause'))  | 422            | 'deposit/account-lifecycle-violation'  | 'Request violates account lifecycle rules.'
        new AccountNotFoundException('not found')                                            | 404            | 'deposit/account-not-found'            | 'Requested account was not found.'
        new RuntimeException('unexpected')                                                   | 500            | 'deposit/internal-server-error'        | 'Internal Server Error'
    }

    def "create demand deposit account returns 400 problem detail for invalid customer id header"() {
        when:
        def response = mockMvc.perform(post("/demand-deposit-accounts")
            .header("x-customer-id", 'invalid-uuid')
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content('{"idempotencyKey":"idem-controller-header-001"}'))

        then:
        0 * demandDepositAccountService._
        response.andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value('deposit/invalid-request'))
            .andExpect(jsonPath('$.detail').value('Request validation failed.'))
    }

    def "create demand deposit account returns safe generic detail for unexpected server error"() {
        given:
        def customerId = GUID.v7().toUUID()

        when:
        def response = mockMvc.perform(post("/demand-deposit-accounts")
            .header("x-customer-id", customerId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content('{"idempotencyKey":"idem-controller-unexpected-001"}'))

        then:
        1 * demandDepositAccountService.createMainAccount(_ as CreateDemandDepositAccountCommand) >> {
            throw new RuntimeException("internal-only: simulated stack failure")
        }
        response.andExpect(status().isInternalServerError())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value('deposit/internal-server-error'))
            .andExpect(jsonPath('$.detail').value('Internal Server Error'))
    }
}
