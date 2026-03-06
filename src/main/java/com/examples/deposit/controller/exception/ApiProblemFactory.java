package com.examples.deposit.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class ApiProblemFactory {

    public org.springframework.http.ProblemDetail validationFailed() {
        return build(
            HttpStatus.BAD_REQUEST,
            "deposit/validation-failed",
            "Validation failed",
            "Request validation failed"
        );
    }

    public org.springframework.http.ProblemDetail customerNotEligible() {
        return build(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "deposit/customer-not-eligible",
            "Customer not eligible",
            "Customer is not eligible for account creation"
        );
    }

    public org.springframework.http.ProblemDetail idempotencyConflict() {
        return build(
            HttpStatus.CONFLICT,
            "deposit/idempotency-conflict",
            "Idempotency conflict",
            "Unable to resolve idempotent account creation request"
        );
    }

    public org.springframework.http.ProblemDetail malformedRequest() {
        return build(
            HttpStatus.BAD_REQUEST,
            "deposit/malformed-request",
            "Malformed request",
            "Request headers or payload are invalid"
        );
    }

    private org.springframework.http.ProblemDetail build(HttpStatus status, String type, String title, String detail) {
        org.springframework.http.ProblemDetail problemDetail = org.springframework.http.ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setType(URI.create(type));
        problemDetail.setTitle(title);
        return problemDetail;
    }
}
