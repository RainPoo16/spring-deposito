package com.examples.deposit.controller.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PostDebitTransactionResp(
    UUID transactionId,
    UUID accountId,
    UUID customerId,
    String transactionType,
    BigDecimal amount,
    String transactionCode,
    String referenceId,
    String idempotencyKey,
    Instant postedAt,
    BigDecimal currentBalance,
    BigDecimal availableBalance
) {
}
