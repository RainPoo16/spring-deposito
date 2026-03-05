package com.examples.deposit.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDemandDepositAccountReq(
    @NotBlank(message = "idempotencyKey must not be blank")
    @Size(max = 128, message = "idempotencyKey must be <= 128 characters")
    String idempotencyKey
) {
}
