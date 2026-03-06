package com.examples.deposit.domain.exception;

import java.util.UUID;

public class IdempotencyConflictException extends Exception {

    public IdempotencyConflictException(UUID customerId, String idempotencyKey) {
        super("Unable to resolve idempotency conflict for customerId=" + customerId
            + ", idempotencyKey=" + idempotencyKey);
    }
}
