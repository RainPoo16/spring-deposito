package com.examples.deposit.dto;

import java.math.BigDecimal;
import java.util.Set;

import com.examples.deposit.dto.request.CreditRequest;
import com.examples.deposit.dto.request.DebitRequest;
import com.examples.deposit.dto.request.OpenAccountRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationTests {

	private Validator validator;

	@BeforeEach
	void setUp() {
		this.validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void openAccountRequest_rejectsBlankOwnerName() {
		OpenAccountRequest request = new OpenAccountRequest(" ", "ACCT-DTO-001");

		Set<ConstraintViolation<OpenAccountRequest>> violations = this.validator.validate(request);

		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("Owner name is required");
	}

	@Test
	void openAccountRequest_rejectsBlankAccountNumber() {
		OpenAccountRequest request = new OpenAccountRequest("Alice", " ");

		Set<ConstraintViolation<OpenAccountRequest>> violations = this.validator.validate(request);

		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("Account number is required");
	}

	@Test
	void creditRequest_rejectsNullAmount() {
		CreditRequest request = new CreditRequest(null);

		Set<ConstraintViolation<CreditRequest>> violations = this.validator.validate(request);

		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("Amount is required");
	}

	@Test
	void creditRequest_rejectsNegativeAmount() {
		CreditRequest request = new CreditRequest(new BigDecimal("-1.00"));

		Set<ConstraintViolation<CreditRequest>> violations = this.validator.validate(request);

		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("Amount must be positive");
	}

	@Test
	void debitRequest_rejectsZeroAmount() {
		DebitRequest request = new DebitRequest(BigDecimal.ZERO);

		Set<ConstraintViolation<DebitRequest>> violations = this.validator.validate(request);

		assertThat(violations).hasSize(1);
		assertThat(violations.iterator().next().getMessage()).isEqualTo("Amount must be positive");
	}

}