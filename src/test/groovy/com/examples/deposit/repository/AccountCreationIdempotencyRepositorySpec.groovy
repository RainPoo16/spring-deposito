package com.examples.deposit.repository

import com.examples.deposit.domain.AccountCreationIdempotency
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Specification

import java.util.UUID

@DataJpaTest
class AccountCreationIdempotencyRepositorySpec extends Specification {

    @Autowired
    AccountCreationIdempotencyRepository accountCreationIdempotencyRepository

    def "finds record by customer id and idempotency key"() {
        given:
        UUID customerId = UUID.randomUUID()
        String idempotencyKey = "idem-key-001"
        UUID accountId = UUID.randomUUID()

        and:
        AccountCreationIdempotency saved = accountCreationIdempotencyRepository.saveAndFlush(
            AccountCreationIdempotency.create(customerId, idempotencyKey, accountId)
        )

        when:
        def found = accountCreationIdempotencyRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey)

        then:
        found.isPresent()
        found.get().id == saved.id
        found.get().accountId == accountId
    }

    def "enforces uniqueness of customer id and idempotency key"() {
        given:
        UUID customerId = UUID.randomUUID()
        String idempotencyKey = "idem-key-duplicate"

        when:
        accountCreationIdempotencyRepository.saveAndFlush(
            AccountCreationIdempotency.create(customerId, idempotencyKey, UUID.randomUUID())
        )
        accountCreationIdempotencyRepository.saveAndFlush(
            AccountCreationIdempotency.create(customerId, idempotencyKey, UUID.randomUUID())
        )

        then:
        thrown(DataIntegrityViolationException)
    }
}
