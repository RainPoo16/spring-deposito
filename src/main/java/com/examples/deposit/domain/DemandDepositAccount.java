package com.examples.deposit.domain;

import com.github.f4b6a3.uuid.alt.GUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "demand_deposit_accounts",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_demand_deposit_accounts_customer_idempotency",
            columnNames = {"customer_id", "idempotency_key"}
        )
    }
)
public class DemandDepositAccount {

    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 128;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "idempotency_key", nullable = false, length = 128, updatable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 64)
    private DemandDepositAccountStatus status;

    @Column(name = "ledger_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal ledgerBalance;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableBalance;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected DemandDepositAccount() {
        // JPA constructor
    }

    private DemandDepositAccount(UUID customerId, String idempotencyKey, LocalDateTime now) {
        this.id = GUID.v7().toUUID();
        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        this.idempotencyKey = validateIdempotencyKey(idempotencyKey);
        this.status = DemandDepositAccountStatus.PENDING_VERIFICATION;
        this.ledgerBalance = BigDecimal.ZERO;
        this.availableBalance = BigDecimal.ZERO;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static DemandDepositAccount createPending(UUID customerId, String idempotencyKey) {
        return new DemandDepositAccount(customerId, idempotencyKey, LocalDateTime.now());
    }

    public void activate() {
        if (status != DemandDepositAccountStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("Demand deposit account can only be activated from PENDING_VERIFICATION status");
        }
        status = DemandDepositAccountStatus.ACTIVE;
        updatedAt = LocalDateTime.now();
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

    public DemandDepositAccountStatus getStatus() {
        return status;
    }

    public BigDecimal getLedgerBalance() {
        return ledgerBalance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DemandDepositAccount that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    private static String validateIdempotencyKey(String idempotencyKey) {
        String value = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        if (value.length() > IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw new IllegalArgumentException("idempotencyKey must be <= 128 characters");
        }
        return value;
    }
}
