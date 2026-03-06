package com.examples.deposit.service;

import java.util.UUID;

public interface AccountLifecycleEventPublisher {

    void publishAccountCreated(UUID accountId, UUID customerId);

    void publishAccountActivated(UUID accountId, UUID customerId);
}
