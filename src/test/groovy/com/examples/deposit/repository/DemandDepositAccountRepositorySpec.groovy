package com.examples.deposit.repository

import com.examples.deposit.domain.DemandDepositAccount
import com.examples.deposit.domain.DemandDepositAccountStatus
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import spock.lang.Specification

import java.util.UUID

@DataJpaTest
class DemandDepositAccountRepositorySpec extends Specification {

    @Autowired
    DemandDepositAccountRepository demandDepositAccountRepository

    @Autowired
    EntityManager entityManager

    def "persists and loads account with status stored as string"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = DemandDepositAccount.create(customerId, DemandDepositAccountStatus.PENDING_VERIFICATION)

        when:
        DemandDepositAccount persisted = demandDepositAccountRepository.saveAndFlush(account)
        entityManager.clear()

        then:
        demandDepositAccountRepository.findById(persisted.id).isPresent()
        demandDepositAccountRepository.findById(persisted.id).get().status == DemandDepositAccountStatus.PENDING_VERIFICATION

        and:
        String persistedStatus = (String) entityManager
            .createNativeQuery("SELECT status FROM demand_deposit_account WHERE id = :id")
            .setParameter("id", persisted.id)
            .getSingleResult()
        persistedStatus == "PENDING_VERIFICATION"
    }

    def "increments version when entity is updated"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount original = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.PENDING_VERIFICATION)
        )
        entityManager.clear()

        when:
        DemandDepositAccount account = demandDepositAccountRepository.findById(original.id).orElseThrow()
        Long originalVersion = account.version
        account.activate()
        DemandDepositAccount updated = demandDepositAccountRepository.saveAndFlush(account)

        then:
        updated.version == originalVersion + 1
    }
}
