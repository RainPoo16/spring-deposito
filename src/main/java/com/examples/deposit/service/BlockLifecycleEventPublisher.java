package com.examples.deposit.service;

import java.util.UUID;

public interface BlockLifecycleEventPublisher {

    void publishBlockCreated(UUID accountId, UUID blockId);

    void publishBlockUpdated(UUID accountId, UUID blockId);

    void publishBlockCancelled(UUID accountId, UUID blockId);
}
