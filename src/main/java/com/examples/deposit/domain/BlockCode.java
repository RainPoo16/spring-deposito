package com.examples.deposit.domain;

import java.util.Arrays;

public enum BlockCode {
    ACB(BlockDirection.INCOMING, BlockRequestedBy.BANK, false),
    ACC(BlockDirection.INCOMING, BlockRequestedBy.CUSTOMER, true),
    ACG(BlockDirection.INCOMING, BlockRequestedBy.GOVERNMENT, false),
    ADB(BlockDirection.OUTGOING, BlockRequestedBy.BANK, false);

    private final BlockDirection direction;
    private final BlockRequestedBy requestedBy;
    private final boolean customerAllowed;

    BlockCode(BlockDirection direction, BlockRequestedBy requestedBy, boolean customerAllowed) {
        this.direction = direction;
        this.requestedBy = requestedBy;
        this.customerAllowed = customerAllowed;
    }

    public static BlockCode fromCode(String rawCode) {
        return Arrays.stream(values())
            .filter(value -> value.name().equals(rawCode))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported block code: " + rawCode));
    }

    public boolean isEligibleForCreateBy(BlockRequestedBy actor) {
        if (this.requestedBy != actor) {
            return false;
        }
        return actor != BlockRequestedBy.CUSTOMER || customerAllowed;
    }

    public BlockDirection getDirection() {
        return direction;
    }

    public BlockRequestedBy getRequestedBy() {
        return requestedBy;
    }

    public boolean isCustomerAllowed() {
        return customerAllowed;
    }
}