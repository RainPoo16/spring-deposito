package com.examples.deposit.repository

import com.examples.deposit.domain.DemandDepositAccount
import com.examples.deposit.domain.DemandDepositAccountStatus
import com.examples.deposit.domain.DemandDepositAccountTransaction
import com.examples.deposit.domain.TransactionType
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Specification

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@DataJpaTest
class DemandDepositAccountTransactionRepositorySpec extends Specification {

    @Autowired
    DemandDepositAccountRepository demandDepositAccountRepository

    @Autowired
    DemandDepositAccountTransactionRepository demandDepositAccountTransactionRepository

    @Autowired
    EntityManager entityManager

    def "persists posted transaction with enum stored as string"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )

        and:
        DemandDepositAccountTransaction transaction = DemandDepositAccountTransaction.create(
            account.id,
            customerId,
            TransactionType.CREDIT,
            new BigDecimal("55.25"),
            "CASH_DEPOSIT",
            "reference-001",
            "idem-001",
            Instant.parse("2026-03-06T12:00:00Z")
        )

        when:
        DemandDepositAccountTransaction persisted = demandDepositAccountTransactionRepository.saveAndFlush(transaction)
        entityManager.clear()

        then:
        def loaded = demandDepositAccountTransactionRepository.findById(persisted.id)
        loaded.isPresent()
        loaded.get().transactionType == TransactionType.CREDIT
        loaded.get().amount == new BigDecimal("55.25")

        and:
        String persistedType = (String) entityManager
            .createNativeQuery("SELECT transaction_type FROM demand_deposit_account_transaction WHERE id = :id")
            .setParameter("id", persisted.id)
            .getSingleResult()
        persistedType == "CREDIT"
    }

    def "findByAccountIdAndReferenceId resolves persisted posting"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )

        and:
        DemandDepositAccountTransaction saved = demandDepositAccountTransactionRepository.saveAndFlush(
            DemandDepositAccountTransaction.create(
                account.id,
                customerId,
                TransactionType.DEBIT,
                new BigDecimal("10.00"),
                "POS_PURCHASE",
                "reference-lookup",
                "idem-lookup",
                Instant.parse("2026-03-06T13:00:00Z")
            )
        )

        when:
        def found = demandDepositAccountTransactionRepository.findByAccountIdAndReferenceId(account.id, "reference-lookup")

        then:
        found.isPresent()
        found.get().id == saved.id
    }

    def "rejects duplicate transaction identity for same account and reference"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )

        and:
        demandDepositAccountTransactionRepository.saveAndFlush(
            DemandDepositAccountTransaction.create(
                account.id,
                customerId,
                TransactionType.CREDIT,
                new BigDecimal("20.00"),
                "CASH_DEPOSIT",
                "reference-duplicate",
                "idem-duplicate-1",
                Instant.parse("2026-03-06T14:00:00Z")
            )
        )

        when:
        demandDepositAccountTransactionRepository.saveAndFlush(
            DemandDepositAccountTransaction.create(
                account.id,
                customerId,
                TransactionType.DEBIT,
                new BigDecimal("5.00"),
                "POS_PURCHASE",
                "reference-duplicate",
                "idem-duplicate-2",
                Instant.parse("2026-03-06T15:00:00Z")
            )
        )

        then:
        thrown(DataIntegrityViolationException)
    }
}
