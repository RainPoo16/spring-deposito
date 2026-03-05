package com.examples.deposit.dto

import java.math.BigDecimal

import com.examples.deposit.dto.request.CreditRequest
import com.examples.deposit.dto.request.DebitRequest
import com.examples.deposit.dto.request.OpenAccountRequest
import jakarta.validation.Validation
import jakarta.validation.Validator
import spock.lang.Specification

class ValidationSpec extends Specification {

	private Validator validator

	def setup() {
		validator = Validation.buildDefaultValidatorFactory().validator
	}

	def "openAccountRequest rejects blank ownerName"() {
		given:
		def request = new OpenAccountRequest(" ", "ACCT-DTO-001")

		when:
		def violations = validator.validate(request)

		then:
		violations.size() == 1
		violations.iterator().next().message == "Owner name is required"
	}

	def "openAccountRequest rejects blank accountNumber"() {
		given:
		def request = new OpenAccountRequest("Alice", " ")

		when:
		def violations = validator.validate(request)

		then:
		violations.size() == 1
		violations.iterator().next().message == "Account number is required"
	}

	def "creditRequest rejects null amount"() {
		given:
		def request = new CreditRequest(null)

		when:
		def violations = validator.validate(request)

		then:
		violations.size() == 1
		violations.iterator().next().message == "Amount is required"
	}

	def "creditRequest rejects negative amount"() {
		given:
		def request = new CreditRequest(new BigDecimal("-1.00"))

		when:
		def violations = validator.validate(request)

		then:
		violations.size() == 1
		violations.iterator().next().message == "Amount must be positive"
	}

	def "debitRequest rejects zero amount"() {
		given:
		def request = new DebitRequest(BigDecimal.ZERO)

		when:
		def violations = validator.validate(request)

		then:
		violations.size() == 1
		violations.iterator().next().message == "Amount must be positive"
	}

}