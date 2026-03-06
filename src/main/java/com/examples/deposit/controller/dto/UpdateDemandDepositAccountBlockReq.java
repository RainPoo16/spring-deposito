package com.examples.deposit.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateDemandDepositAccountBlockReq(
    @NotNull(message = "effectiveDate is required")
    LocalDate effectiveDate,
    @NotNull(message = "expiryDate is required")
    LocalDate expiryDate,
    @Size(max = 255, message = "remark must be at most 255 characters")
    String remark
) {
}
