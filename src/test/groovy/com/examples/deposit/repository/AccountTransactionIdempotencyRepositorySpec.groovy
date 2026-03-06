package com.examples.deposit.repository

import com.examples.deposit.domain.AccountTransactionIdempotency
import com.examples.deposit.domain.DemandDepositAccount
import com.examples.deposit.domain.DemandDepositAccountStatus
import com.examples.deposit.domain.DemandDepositAccountTransaction
import com.examples.deposit.domain.TransactionType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Specification

import java.time.Instant
import java.util.UUID
import java.math.BigDecimal

@DataJpaTest
class AccountTransactionIdempotencyRepositorySpec extends Specification {

    @Autowired
    AccountTransactionIdempotencyRepository accountTransactionIdempotencyRepository

    @Autowired
    DemandDepositAccountRepository demandDepositAccountRepository

    @Autowired
    DemandDepositAccountTransactionRepository demandDepositAccountTransactionRepository

    def "finds idempotency record by customer key and reference"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )
        DemandDepositAccountTransaction transaction = demandDepositAccountTransactionRepository.saveAndFlush(
            DemandDepositAccountTransaction.create(
                account.id,
                customerId,
                TransactionType.CREDIT,
                new BigDecimal("25.00"),
                "CASH_DEPOSIT",
                "reference-001",
                "idem-key-001",
                Instant.parse("2026-03-06T12:00:00Z")
            )
        )

        and:
        AccountTransactionIdempotency saved = accountTransactionIdempotencyRepository.saveAndFlush(
            AccountTransactionIdempotency.create(customerId, account.id, "idem-key-001", "reference-001", transaction.id)
        )

        when:
        def found = accountTransactionIdempotencyRepository.findByCustomerIdAndIdempotencyKeyAndReferenceId(
            customerId,
            "idem-key-001",
            "reference-001"
        )

        then:
        found.isPresent()
        found.get().id == saved.id
        found.get().transactionId == transaction.id
    }

    def "enforces uniqueness of customer idempotency key and reference id"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )
        DemandDepositAccountTransaction firstTransaction = demandDepositAccountTransactionRepository.saveAndFlush(
            DemandDepositAccountTransaction.create(
                account.id,
                customerId,
                TransactionType.DEBIT,
                new BigDecimal("5.00"),
                "POS_PURCHASE",
                "reference-duplicate",
                "idem-key-duplicate",
                Instant.parse("2026-03-06T13:00:00Z")
            )
        )
        DemandDepositAccountTransaction secondTransaction = demandDepositAccountTransactionRepository.saveAndFlush(
            DemandDepositAccountTransaction.create(
                account.id,
                customerId,
                TransactionType.DEBIT,
                new BigDecimal("7.00"),
                "POS_PURCHASE",
                "reference-duplicate-2",
                "idem-key-duplicate-2",
                Instant.parse("2026-03-06T14:00:00Z")
            )
        )

        when:
        accountTransactionIdempotencyRepository.saveAndFlush(
            AccountTransactionIdempotency.create(
                customerId,
                account.id,
                "idem-key-duplicate",
                "reference-duplicate",
                firstTransaction.id
            )
        )
        accountTransactionIdempotencyRepository.saveAndFlush(
            AccountTransactionIdempotency.create(
                customerId,
                account.id,
                "idem-key-duplicate",
                "reference-duplicate",
                secondTransaction.id
            )
        )

        then:
        thrown(DataIntegrityViolationException)
    }
}
