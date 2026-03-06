package com.examples.deposit.domain.exception;

import java.util.UUID;

public class BlockNotFoundException extends RuntimeException {

    public BlockNotFoundException(UUID blockId) {
        super("Block not found: " + blockId);
    }
}
