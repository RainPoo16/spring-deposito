package com.examples.deposit.repository

import com.github.f4b6a3.uuid.alt.GUID
import com.examples.deposit.domain.DemandDepositAccount
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Specification

@DataJpaTest
class DemandDepositAccountRepositorySpec extends Specification {

    @Autowired
    private DemandDepositAccountRepository repository

    def "findByCustomerIdAndIdempotencyKey returns persisted account"() {
        given:
        def customerId = GUID.v7().toUUID()
        def idempotencyKey = "idem-repository-find-001"
        def account = DemandDepositAccount.createPending(customerId, idempotencyKey)
        repository.saveAndFlush(account)

        when:
        def result = repository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey)

        then:
        result.present
        result.get().id == account.id
    }

    def "saving duplicate customer and idempotency key is rejected by database constraint"() {
        given:
        def customerId = GUID.v7().toUUID()
        def idempotencyKey = "idem-repository-unique-001"
        repository.saveAndFlush(DemandDepositAccount.createPending(customerId, idempotencyKey))

        when:
        repository.saveAndFlush(DemandDepositAccount.createPending(customerId, idempotencyKey))

        then:
        thrown(DataIntegrityViolationException)
    }
}
