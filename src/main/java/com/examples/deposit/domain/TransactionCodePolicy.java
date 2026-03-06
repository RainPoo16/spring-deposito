package com.examples.deposit.domain;

import java.util.Locale;
import java.util.Set;

public final class TransactionCodePolicy {

    private static final Set<String> BYPASS_VALIDATION_CODES = Set.of(
        "FUND_HOLD_DEBIT",
        "FUND_RELEASE_CR"
    );

    private static final Set<String> PENDING_VERIFICATION_CREDIT_ALLOWED_CODES = Set.of(
        "CASH_DEPOSIT",
        "SALARY_CREDIT"
    );

    private TransactionCodePolicy() {
    }

    public static TransactionValidationMode resolveValidationMode(String transactionCode) {
        String normalizedCode = normalize(transactionCode);
        if (BYPASS_VALIDATION_CODES.contains(normalizedCode)) {
            return TransactionValidationMode.BYPASS;
        }
        return TransactionValidationMode.REQUIRED;
    }

    public static boolean isAllowedForPendingVerification(TransactionType transactionType, String transactionCode) {
        if (transactionType != TransactionType.CREDIT) {
            return false;
        }
        String normalizedCode = normalize(transactionCode);
        return PENDING_VERIFICATION_CREDIT_ALLOWED_CODES.contains(normalizedCode);
    }

    private static String normalize(String transactionCode) {
        if (transactionCode == null || transactionCode.isBlank()) {
            return "";
        }
        return transactionCode.trim().toUpperCase(Locale.ROOT);
    }
}
