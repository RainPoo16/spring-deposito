package com.examples.deposit.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AccountCreationEligibilityService {

    public boolean isEligibleForMainAccountCreation(UUID customerId) {
        return customerId != null;
    }
}
