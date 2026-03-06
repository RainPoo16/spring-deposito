package com.examples.deposit.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDemandDepositAccountReq(
    @NotBlank(message = "idempotencyKey is required")
    @Size(max = 128, message = "idempotencyKey must be at most 128 characters")
    String idempotencyKey
) {
}
