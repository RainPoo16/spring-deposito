package com.examples.deposit.domain.exception;

import java.util.UUID;

public class CustomerNotEligibleForAccountCreationException extends Exception {

    public CustomerNotEligibleForAccountCreationException(UUID customerId) {
        super("Customer is not eligible for account creation: " + customerId);
    }
}
