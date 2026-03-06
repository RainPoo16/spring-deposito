package com.examples.deposit.integration

import com.examples.deposit.service.DemandDepositAccountService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import java.util.UUID

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class DemandDepositAccountActivationIntegrationSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    DemandDepositAccountService demandDepositAccountService

    @Autowired
    com.examples.deposit.repository.DemandDepositAccountRepository demandDepositAccountRepository

    @Autowired
    com.examples.deposit.repository.AccountCreationIdempotencyRepository accountCreationIdempotencyRepository

    def setup() {
        accountCreationIdempotencyRepository.deleteAll()
        demandDepositAccountRepository.deleteAll()
    }

    def "activation flow transitions pending account to active only once"() {
        given:
        UUID customerId = UUID.fromString("10000000-0000-0000-0000-000000000002")

        mockMvc.perform(post("/demand-deposit-accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-customer-id", customerId.toString())
            .content('{"idempotencyKey":"it-activate-001"}'))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.customerId').value(customerId.toString()))
            .andExpect(jsonPath('$.status').value('PENDING_VERIFICATION'))
            .andReturn()

        UUID accountUuid = demandDepositAccountRepository.findByCustomerId(customerId).orElseThrow().id
        Long initialVersion = demandDepositAccountRepository.findById(accountUuid).orElseThrow().version

        when:
        def firstActivation = demandDepositAccountService.activateAccountIfEligible(accountUuid)
        def secondActivation = demandDepositAccountService.activateAccountIfEligible(accountUuid)
        def reloaded = demandDepositAccountRepository.findById(accountUuid).orElseThrow()

        then:
        firstActivation.status.name() == 'ACTIVE'
        secondActivation.status.name() == 'ACTIVE'
        reloaded.status.name() == 'ACTIVE'
        reloaded.version == initialVersion + 1
    }
}
