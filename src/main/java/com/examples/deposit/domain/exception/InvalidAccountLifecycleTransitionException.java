package com.examples.deposit.domain.exception;

import com.examples.deposit.domain.DemandDepositAccountStatus;

import java.util.UUID;

public class InvalidAccountLifecycleTransitionException extends RuntimeException {

    public InvalidAccountLifecycleTransitionException(UUID accountId,
                                                      DemandDepositAccountStatus currentStatus,
                                                      DemandDepositAccountStatus targetStatus) {
        super("Cannot transition account %s from %s to %s".formatted(accountId, currentStatus, targetStatus));
    }
}
