package com.examples.deposit.integration

import com.examples.deposit.domain.DemandDepositAccountStatus
import com.examples.deposit.repository.AccountCreationIdempotencyRepository
import com.examples.deposit.repository.DemandDepositAccountRepository
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
class DemandDepositAccountCreationIntegrationSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    DemandDepositAccountRepository demandDepositAccountRepository

    @Autowired
    AccountCreationIdempotencyRepository accountCreationIdempotencyRepository

    def setup() {
        accountCreationIdempotencyRepository.deleteAll()
        demandDepositAccountRepository.deleteAll()
    }

    def "full create flow creates pending account and replays by idempotency key"() {
        given:
        UUID customerId = UUID.fromString("10000000-0000-0000-0000-000000000001")
        String idempotencyKey = "it-create-001"

        when:
        mockMvc.perform(post("/demand-deposit-accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-customer-id", customerId.toString())
            .content('{"idempotencyKey":"it-create-001"}'))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.customerId').value(customerId.toString()))
            .andExpect(jsonPath('$.status').value('PENDING_VERIFICATION'))
            .andReturn()

        UUID accountId = demandDepositAccountRepository.findByCustomerId(customerId).orElseThrow().id

        mockMvc.perform(post("/demand-deposit-accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-customer-id", customerId.toString())
            .content('{"idempotencyKey":"it-create-001"}'))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.customerId').value(customerId.toString()))
            .andExpect(jsonPath('$.status').value('PENDING_VERIFICATION'))
            .andExpect(jsonPath('$.accountId').value(accountId.toString()))
            .andReturn()

        then:
        demandDepositAccountRepository.count() == 1
        accountCreationIdempotencyRepository.count() == 1
        demandDepositAccountRepository.findById(accountId).orElseThrow().status ==
            DemandDepositAccountStatus.PENDING_VERIFICATION
    }
}
