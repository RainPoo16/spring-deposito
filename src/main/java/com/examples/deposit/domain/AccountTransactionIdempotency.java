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
    name = "account_transaction_idempotency",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_account_transaction_idempotency_identity",
        columnNames = {"customer_id", "idempotency_key", "reference_id"}
    )
)
public class AccountTransactionIdempotency {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "reference_id", nullable = false, updatable = false, length = 128)
    private String referenceId;

    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected AccountTransactionIdempotency() {
    }

    public static AccountTransactionIdempotency create(
        UUID customerId,
        UUID accountId,
        String idempotencyKey,
        String referenceId,
        UUID transactionId
    ) {
        AccountTransactionIdempotency record = new AccountTransactionIdempotency();
        record.id = GUID.v7().toUUID();
        record.customerId = customerId;
        record.accountId = accountId;
        record.idempotencyKey = idempotencyKey;
        record.referenceId = referenceId;
        record.transactionId = transactionId;
        return record;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public Long getVersion() {
        return version;
    }
}
