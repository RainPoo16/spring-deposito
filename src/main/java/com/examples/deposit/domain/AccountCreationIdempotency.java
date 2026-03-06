package com.examples.deposit.domain;

import com.github.f4b6a3.uuid.alt.GUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.util.UUID;

@Entity
@Table(
    name = "account_creation_idempotency",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_account_creation_idempotency_customer_key",
        columnNames = {"customer_id", "idempotency_key"}
    )
)
public class AccountCreationIdempotency {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String idempotencyKey;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected AccountCreationIdempotency() {
    }

    public static AccountCreationIdempotency create(UUID customerId, String idempotencyKey, UUID accountId) {
        AccountCreationIdempotency record = new AccountCreationIdempotency();
        record.id = GUID.v7().toUUID();
        record.customerId = customerId;
        record.idempotencyKey = idempotencyKey;
        record.accountId = accountId;
        return record;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public Long getVersion() {
        return version;
    }
}
