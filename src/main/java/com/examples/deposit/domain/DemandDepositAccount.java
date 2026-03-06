package com.examples.deposit.domain;

import com.examples.deposit.domain.exception.InvalidAccountLifecycleTransitionException;
import com.github.f4b6a3.uuid.alt.GUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

@Entity
@Table(name = "demand_deposit_account")
public class DemandDepositAccount {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DemandDepositAccountStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected DemandDepositAccount() {
    }

    public static DemandDepositAccount create(UUID customerId, DemandDepositAccountStatus status) {
        return createWithId(GUID.v7().toUUID(), customerId, status);
    }

    public static DemandDepositAccount createWithId(UUID id, UUID customerId, DemandDepositAccountStatus status) {
        DemandDepositAccount account = new DemandDepositAccount();
        account.id = id;
        account.customerId = customerId;
        account.status = status;
        return account;
    }

    public boolean activate() {
        if (this.status == DemandDepositAccountStatus.ACTIVE) {
            return false;
        }
        if (this.status != DemandDepositAccountStatus.PENDING_VERIFICATION) {
            throw new InvalidAccountLifecycleTransitionException(id, this.status, DemandDepositAccountStatus.ACTIVE);
        }
        this.status = DemandDepositAccountStatus.ACTIVE;
        return true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public DemandDepositAccountStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }
}
