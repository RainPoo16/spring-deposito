package com.examples.deposit.controller.exception;

import com.examples.deposit.exception.AccountCreationConflictException;
import com.examples.deposit.exception.AccountLifecycleException;
import com.examples.deposit.exception.AccountNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String CONFLICT_DETAIL = "Account creation request conflicts with current state.";
    private static final String LIFECYCLE_VIOLATION_DETAIL = "Request violates account lifecycle rules.";
    private static final String ACCOUNT_NOT_FOUND_DETAIL = "Requested account was not found.";
    private static final String INVALID_REQUEST_DETAIL = "Request validation failed.";

    @ExceptionHandler(AccountCreationConflictException.class)
    public ResponseEntity<ProblemDetail> handleAccountCreationConflict(AccountCreationConflictException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "deposit/account-creation-conflict", CONFLICT_DETAIL, request.getRequestURI());
    }

    @ExceptionHandler(AccountLifecycleException.class)
    public ResponseEntity<ProblemDetail> handleAccountLifecycle(AccountLifecycleException ex, HttpServletRequest request) {
        return problem(HttpStatus.valueOf(422), "deposit/account-lifecycle-violation", LIFECYCLE_VIOLATION_DETAIL, request.getRequestURI());
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleAccountNotFound(AccountNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "deposit/account-not-found", ACCOUNT_NOT_FOUND_DETAIL, request.getRequestURI());
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        MethodArgumentTypeMismatchException.class,
        MissingRequestHeaderException.class,
        HttpMessageNotReadableException.class,
        IllegalArgumentException.class
    })
    public ResponseEntity<ProblemDetail> handleBadRequest(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "deposit/invalid-request", INVALID_REQUEST_DETAIL, request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "deposit/internal-server-error", null, request.getRequestURI());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String type, String detail, String instance) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, defaultDetail(detail, status));
        problemDetail.setType(URI.create(type));
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setInstance(URI.create(instance));
        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problemDetail);
    }

    private static String defaultDetail(String detail, HttpStatus status) {
        if (detail == null || detail.isBlank()) {
            return status.getReasonPhrase();
        }
        return detail;
    }
}
