package com.examples.deposit.domain;

import com.github.f4b6a3.uuid.alt.GUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "demand_deposit_account_transaction")
public class DemandDepositAccountTransaction {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, updatable = false)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_code", nullable = false, updatable = false, length = 64)
    private String transactionCode;

    @Column(name = "reference_id", nullable = false, updatable = false, length = 128)
    private String referenceId;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "posted_at", nullable = false, updatable = false)
    private Instant postedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected DemandDepositAccountTransaction() {
    }

    public static DemandDepositAccountTransaction create(
        UUID accountId,
        UUID customerId,
        TransactionType transactionType,
        BigDecimal amount,
        String transactionCode,
        String referenceId,
        String idempotencyKey,
        Instant postedAt
    ) {
        DemandDepositAccountTransaction transaction = new DemandDepositAccountTransaction();
        transaction.id = GUID.v7().toUUID();
        transaction.accountId = accountId;
        transaction.customerId = customerId;
        transaction.transactionType = transactionType;
        transaction.amount = amount;
        transaction.transactionCode = transactionCode;
        transaction.referenceId = referenceId;
        transaction.idempotencyKey = idempotencyKey;
        transaction.postedAt = postedAt;
        return transaction;
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

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getTransactionCode() {
        return transactionCode;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public Long getVersion() {
        return version;
    }
}
