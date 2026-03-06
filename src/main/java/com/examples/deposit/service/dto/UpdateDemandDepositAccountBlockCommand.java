package com.examples.deposit.service.dto;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateDemandDepositAccountBlockCommand(
    UUID accountId,
    UUID customerId,
    UUID blockId,
    LocalDate effectiveDate,
    LocalDate expiryDate,
    String remark
) {
}
