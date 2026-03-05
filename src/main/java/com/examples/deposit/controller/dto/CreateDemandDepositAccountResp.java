package com.examples.deposit.controller.dto;

import com.examples.deposit.domain.DemandDepositAccountStatus;

import java.util.UUID;

public record CreateDemandDepositAccountResp(
    UUID accountId,
    UUID customerId,
    DemandDepositAccountStatus status
) {
}
