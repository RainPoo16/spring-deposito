package com.examples.deposit.domain.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientAvailableBalanceException extends RuntimeException {

    public InsufficientAvailableBalanceException(UUID accountId,
                                                 BigDecimal availableBalance,
                                                 BigDecimal requestedAmount) {
        super("Insufficient available balance for accountId=" + accountId);
    }
}
