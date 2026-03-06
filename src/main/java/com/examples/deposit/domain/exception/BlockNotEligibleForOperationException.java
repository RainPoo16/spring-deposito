package com.examples.deposit.domain.exception;

import java.util.UUID;

public class BlockNotEligibleForOperationException extends RuntimeException {

    public BlockNotEligibleForOperationException(UUID blockId, String operation) {
        super("Block %s is not eligible for operation: %s".formatted(blockId, operation));
    }
}