package com.examples.deposit.integration

import com.examples.deposit.domain.AccountBlockStatus
import com.examples.deposit.repository.AccountCreationIdempotencyRepository
import com.examples.deposit.repository.DemandDepositAccountBlockRepository
import com.examples.deposit.repository.DemandDepositAccountRepository
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class DemandDepositAccountBlockIntegrationSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    DemandDepositAccountRepository demandDepositAccountRepository

    @Autowired
    DemandDepositAccountBlockRepository demandDepositAccountBlockRepository

    @Autowired
    AccountCreationIdempotencyRepository accountCreationIdempotencyRepository

    def setup() {
        demandDepositAccountBlockRepository.deleteAll()
        accountCreationIdempotencyRepository.deleteAll()
        demandDepositAccountRepository.deleteAll()
    }

    def cleanup() {
        demandDepositAccountBlockRepository.deleteAll()
        accountCreationIdempotencyRepository.deleteAll()
        demandDepositAccountRepository.deleteAll()
    }

    def "full create-block flow persists expected fields and pending status"() {
        given:
        UUID customerId = UUID.fromString("21000000-0000-0000-0000-000000000001")
        UUID accountId = createAccount(customerId, "it-block-create-001")

        when:
        def result = createBlock(customerId, accountId, '''
            {
              "blockCode": "ACC",
              "effectiveDate": "2026-03-12",
              "expiryDate": "2026-03-22",
              "remark": "integration create"
            }
        ''')

        then:
        result.andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.accountId').value(accountId.toString()))
            .andExpect(jsonPath('$.blockCode').value('ACC'))
            .andExpect(jsonPath('$.status').value('PENDING'))
            .andExpect(jsonPath('$.effectiveDate').value('2026-03-12'))
            .andExpect(jsonPath('$.expiryDate').value('2026-03-22'))
            .andExpect(jsonPath('$.remark').value('integration create'))

        and:
        demandDepositAccountBlockRepository.count() == 1
        def persisted = demandDepositAccountBlockRepository.findAll().first()
        persisted.accountId == accountId
        persisted.status == AccountBlockStatus.PENDING
        persisted.effectiveDate == LocalDate.of(2026, 3, 12)
        persisted.expiryDate == LocalDate.of(2026, 3, 22)
        persisted.remark == 'integration create'
    }

    def "duplicate and overlapping block requests are rejected end-to-end"() {
        given:
        UUID customerId = UUID.fromString("21000000-0000-0000-0000-000000000002")
        UUID accountId = createAccount(customerId, "it-block-overlap-001")

        createBlock(customerId, accountId, '''
            {
              "blockCode": "ACC",
              "effectiveDate": "2026-03-12",
              "expiryDate": "2026-03-22",
              "remark": "first"
            }
        ''').andExpect(status().isCreated())

        when:
        def duplicateResult = createBlock(customerId, accountId, '''
            {
              "blockCode": "ACC",
              "effectiveDate": "2026-03-12",
              "expiryDate": "2026-03-22",
              "remark": "duplicate"
            }
        ''')

        def overlapResult = createBlock(customerId, accountId, '''
            {
              "blockCode": "ACC",
              "effectiveDate": "2026-03-20",
              "expiryDate": "2026-03-25",
              "remark": "overlap"
            }
        ''')

        then:
        duplicateResult.andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value('deposit/duplicate-or-overlapping-block'))
            .andExpect(jsonPath('$.title').value('Duplicate or overlapping block'))
            .andExpect(jsonPath('$.status').value(422))

        overlapResult.andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value('deposit/duplicate-or-overlapping-block'))
            .andExpect(jsonPath('$.status').value(422))

        and:
        demandDepositAccountBlockRepository.count() == 1
    }

    def "foreign customer cannot create block on another customer's account"() {
        given:
        UUID ownerCustomerId = UUID.fromString("21000000-0000-0000-0000-000000000005")
        UUID foreignCustomerId = UUID.fromString("21000000-0000-0000-0000-000000000006")
        UUID accountId = createAccount(ownerCustomerId, "it-block-foreign-owner-001")

        when:
        def result = createBlock(foreignCustomerId, accountId, '''
            {
              "blockCode": "ACC",
              "effectiveDate": "2026-03-12",
              "expiryDate": "2026-03-22",
              "remark": "foreign customer"
            }
        ''')

        then:
        result.andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value('deposit/account-not-found'))
            .andExpect(jsonPath('$.title').value('Account not found'))
            .andExpect(jsonPath('$.status').value(404))
            .andExpect(jsonPath('$.detail').value('Demand deposit account was not found'))

        and:
        demandDepositAccountBlockRepository.count() == 0
    }

    def "create block on non-existent account returns 404 account-not-found"() {
        given:
        UUID customerId = UUID.fromString("21000000-0000-0000-0000-000000000007")
        UUID missingAccountId = UUID.fromString("22000000-0000-0000-0000-000000000001")

        when:
        def result = createBlock(customerId, missingAccountId, '''
            {
              "blockCode": "ACC",
              "effectiveDate": "2026-03-12",
              "expiryDate": "2026-03-22",
              "remark": "missing account"
            }
        ''')

        then:
        result.andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value('deposit/account-not-found'))
            .andExpect(jsonPath('$.title').value('Account not found'))
            .andExpect(jsonPath('$.status').value(404))
            .andExpect(jsonPath('$.detail').value('Demand deposit account was not found'))

        and:
        demandDepositAccountBlockRepository.count() == 0
    }

    def "foreign customer cannot update or cancel another customer's block"() {
        given:
        UUID ownerCustomerId = UUID.fromString("21000000-0000-0000-0000-000000000008")
        UUID foreignCustomerId = UUID.fromString("21000000-0000-0000-0000-000000000009")
        UUID accountId = createAccount(ownerCustomerId, "it-block-foreign-update-cancel-create-001")

        createBlock(ownerCustomerId, accountId, '''
            {
              "blockCode": "ACC",
              "effectiveDate": "2026-03-12",
              "expiryDate": "2026-03-22",
              "remark": "owner-created"
            }
        ''').andExpect(status().isCreated())
        UUID blockId = demandDepositAccountBlockRepository.findAll().first().id

        when:
        def updateResult = mockMvc.perform(put("/demand-deposit-accounts/${accountId}/blocks/${blockId}")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-customer-id", foreignCustomerId.toString())
            .content('''
                {
                  "effectiveDate": "2026-03-13",
                  "expiryDate": "2026-03-24",
                  "remark": "foreign update"
                }
            '''))
        def cancelResult = mockMvc.perform(patch("/demand-deposit-accounts/${accountId}/blocks/${blockId}/cancel")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-customer-id", foreignCustomerId.toString()))

        then:
        updateResult.andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value('deposit/account-not-found'))
            .andExpect(jsonPath('$.title').value('Account not found'))
            .andExpect(jsonPath('$.status').value(404))
            .andExpect(jsonPath('$.detail').value('Demand deposit account was not found'))

        cancelResult.andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value('deposit/account-not-found'))
            .andExpect(jsonPath('$.title').value('Account not found'))
            .andExpect(jsonPath('$.status').value(404))
            .andExpect(jsonPath('$.detail').value('Demand deposit account was not found'))

        and:
        demandDepositAccountBlockRepository.findById(blockId).orElseThrow().status == AccountBlockStatus.PENDING
    }

    def "block status lifecycle transitions are persisted and queryable"() {
        given:
        UUID customerId = UUID.fromString("21000000-0000-0000-0000-000000000003")
        UUID accountId = createAccount(customerId, "it-block-update-cancel-001")

                createBlock(customerId, accountId, '''
            {
              "blockCode": "ACC",
              "effectiveDate": "2026-03-11",
              "expiryDate": "2026-03-21",
              "remark": "before update"
            }
                ''').andExpect(status().isCreated())
                UUID blockId = demandDepositAccountBlockRepository.findAll().first().id
            def createdPersisted = demandDepositAccountBlockRepository.findById(blockId).orElseThrow()
            Long createVersion = createdPersisted.version

        when:
        def updateResult = mockMvc.perform(put("/demand-deposit-accounts/${accountId}/blocks/${blockId}")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-customer-id", customerId.toString())
            .content('''
                {
                  "effectiveDate": "2026-03-13",
                  "expiryDate": "2026-03-24",
                  "remark": "after update"
                }
            '''))

        then:
        updateResult.andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath('$.blockId').value(blockId.toString()))
            .andExpect(jsonPath('$.status').value('PENDING'))
            .andExpect(jsonPath('$.effectiveDate').value('2026-03-13'))
            .andExpect(jsonPath('$.expiryDate').value('2026-03-24'))
            .andExpect(jsonPath('$.remark').value('after update'))

        and:
        def updatedPersisted = demandDepositAccountBlockRepository.findById(blockId).orElseThrow()
        createdPersisted.status == AccountBlockStatus.PENDING
        createdPersisted.version == createVersion
        updatedPersisted.id == blockId
        updatedPersisted.status == AccountBlockStatus.PENDING
        updatedPersisted.effectiveDate == LocalDate.of(2026, 3, 13)
        updatedPersisted.expiryDate == LocalDate.of(2026, 3, 24)
        updatedPersisted.remark == 'after update'
        updatedPersisted.version == createVersion + 1
        demandDepositAccountBlockRepository.count() == 1

        when:
        def cancelResult = mockMvc.perform(patch("/demand-deposit-accounts/${accountId}/blocks/${blockId}/cancel")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-customer-id", customerId.toString()))

        then:
        cancelResult.andExpect(status().isNoContent())
            .andExpect(content().string(''))

        and:
        def cancelledPersisted = demandDepositAccountBlockRepository.findById(blockId).orElseThrow()
        cancelledPersisted.id == blockId
        cancelledPersisted.status == AccountBlockStatus.CANCELLED
        cancelledPersisted.effectiveDate == LocalDate.of(2026, 3, 13)
        cancelledPersisted.expiryDate == LocalDate.of(2026, 3, 24)
        cancelledPersisted.remark == 'after update'
        cancelledPersisted.version == createVersion + 2
        demandDepositAccountBlockRepository.count() == 1
    }

    def "malformed and business-rule violations return consistent problem details"() {
        given:
        UUID customerId = UUID.fromString("21000000-0000-0000-0000-000000000004")
        UUID accountId = createAccount(customerId, "it-block-errors-001")

        when:
        def malformedResult = mockMvc.perform(post("/demand-deposit-accounts/${accountId}/blocks")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-customer-id", customerId.toString())
            .content('{"blockCode":'))

        def businessRuleResult = createBlock(customerId, accountId, '''
            {
              "blockCode": "ACB",
              "effectiveDate": "2026-03-12",
              "expiryDate": "2026-03-22",
              "remark": "not customer allowed"
            }
        ''')

        then:
        malformedResult.andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value('deposit/malformed-request'))
            .andExpect(jsonPath('$.title').value('Malformed request'))
            .andExpect(jsonPath('$.status').value(400))

        businessRuleResult.andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath('$.type').value('deposit/block-not-eligible-for-operation'))
            .andExpect(jsonPath('$.title').value('Block not eligible for operation'))
            .andExpect(jsonPath('$.status').value(422))
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

    private ResultActions createBlock(UUID customerId, UUID accountId, String body) {
        return mockMvc.perform(post("/demand-deposit-accounts/${accountId}/blocks")
            .contentType(MediaType.APPLICATION_JSON)
            .header('x-customer-id', customerId.toString())
            .content(body))
    }
}
