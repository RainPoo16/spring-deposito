package com.examples.deposit.domain.exception;

import java.util.UUID;

public class DuplicateOrOverlappingBlockException extends RuntimeException {

    public DuplicateOrOverlappingBlockException(UUID accountId, String blockCode) {
        super("Duplicate or overlapping block found for account %s and block code %s"
            .formatted(accountId, blockCode));
    }
}