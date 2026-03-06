package com.examples.deposit.domain;

public enum AccountBlockStatus {
    PENDING,
    ACTIVE,
    CANCELLED;

    public boolean isEligibleForUpdate(BlockRequestedBy actor, BlockCode blockCode) {
        if (this == CANCELLED) {
            return false;
        }
        return blockCode.isEligibleForCreateBy(actor);
    }

    public boolean isEligibleForCancel(BlockRequestedBy actor, BlockCode blockCode) {
        if (this == CANCELLED) {
            return false;
        }
        return blockCode.isEligibleForCreateBy(actor);
    }
}