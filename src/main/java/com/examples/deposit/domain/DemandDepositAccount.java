package com.examples.deposit.domain;

import com.examples.deposit.domain.exception.InvalidAccountLifecycleTransitionException;
import com.examples.deposit.domain.exception.InsufficientAvailableBalanceException;
import com.examples.deposit.domain.exception.TransactionNotAllowedForAccountStatusException;
import com.github.f4b6a3.uuid.alt.GUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "demand_deposit_account")
public class DemandDepositAccount {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DemandDepositAccountStatus status;

    @Column(name = "current_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal availableBalance;

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
        account.currentBalance = ZERO;
        account.availableBalance = ZERO;
        return account;
    }

    public void applyCredit(BigDecimal amount, String transactionCode) {
        requirePositiveAmount(amount);
        requireEligibleStatus(TransactionType.CREDIT, transactionCode);
        this.currentBalance = getCurrentBalance().add(amount);
        this.availableBalance = getAvailableBalance().add(amount);
    }

    public void applyDebit(BigDecimal amount, String transactionCode) {
        requirePositiveAmount(amount);
        requireEligibleStatus(TransactionType.DEBIT, transactionCode);
        if (getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientAvailableBalanceException(id, getAvailableBalance(), amount);
        }
        this.currentBalance = getCurrentBalance().subtract(amount);
        this.availableBalance = getAvailableBalance().subtract(amount);
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

    public BigDecimal getCurrentBalance() {
        if (currentBalance == null) {
            return ZERO;
        }
        return currentBalance;
    }

    public BigDecimal getAvailableBalance() {
        if (availableBalance == null) {
            return ZERO;
        }
        return availableBalance;
    }

    public Long getVersion() {
        return version;
    }

    private void requirePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be greater than zero");
        }
    }

    private void requireEligibleStatus(TransactionType transactionType, String transactionCode) {
        if (status == DemandDepositAccountStatus.ACTIVE) {
            return;
        }

        if (status == DemandDepositAccountStatus.CLOSED
            || status == DemandDepositAccountStatus.CLOSE_INITIATED
            || status == DemandDepositAccountStatus.UNVERIFIED) {
            throw new TransactionNotAllowedForAccountStatusException(id, status, transactionType, transactionCode);
        }

        if (status == DemandDepositAccountStatus.PENDING_VERIFICATION) {
            boolean pendingVerificationAllowed = transactionType == TransactionType.CREDIT
                && TransactionCodePolicy.isAllowedForPendingVerification(transactionType, transactionCode);
            if (!pendingVerificationAllowed) {
                throw new TransactionNotAllowedForAccountStatusException(id, status, transactionType, transactionCode);
            }
            return;
        }

        if (status == DemandDepositAccountStatus.DORMANT) {
            boolean dormantDebitAllowed = transactionType == TransactionType.DEBIT
                && TransactionCodePolicy.resolveValidationMode(transactionCode) == TransactionValidationMode.BYPASS;
            if (transactionType == TransactionType.CREDIT || dormantDebitAllowed) {
                return;
            }
            throw new TransactionNotAllowedForAccountStatusException(id, status, transactionType, transactionCode);
        }

        throw new TransactionNotAllowedForAccountStatusException(id, status, transactionType, transactionCode);
    }
}
