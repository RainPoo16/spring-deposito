package com.examples.deposit.controller.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record PostCreditTransactionReq(
    @NotNull(message = "accountId is required")
    UUID accountId,
    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    BigDecimal amount,
    @NotBlank(message = "transactionCode is required")
    @Size(max = 64, message = "transactionCode must be at most 64 characters")
    @Pattern(regexp = "^[A-Z]+(?:_[A-Z]+)*$", message = "transactionCode must be uppercase letters with optional underscores")
    String transactionCode,
    @NotBlank(message = "referenceId is required")
    @Size(max = 128, message = "referenceId must be at most 128 characters")
    String referenceId,
    @NotBlank(message = "idempotencyKey is required")
    @Size(max = 128, message = "idempotencyKey must be at most 128 characters")
    String idempotencyKey
) {
}
