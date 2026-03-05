package com.examples.deposit.service.dto;

import java.util.Objects;
import java.util.UUID;

public record CreateDemandDepositAccountCommand(UUID customerId, String idempotencyKey) {

    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 128;

    public CreateDemandDepositAccountCommand {
        customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null").trim();
        if (idempotencyKey.isEmpty()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        if (idempotencyKey.length() > IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw new IllegalArgumentException("idempotencyKey must be <= 128 characters");
        }
    }
}
