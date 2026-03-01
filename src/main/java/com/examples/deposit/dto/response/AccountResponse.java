package com.examples.deposit.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountResponse(UUID id, String accountNumber, String ownerName, BigDecimal balance, String status,
		LocalDateTime createdAt, LocalDateTime updatedAt) {
}