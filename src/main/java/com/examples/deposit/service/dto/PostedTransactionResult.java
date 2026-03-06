package com.examples.deposit.service.dto;

import com.examples.deposit.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PostedTransactionResult(
    UUID transactionId,
    UUID accountId,
    UUID customerId,
    TransactionType transactionType,
    BigDecimal amount,
    String transactionCode,
    String referenceId,
    String idempotencyKey,
    Instant postedAt,
    BigDecimal currentBalance,
    BigDecimal availableBalance
) {
}
