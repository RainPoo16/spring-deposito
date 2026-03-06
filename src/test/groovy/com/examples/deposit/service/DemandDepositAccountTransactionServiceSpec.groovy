package com.examples.deposit.service

import com.examples.deposit.domain.AccountBlockStatus
import com.examples.deposit.domain.BlockCode
import com.examples.deposit.domain.BlockRequestedBy
import com.examples.deposit.domain.DemandDepositAccount
import com.examples.deposit.domain.DemandDepositAccountBlock
import com.examples.deposit.domain.DemandDepositAccountStatus
import com.examples.deposit.domain.TransactionType
import com.examples.deposit.domain.exception.InsufficientAvailableBalanceException
import com.examples.deposit.domain.exception.TransactionBlockedException
import com.examples.deposit.domain.exception.TransactionIdempotencyConflictException
import com.examples.deposit.domain.exception.TransactionNotAllowedForAccountStatusException
import com.examples.deposit.repository.AccountTransactionIdempotencyRepository
import com.examples.deposit.repository.DemandDepositAccountBlockRepository
import com.examples.deposit.repository.DemandDepositAccountRepository
import com.examples.deposit.repository.DemandDepositAccountTransactionRepository
import com.examples.deposit.service.dto.PostCreditTransactionCommand
import com.examples.deposit.service.dto.PostDebitTransactionCommand
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
class DemandDepositAccountTransactionServiceSpec extends Specification {

    @Autowired
    DemandDepositAccountTransactionService demandDepositAccountTransactionService

    @Autowired
    DemandDepositAccountRepository demandDepositAccountRepository

    @Autowired
    DemandDepositAccountTransactionRepository demandDepositAccountTransactionRepository

    @Autowired
    AccountTransactionIdempotencyRepository accountTransactionIdempotencyRepository

    @Autowired
    DemandDepositAccountBlockRepository demandDepositAccountBlockRepository

    def setup() {
        accountTransactionIdempotencyRepository.deleteAll()
        demandDepositAccountTransactionRepository.deleteAll()
        demandDepositAccountBlockRepository.deleteAll()
        demandDepositAccountRepository.deleteAll()
    }

    def "postCredit persists posting and updates balances"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )

        when:
        def result = demandDepositAccountTransactionService.postCredit(
            new PostCreditTransactionCommand(
                account.id,
                customerId,
                new BigDecimal("20.00"),
                "CASH_DEPOSIT",
                "credit-ref-001",
                "credit-idem-001"
            )
        )

        then:
        result.transactionType == TransactionType.CREDIT
        result.transactionId != null
        result.accountId == account.id
        result.referenceId == "credit-ref-001"
        result.currentBalance.compareTo(new BigDecimal("20.00")) == 0
        result.availableBalance.compareTo(new BigDecimal("20.00")) == 0
        demandDepositAccountTransactionRepository.count() == 1
        accountTransactionIdempotencyRepository.count() == 1

        and:
        def reloaded = demandDepositAccountRepository.findById(account.id).orElseThrow()
        reloaded.currentBalance.compareTo(new BigDecimal("20.00")) == 0
        reloaded.availableBalance.compareTo(new BigDecimal("20.00")) == 0
    }

    def "postDebit persists posting and updates balances"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        account.applyCredit(new BigDecimal("70.00"), "CASH_DEPOSIT")
        account = demandDepositAccountRepository.saveAndFlush(account)

        when:
        def result = demandDepositAccountTransactionService.postDebit(
            new PostDebitTransactionCommand(
                account.id,
                customerId,
                new BigDecimal("15.00"),
                "POS_PURCHASE",
                "debit-ref-001",
                "debit-idem-001"
            )
        )

        then:
        result.transactionType == TransactionType.DEBIT
        result.currentBalance.compareTo(new BigDecimal("55.00")) == 0
        result.availableBalance.compareTo(new BigDecimal("55.00")) == 0
        demandDepositAccountTransactionRepository.count() == 1
        accountTransactionIdempotencyRepository.count() == 1
    }

    def "postDebit rejects insufficient balance and keeps balances unchanged"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        account.applyCredit(new BigDecimal("10.00"), "CASH_DEPOSIT")
        account = demandDepositAccountRepository.saveAndFlush(account)

        when:
        demandDepositAccountTransactionService.postDebit(
            new PostDebitTransactionCommand(
                account.id,
                customerId,
                new BigDecimal("11.00"),
                "POS_PURCHASE",
                "debit-ref-insufficient-001",
                "debit-idem-insufficient-001"
            )
        )

        then:
        thrown(InsufficientAvailableBalanceException)
        demandDepositAccountTransactionRepository.count() == 0
        accountTransactionIdempotencyRepository.count() == 0

        and:
        def reloaded = demandDepositAccountRepository.findById(account.id).orElseThrow()
        reloaded.currentBalance.compareTo(new BigDecimal("10.00")) == 0
        reloaded.availableBalance.compareTo(new BigDecimal("10.00")) == 0
    }

    def "postCredit rejects when account status is not eligible"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.CLOSED)
        )

        when:
        demandDepositAccountTransactionService.postCredit(
            new PostCreditTransactionCommand(
                account.id,
                customerId,
                new BigDecimal("20.00"),
                "CASH_DEPOSIT",
                "credit-ref-status-001",
                "credit-idem-status-001"
            )
        )

        then:
        thrown(TransactionNotAllowedForAccountStatusException)
        demandDepositAccountTransactionRepository.count() == 0
        accountTransactionIdempotencyRepository.count() == 0
    }

    def "postCredit rejects when active credit restriction exists for required validation code"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )
        demandDepositAccountBlockRepository.saveAndFlush(
            DemandDepositAccountBlock.create(
                account.id,
                BlockCode.ACB,
                BlockRequestedBy.BANK,
                AccountBlockStatus.ACTIVE,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1),
                "active incoming restriction"
            )
        )

        when:
        demandDepositAccountTransactionService.postCredit(
            new PostCreditTransactionCommand(
                account.id,
                customerId,
                new BigDecimal("20.00"),
                "CASH_DEPOSIT",
                "credit-ref-block-001",
                "credit-idem-block-001"
            )
        )

        then:
        thrown(TransactionBlockedException)
        demandDepositAccountTransactionRepository.count() == 0
        accountTransactionIdempotencyRepository.count() == 0
    }

    def "postDebit rejects when active debit restriction exists for required validation code"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        account.applyCredit(new BigDecimal("30.00"), "CASH_DEPOSIT")
        account = demandDepositAccountRepository.saveAndFlush(account)
        demandDepositAccountBlockRepository.saveAndFlush(
            DemandDepositAccountBlock.create(
                account.id,
                BlockCode.ADB,
                BlockRequestedBy.BANK,
                AccountBlockStatus.ACTIVE,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1),
                "active outgoing restriction"
            )
        )

        when:
        demandDepositAccountTransactionService.postDebit(
            new PostDebitTransactionCommand(
                account.id,
                customerId,
                new BigDecimal("10.00"),
                "POS_PURCHASE",
                "debit-ref-block-001",
                "debit-idem-block-001"
            )
        )

        then:
        thrown(TransactionBlockedException)
        demandDepositAccountTransactionRepository.count() == 0
        accountTransactionIdempotencyRepository.count() == 0

        and:
        def reloaded = demandDepositAccountRepository.findById(account.id).orElseThrow()
        reloaded.currentBalance.compareTo(new BigDecimal("30.00")) == 0
    }

    def "replay with same idempotency identity returns existing posting without duplicate mutation"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )
        def command = new PostCreditTransactionCommand(
            account.id,
            customerId,
            new BigDecimal("12.50"),
            "CASH_DEPOSIT",
            "credit-ref-replay-001",
            "credit-idem-replay-001"
        )

        when:
        def first = demandDepositAccountTransactionService.postCredit(command)
        def replay = demandDepositAccountTransactionService.postCredit(command)

        then:
        replay.transactionId == first.transactionId
        replay.currentBalance.compareTo(new BigDecimal("12.50")) == 0
        replay.availableBalance.compareTo(new BigDecimal("12.50")) == 0
        demandDepositAccountTransactionRepository.count() == 1
        accountTransactionIdempotencyRepository.count() == 1
    }

    def "concurrent replay results in one posting and one balance mutation"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )
        def command = new PostCreditTransactionCommand(
            account.id,
            customerId,
            new BigDecimal("9.00"),
            "CASH_DEPOSIT",
            "credit-ref-race-001",
            "credit-idem-race-001"
        )

        int parallelRequests = 8
        ExecutorService executor = Executors.newFixedThreadPool(parallelRequests)
        CountDownLatch startGate = new CountDownLatch(1)
        List<Callable<UUID>> tasks = (1..parallelRequests).collect {
            return {
                startGate.await(5, TimeUnit.SECONDS)
                return demandDepositAccountTransactionService.postCredit(command).transactionId
            } as Callable<UUID>
        }

        when:
        def futures = tasks.collect { executor.submit(it) }
        startGate.countDown()
        List<UUID> returnedIds = futures.collect { it.get(10, TimeUnit.SECONDS) }

        then:
        returnedIds.toSet().size() == 1
        demandDepositAccountTransactionRepository.count() == 1
        accountTransactionIdempotencyRepository.count() == 1

        and:
        def reloaded = demandDepositAccountRepository.findById(account.id).orElseThrow()
        reloaded.currentBalance.compareTo(new BigDecimal("9.00")) == 0
        reloaded.availableBalance.compareTo(new BigDecimal("9.00")) == 0

        cleanup:
        executor.shutdownNow()
    }

    def "postCredit replay with same idempotency identity but different accountId throws conflict"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount accountA = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )
        DemandDepositAccount accountB = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )

        demandDepositAccountTransactionService.postCredit(
            new PostCreditTransactionCommand(
                accountA.id,
                customerId,
                new BigDecimal("15.00"),
                "CASH_DEPOSIT",
                "credit-ref-account-mismatch-001",
                "credit-idem-account-mismatch-001"
            )
        )

        when:
        demandDepositAccountTransactionService.postCredit(
            new PostCreditTransactionCommand(
                accountB.id,
                customerId,
                new BigDecimal("15.00"),
                "CASH_DEPOSIT",
                "credit-ref-account-mismatch-001",
                "credit-idem-account-mismatch-001"
            )
        )

        then:
        thrown(TransactionIdempotencyConflictException)
        demandDepositAccountTransactionRepository.count() == 1
        accountTransactionIdempotencyRepository.count() == 1
    }

    def "postCredit replay with mismatched amount or code under same idempotency identity throws conflict"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )

        demandDepositAccountTransactionService.postCredit(
            new PostCreditTransactionCommand(
                account.id,
                customerId,
                new BigDecimal("10.00"),
                "CASH_DEPOSIT",
                "credit-ref-payload-mismatch-001",
                "credit-idem-payload-mismatch-001"
            )
        )

        when:
        demandDepositAccountTransactionService.postCredit(
            new PostCreditTransactionCommand(
                account.id,
                customerId,
                replayAmount,
                replayCode,
                "credit-ref-payload-mismatch-001",
                "credit-idem-payload-mismatch-001"
            )
        )

        then:
        thrown(TransactionIdempotencyConflictException)
        demandDepositAccountTransactionRepository.count() == 1
        accountTransactionIdempotencyRepository.count() == 1

        where:
        replayAmount              | replayCode
        new BigDecimal("11.00")  | "CASH_DEPOSIT"
        new BigDecimal("10.00")  | "REVERSAL"
    }

    def "postDebit under same idempotency identity after credit throws conflict"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )

        demandDepositAccountTransactionService.postCredit(
            new PostCreditTransactionCommand(
                account.id,
                customerId,
                new BigDecimal("25.00"),
                "CASH_DEPOSIT",
                "txn-ref-type-mismatch-001",
                "txn-idem-type-mismatch-001"
            )
        )

        when:
        demandDepositAccountTransactionService.postDebit(
            new PostDebitTransactionCommand(
                account.id,
                customerId,
                new BigDecimal("25.00"),
                "POS_PURCHASE",
                "txn-ref-type-mismatch-001",
                "txn-idem-type-mismatch-001"
            )
        )

        then:
        thrown(TransactionIdempotencyConflictException)
        demandDepositAccountTransactionRepository.count() == 1
        accountTransactionIdempotencyRepository.count() == 1
    }
}
