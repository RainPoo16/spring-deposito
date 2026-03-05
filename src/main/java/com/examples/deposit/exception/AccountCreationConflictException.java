package com.examples.deposit.exception;

public class AccountCreationConflictException extends RuntimeException {

    public AccountCreationConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
