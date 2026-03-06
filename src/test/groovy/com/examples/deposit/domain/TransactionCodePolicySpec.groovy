package com.examples.deposit.domain

import spock.lang.Specification
import spock.lang.Unroll

class TransactionCodePolicySpec extends Specification {

    @Unroll
    def "validation mode for #transactionCode is #expected"() {
        expect:
        TransactionCodePolicy.resolveValidationMode(transactionCode) == expected

        where:
        transactionCode     || expected
        "FAST_TRANSFER"    || TransactionValidationMode.REQUIRED
        "ATM_WITHDRAWAL"   || TransactionValidationMode.REQUIRED
        "FUND_HOLD_DEBIT"  || TransactionValidationMode.BYPASS
        "FUND_RELEASE_CR"  || TransactionValidationMode.BYPASS
        "UNKNOWN_TX_CODE"  || TransactionValidationMode.REQUIRED
    }

    @Unroll
    def "pending verification allowlist for #transactionType/#transactionCode returns #expected"() {
        expect:
        TransactionCodePolicy.isAllowedForPendingVerification(transactionType, transactionCode) == expected

        where:
        transactionType        | transactionCode   || expected
        TransactionType.CREDIT | "CASH_DEPOSIT"   || true
        TransactionType.CREDIT | "SALARY_CREDIT"  || true
        TransactionType.CREDIT | "FAST_TRANSFER"  || false
        TransactionType.DEBIT  | "CASH_DEPOSIT"   || false
        TransactionType.DEBIT  | "ATM_WITHDRAWAL" || false
    }

    @Unroll
    def "validation mode normalization for '#transactionCode' resolves to #expected"() {
        expect:
        TransactionCodePolicy.resolveValidationMode(transactionCode) == expected

        where:
        transactionCode       || expected
        null                  || TransactionValidationMode.REQUIRED
        ""                    || TransactionValidationMode.REQUIRED
        "   "                 || TransactionValidationMode.REQUIRED
        " fund_hold_debit "   || TransactionValidationMode.BYPASS
        "fund_release_cr"     || TransactionValidationMode.BYPASS
    }

    @Unroll
    def "pending verification normalization for '#transactionCode' returns #expected"() {
        expect:
        TransactionCodePolicy.isAllowedForPendingVerification(TransactionType.CREDIT, transactionCode) == expected

        where:
        transactionCode       || expected
        null                  || false
        ""                    || false
        "   "                 || false
        " cash_deposit "      || true
        "salary_credit"       || true
        " fast_transfer "     || false
    }
}
