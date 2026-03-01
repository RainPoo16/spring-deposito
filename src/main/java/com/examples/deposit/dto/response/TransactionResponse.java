package com.examples.deposit.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(UUID id, String type, BigDecimal amount, BigDecimal balanceAfter,
		LocalDateTime createdAt) {
}