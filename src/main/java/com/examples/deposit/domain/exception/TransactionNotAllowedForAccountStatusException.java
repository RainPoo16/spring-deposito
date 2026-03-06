package com.examples.deposit.domain.exception;

import com.examples.deposit.domain.DemandDepositAccountStatus;
import com.examples.deposit.domain.TransactionType;

import java.util.UUID;

public class TransactionNotAllowedForAccountStatusException extends RuntimeException {

    public TransactionNotAllowedForAccountStatusException(UUID accountId,
                                                          DemandDepositAccountStatus accountStatus,
                                                          TransactionType transactionType,
                                                          String transactionCode) {
        super("Transaction not allowed for accountId=" + accountId
            + ", accountStatus=" + accountStatus
            + ", transactionType=" + transactionType
            + ", transactionCode=" + transactionCode);
    }
}
