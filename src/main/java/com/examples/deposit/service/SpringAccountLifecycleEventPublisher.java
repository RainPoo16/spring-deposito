package com.examples.deposit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpringAccountLifecycleEventPublisher implements AccountLifecycleEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publishAccountCreated(UUID accountId, UUID customerId) {
        applicationEventPublisher.publishEvent(new AccountCreatedLifecycleEvent(accountId, customerId));
    }

    @Override
    public void publishAccountActivated(UUID accountId, UUID customerId) {
        applicationEventPublisher.publishEvent(new AccountActivatedLifecycleEvent(accountId, customerId));
    }

    public record AccountCreatedLifecycleEvent(UUID accountId, UUID customerId) {
    }

    public record AccountActivatedLifecycleEvent(UUID accountId, UUID customerId) {
    }
}
