package com.examples.deposit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpringBlockLifecycleEventPublisher implements BlockLifecycleEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publishBlockCreated(UUID accountId, UUID blockId) {
        applicationEventPublisher.publishEvent(new BlockCreatedLifecycleEvent(accountId, blockId));
    }

    @Override
    public void publishBlockUpdated(UUID accountId, UUID blockId) {
        applicationEventPublisher.publishEvent(new BlockUpdatedLifecycleEvent(accountId, blockId));
    }

    @Override
    public void publishBlockCancelled(UUID accountId, UUID blockId) {
        applicationEventPublisher.publishEvent(new BlockCancelledLifecycleEvent(accountId, blockId));
    }

    public record BlockCreatedLifecycleEvent(UUID accountId, UUID blockId) {
    }

    public record BlockUpdatedLifecycleEvent(UUID accountId, UUID blockId) {
    }

    public record BlockCancelledLifecycleEvent(UUID accountId, UUID blockId) {
    }
}
