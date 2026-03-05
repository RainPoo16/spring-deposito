package com.examples.deposit.domain;

import com.github.f4b6a3.uuid.alt.GUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "demand_deposit_account_lifecycle_events")
public class DemandDepositAccountLifecycleEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64, updatable = false)
    private DemandDepositAccountLifecycleEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 64, updatable = false)
    private DemandDepositAccountStatus accountStatus;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    protected DemandDepositAccountLifecycleEvent() {
        // JPA constructor
    }

    private DemandDepositAccountLifecycleEvent(
        UUID accountId,
        UUID customerId,
        DemandDepositAccountLifecycleEventType eventType,
        DemandDepositAccountStatus accountStatus,
        LocalDateTime occurredAt
    ) {
        this.id = GUID.v7().toUUID();
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.accountStatus = Objects.requireNonNull(accountStatus, "accountStatus must not be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static DemandDepositAccountLifecycleEvent accountCreated(DemandDepositAccount account) {
        return new DemandDepositAccountLifecycleEvent(
            account.getId(),
            account.getCustomerId(),
            DemandDepositAccountLifecycleEventType.ACCOUNT_CREATED,
            account.getStatus(),
            LocalDateTime.now()
        );
    }

    public static DemandDepositAccountLifecycleEvent accountActivated(DemandDepositAccount account) {
        return new DemandDepositAccountLifecycleEvent(
            account.getId(),
            account.getCustomerId(),
            DemandDepositAccountLifecycleEventType.ACCOUNT_ACTIVATED,
            account.getStatus(),
            LocalDateTime.now()
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public DemandDepositAccountLifecycleEventType getEventType() {
        return eventType;
    }

    public DemandDepositAccountStatus getAccountStatus() {
        return accountStatus;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
