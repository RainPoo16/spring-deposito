package com.examples.deposit.service.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CreateDemandDepositAccountBlockCommand(
    UUID accountId,
    UUID customerId,
    String blockCode,
    LocalDate effectiveDate,
    LocalDate expiryDate,
    String remark
) {
}
