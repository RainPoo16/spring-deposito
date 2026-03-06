package com.examples.deposit.domain

import com.examples.deposit.domain.exception.InsufficientAvailableBalanceException
import com.examples.deposit.domain.exception.TransactionNotAllowedForAccountStatusException
import spock.lang.Specification
import spock.lang.Unroll

import java.math.BigDecimal
import java.util.UUID

class DemandDepositAccountTransactionSpec extends Specification {

    def "status vocabulary includes transaction validation states"() {
        expect:
        DemandDepositAccountStatus.values().toList().toSet() == [
            DemandDepositAccountStatus.PENDING_VERIFICATION,
            DemandDepositAccountStatus.ACTIVE,
            DemandDepositAccountStatus.DORMANT,
            DemandDepositAccountStatus.CLOSE_INITIATED,
            DemandDepositAccountStatus.CLOSED,
            DemandDepositAccountStatus.UNVERIFIED
        ] as Set
    }

    def "credit increases current and available balances"() {
        given:
        def account = DemandDepositAccount.createWithId(UUID.randomUUID(), UUID.randomUUID(), DemandDepositAccountStatus.ACTIVE)

        when:
        account.applyCredit(new BigDecimal("25.500"), "FAST_TRANSFER")

        then:
        account.currentBalance.compareTo(new BigDecimal("25.5")) == 0
        account.availableBalance.compareTo(new BigDecimal("25.5")) == 0
    }

    def "debit decreases current and available balances"() {
        given:
        def account = DemandDepositAccount.createWithId(UUID.randomUUID(), UUID.randomUUID(), DemandDepositAccountStatus.ACTIVE)
        account.applyCredit(new BigDecimal("100.00"), "FAST_TRANSFER")

        when:
        account.applyDebit(new BigDecimal("40.000"), "FAST_TRANSFER")

        then:
        account.currentBalance.compareTo(new BigDecimal("60.0")) == 0
        account.availableBalance.compareTo(new BigDecimal("60")) == 0
    }

    def "debit throws when available balance is insufficient"() {
        given:
        def account = DemandDepositAccount.createWithId(UUID.randomUUID(), UUID.randomUUID(), DemandDepositAccountStatus.ACTIVE)
        account.applyCredit(new BigDecimal("10.00"), "FAST_TRANSFER")

        when:
        account.applyDebit(new BigDecimal("10.001"), "FAST_TRANSFER")

        then:
        def exception = thrown(InsufficientAvailableBalanceException)
        exception.message.contains("Insufficient available balance for accountId=")
        !exception.message.contains("availableBalance=")
        !exception.message.contains("requestedAmount=")
    }

    @Unroll
    def "credit is allowed for status #status with code #transactionCode"() {
        given:
        def account = DemandDepositAccount.createWithId(UUID.randomUUID(), UUID.randomUUID(), status)

        when:
        account.applyCredit(new BigDecimal("10.00"), transactionCode)

        then:
        noExceptionThrown()
        account.currentBalance.compareTo(new BigDecimal("10.00")) == 0

        where:
        status                                           | transactionCode
        DemandDepositAccountStatus.ACTIVE               | "FAST_TRANSFER"
        DemandDepositAccountStatus.DORMANT              | "FAST_TRANSFER"
        DemandDepositAccountStatus.PENDING_VERIFICATION | "CASH_DEPOSIT"
    }

    @Unroll
    def "credit is rejected for status #status with code #transactionCode"() {
        given:
        def account = DemandDepositAccount.createWithId(UUID.randomUUID(), UUID.randomUUID(), status)

        when:
        account.applyCredit(new BigDecimal("10.00"), transactionCode)

        then:
        thrown(TransactionNotAllowedForAccountStatusException)

        where:
        status                                           | transactionCode
        DemandDepositAccountStatus.PENDING_VERIFICATION | "FAST_TRANSFER"
        DemandDepositAccountStatus.CLOSE_INITIATED      | "FAST_TRANSFER"
        DemandDepositAccountStatus.UNVERIFIED           | "FAST_TRANSFER"
        DemandDepositAccountStatus.CLOSED               | "FAST_TRANSFER"
    }

    @Unroll
    def "debit is allowed for status #status with code #transactionCode"() {
        given:
        def account = DemandDepositAccount.createWithId(UUID.randomUUID(), UUID.randomUUID(), status)
        account.applyCredit(new BigDecimal("30.00"), "FAST_TRANSFER")

        when:
        account.applyDebit(new BigDecimal("10.00"), transactionCode)

        then:
        noExceptionThrown()
        account.currentBalance.compareTo(new BigDecimal("20.00")) == 0

        where:
        status                              | transactionCode
        DemandDepositAccountStatus.ACTIVE  | "FAST_TRANSFER"
        DemandDepositAccountStatus.DORMANT | "FUND_HOLD_DEBIT"
    }

    @Unroll
    def "debit is rejected for status #status with code #transactionCode"() {
        given:
        def account = DemandDepositAccount.createWithId(UUID.randomUUID(), UUID.randomUUID(), status)
        if (status == DemandDepositAccountStatus.ACTIVE || status == DemandDepositAccountStatus.DORMANT) {
            account.applyCredit(new BigDecimal("30.00"), "FAST_TRANSFER")
        }

        when:
        account.applyDebit(new BigDecimal("10.00"), transactionCode)

        then:
        thrown(TransactionNotAllowedForAccountStatusException)

        where:
        status                                           | transactionCode
        DemandDepositAccountStatus.DORMANT              | "FAST_TRANSFER"
        DemandDepositAccountStatus.PENDING_VERIFICATION | "FAST_TRANSFER"
        DemandDepositAccountStatus.CLOSE_INITIATED      | "FAST_TRANSFER"
        DemandDepositAccountStatus.UNVERIFIED           | "FAST_TRANSFER"
        DemandDepositAccountStatus.CLOSED               | "FAST_TRANSFER"
    }
}
