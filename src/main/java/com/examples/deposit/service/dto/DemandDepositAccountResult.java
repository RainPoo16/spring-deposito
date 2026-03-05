package com.examples.deposit.service.dto;

import com.examples.deposit.domain.DemandDepositAccountStatus;

import java.util.UUID;

public record DemandDepositAccountResult(
    UUID accountId,
    UUID customerId,
    DemandDepositAccountStatus status,
    boolean replay
) {
}
