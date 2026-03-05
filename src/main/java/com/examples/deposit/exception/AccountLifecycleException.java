package com.examples.deposit.exception;

public class AccountLifecycleException extends RuntimeException {

    public AccountLifecycleException(String message, Throwable cause) {
        super(message, cause);
    }
}
