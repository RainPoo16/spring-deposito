package com.examples.deposit.repository

import com.examples.deposit.domain.DemandDepositAccount
import com.examples.deposit.domain.DemandDepositAccountStatus
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import spock.lang.Specification

import java.math.BigDecimal
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

    def "findByIdAndCustomerId returns account only for matching owner"() {
        given:
        UUID ownerCustomerId = UUID.randomUUID()
        UUID anotherCustomerId = UUID.randomUUID()
        DemandDepositAccount persisted = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(ownerCustomerId, DemandDepositAccountStatus.PENDING_VERIFICATION)
        )

        when:
        def foundForOwner = demandDepositAccountRepository.findByIdAndCustomerId(persisted.id, ownerCustomerId)
        def foundForAnotherCustomer = demandDepositAccountRepository.findByIdAndCustomerId(persisted.id, anotherCustomerId)

        then:
        foundForOwner.isPresent()
        foundForOwner.get().id == persisted.id
        foundForAnotherCustomer.isEmpty()
    }

    def "persists current and available balances with deterministic defaults"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        account.applyCredit(new BigDecimal("120.00"), "CASH_DEPOSIT")
        account.applyDebit(new BigDecimal("20.00"), "POS_PURCHASE")

        when:
        DemandDepositAccount persisted = demandDepositAccountRepository.saveAndFlush(account)
        entityManager.clear()

        then:
        DemandDepositAccount reloaded = demandDepositAccountRepository.findById(persisted.id).orElseThrow()
        reloaded.currentBalance == new BigDecimal("100.00")
        reloaded.availableBalance == new BigDecimal("100.00")

        and:
        BigDecimal persistedCurrentBalance = (BigDecimal) entityManager
            .createNativeQuery("SELECT current_balance FROM demand_deposit_account WHERE id = :id")
            .setParameter("id", persisted.id)
            .getSingleResult()
        BigDecimal persistedAvailableBalance = (BigDecimal) entityManager
            .createNativeQuery("SELECT available_balance FROM demand_deposit_account WHERE id = :id")
            .setParameter("id", persisted.id)
            .getSingleResult()
        persistedCurrentBalance == new BigDecimal("100.00")
        persistedAvailableBalance == new BigDecimal("100.00")
    }

    def "findByIdAndCustomerIdForUpdate returns account only for matching owner"() {
        given:
        UUID ownerCustomerId = UUID.randomUUID()
        UUID anotherCustomerId = UUID.randomUUID()
        DemandDepositAccount persisted = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(ownerCustomerId, DemandDepositAccountStatus.ACTIVE)
        )

        when:
        def foundForOwner = demandDepositAccountRepository.findByIdAndCustomerIdForUpdate(persisted.id, ownerCustomerId)
        def foundForAnotherCustomer = demandDepositAccountRepository.findByIdAndCustomerIdForUpdate(persisted.id, anotherCustomerId)

        then:
        foundForOwner.isPresent()
        foundForOwner.get().id == persisted.id
        foundForAnotherCustomer.isEmpty()
    }
}
