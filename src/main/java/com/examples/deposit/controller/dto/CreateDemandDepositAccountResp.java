package com.examples.deposit.controller.dto;

import java.util.UUID;

public record CreateDemandDepositAccountResp(
    UUID accountId,
    UUID customerId,
    String status
) {
}
