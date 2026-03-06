package com.examples.deposit.service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PostDebitTransactionCommand(
    UUID accountId,
    UUID customerId,
    BigDecimal amount,
    String transactionCode,
    String referenceId,
    String idempotencyKey
) {
}
