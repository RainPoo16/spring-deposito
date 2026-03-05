package com.examples.deposit.integration

import com.examples.deposit.domain.DemandDepositAccountStatus
import com.examples.deposit.exception.AccountLifecycleException
import com.examples.deposit.service.DemandDepositAccountService
import com.github.f4b6a3.uuid.alt.GUID
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import spock.lang.Specification

import java.util.UUID

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DemandDepositAccountCreationIntegrationSpec extends Specification {

    @Autowired
    private MockMvc mockMvc

    @Autowired
    private DemandDepositAccountService demandDepositAccountService

    @Autowired
    private JdbcTemplate jdbcTemplate

    @Autowired
    private ObjectMapper objectMapper

    def "create endpoint persists once and replay returns same account while scoped DB count remains one"() {
        given:
        UUID customerId = GUID.v7().toUUID()
        String idempotencyKey = "idem-it-create-replay-${GUID.v7()}"
        String requestBody = "{\"idempotencyKey\":\"${idempotencyKey}\"}"

        when:
        MvcResult firstResult = mockMvc.perform(post("/demand-deposit-accounts")
            .header("x-customer-id", customerId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.customerId').value(customerId.toString()))
            .andExpect(jsonPath('$.status').value('PENDING_VERIFICATION'))
            .andReturn()

        MvcResult secondResult = mockMvc.perform(post("/demand-deposit-accounts")
            .header("x-customer-id", customerId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.customerId').value(customerId.toString()))
            .andExpect(jsonPath('$.status').value('PENDING_VERIFICATION'))
            .andReturn()

        then:
        UUID firstAccountId = readAccountId(firstResult, objectMapper)
        UUID secondAccountId = readAccountId(secondResult, objectMapper)

        Integer accountCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM demand_deposit_accounts
            WHERE customer_id = ?
              AND idempotency_key = ?
            """,
            Integer.class,
            customerId,
            idempotencyKey
        )

        UUID persistedAccountId = jdbcTemplate.queryForObject(
            """
            SELECT id
            FROM demand_deposit_accounts
            WHERE customer_id = ?
              AND idempotency_key = ?
            """,
            UUID.class,
            customerId,
            idempotencyKey
        )

        firstAccountId == secondAccountId
        accountCount == 1
        persistedAccountId == firstAccountId
    }

    def "activation transitions to ACTIVE once and second activation enforces one-time lifecycle rule"() {
        given:
        UUID customerId = GUID.v7().toUUID()
        String idempotencyKey = "idem-it-activate-once-${GUID.v7()}"
        String requestBody = "{\"idempotencyKey\":\"${idempotencyKey}\"}"

        MvcResult createResult = mockMvc.perform(post("/demand-deposit-accounts")
            .header("x-customer-id", customerId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.status').value('PENDING_VERIFICATION'))
            .andReturn()

        UUID accountId = readAccountId(createResult, objectMapper)

        when:
        def activated = demandDepositAccountService.activateAccount(accountId)

        then:
        activated.accountId() == accountId
        activated.status() == DemandDepositAccountStatus.ACTIVE

        when:
        demandDepositAccountService.activateAccount(accountId)

        then:
        thrown(AccountLifecycleException)

        and:
        String storedStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM demand_deposit_accounts WHERE id = ?",
            String.class,
            accountId
        )
        Integer activationEventCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM demand_deposit_account_lifecycle_events
            WHERE account_id = ?
              AND event_type = 'ACCOUNT_ACTIVATED'
            """,
            Integer.class,
            accountId
        )

        storedStatus == 'ACTIVE'
        activationEventCount == 1
    }

    private static UUID readAccountId(MvcResult result, ObjectMapper objectMapper) {
        def jsonNode = objectMapper.readTree(result.response.contentAsString)
        return UUID.fromString(jsonNode.get("accountId").asText())
    }
}
