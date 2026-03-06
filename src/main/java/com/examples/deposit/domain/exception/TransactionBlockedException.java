package com.examples.deposit.domain.exception;

import com.examples.deposit.domain.TransactionType;

import java.util.UUID;

public class TransactionBlockedException extends RuntimeException {

    public TransactionBlockedException(UUID accountId,
                                       TransactionType transactionType,
                                       String transactionCode,
                                       String blockCode) {
        super("Transaction blocked for accountId=" + accountId
            + ", transactionType=" + transactionType
            + ", transactionCode=" + transactionCode
            + ", blockCode=" + blockCode);
    }
}
