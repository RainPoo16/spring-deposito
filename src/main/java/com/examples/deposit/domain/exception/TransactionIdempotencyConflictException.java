package com.examples.deposit.domain.exception;

import java.util.UUID;

public class TransactionIdempotencyConflictException extends RuntimeException {

    public TransactionIdempotencyConflictException(UUID customerId, String idempotencyKey, String referenceId) {
        super("Unable to resolve transaction idempotency conflict for customerId=" + customerId
            + ", idempotencyKey=" + idempotencyKey
            + ", referenceId=" + referenceId);
    }
}
