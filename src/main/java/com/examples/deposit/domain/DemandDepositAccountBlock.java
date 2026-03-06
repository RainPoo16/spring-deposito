package com.examples.deposit.domain;

import com.examples.deposit.domain.exception.BlockNotEligibleForOperationException;
import com.github.f4b6a3.uuid.alt.GUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "demand_deposit_account_block")
public class DemandDepositAccountBlock {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_code", nullable = false)
    private BlockCode blockCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_by", nullable = false)
    private BlockRequestedBy requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountBlockStatus status;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "remark", length = 255)
    private String remark;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected DemandDepositAccountBlock() {
    }

    public static DemandDepositAccountBlock create(
        UUID accountId,
        BlockCode blockCode,
        BlockRequestedBy requestedBy,
        AccountBlockStatus status,
        LocalDate effectiveDate,
        LocalDate expiryDate,
        String remark
    ) {
        DemandDepositAccountBlock block = new DemandDepositAccountBlock();
        block.id = GUID.v7().toUUID();
        block.accountId = accountId;
        block.blockCode = blockCode;
        block.requestedBy = requestedBy;
        block.status = status;
        block.effectiveDate = effectiveDate;
        block.expiryDate = expiryDate;
        block.remark = remark;
        return block;
    }

    public void updateDetails(BlockRequestedBy actor, LocalDate effectiveDate, LocalDate expiryDate, String remark) {
        if (effectiveDate.isAfter(expiryDate)) {
            throw new BlockNotEligibleForOperationException(id, "update");
        }
        if (!status.isEligibleForUpdate(actor, blockCode)) {
            throw new BlockNotEligibleForOperationException(id, "update");
        }
        this.effectiveDate = effectiveDate;
        this.expiryDate = expiryDate;
        this.remark = remark;
    }

    public void cancel(BlockRequestedBy actor) {
        if (!status.isEligibleForCancel(actor, blockCode)) {
            throw new BlockNotEligibleForOperationException(id, "cancel");
        }
        this.status = AccountBlockStatus.CANCELLED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public BlockCode getBlockCode() {
        return blockCode;
    }

    public BlockRequestedBy getRequestedBy() {
        return requestedBy;
    }

    public AccountBlockStatus getStatus() {
        return status;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public String getRemark() {
        return remark;
    }

    public Long getVersion() {
        return version;
    }
}
