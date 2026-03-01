package com.examples.deposit.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OpenAccountRequest(@NotBlank(message = "Owner name is required") String ownerName,
		@NotBlank(message = "Account number is required") String accountNumber) {
}