package com.examples.deposit.controller.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CreateDemandDepositAccountBlockResp(
    UUID blockId,
    UUID accountId,
    String blockCode,
    String status,
    LocalDate effectiveDate,
    LocalDate expiryDate,
    String remark
) {
}
