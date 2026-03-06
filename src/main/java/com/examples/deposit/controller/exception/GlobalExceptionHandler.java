package com.examples.deposit.controller.exception;

import com.examples.deposit.domain.exception.CustomerNotEligibleForAccountCreationException;
import com.examples.deposit.domain.exception.AccountNotFoundException;
import com.examples.deposit.domain.exception.BlockNotFoundException;
import com.examples.deposit.domain.exception.BlockNotEligibleForOperationException;
import com.examples.deposit.domain.exception.DuplicateOrOverlappingBlockException;
import com.examples.deposit.domain.exception.IdempotencyConflictException;
import com.examples.deposit.domain.exception.InsufficientAvailableBalanceException;
import com.examples.deposit.domain.exception.TransactionBlockedException;
import com.examples.deposit.domain.exception.TransactionIdempotencyConflictException;
import com.examples.deposit.domain.exception.TransactionNotAllowedForAccountStatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ApiProblemFactory apiProblemFactory;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException ignored) {
        return ResponseEntity.badRequest()
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(apiProblemFactory.validationFailed());
    }

    @ExceptionHandler(CustomerNotEligibleForAccountCreationException.class)
    public ResponseEntity<ProblemDetail> handleCustomerNotEligible(CustomerNotEligibleForAccountCreationException ignored) {
        return ResponseEntity.unprocessableEntity()
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(apiProblemFactory.customerNotEligible());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ProblemDetail> handleIdempotencyConflict(IdempotencyConflictException ignored) {
        return ResponseEntity.status(409)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(apiProblemFactory.idempotencyConflict());
    }

    @ExceptionHandler(DuplicateOrOverlappingBlockException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateOrOverlappingBlock(DuplicateOrOverlappingBlockException ignored) {
        return ResponseEntity.unprocessableEntity()
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(apiProblemFactory.duplicateOrOverlappingBlock());
    }

    @ExceptionHandler(BlockNotEligibleForOperationException.class)
    public ResponseEntity<ProblemDetail> handleBlockNotEligible(BlockNotEligibleForOperationException ignored) {
        return ResponseEntity.unprocessableEntity()
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(apiProblemFactory.blockNotEligibleForOperation());
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleAccountNotFound(AccountNotFoundException ignored) {
        return ResponseEntity.status(404)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(apiProblemFactory.accountNotFound());
    }

    @ExceptionHandler(BlockNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleBlockNotFound(BlockNotFoundException ignored) {
        return ResponseEntity.status(404)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(apiProblemFactory.blockNotFound());
    }

    @ExceptionHandler(TransactionBlockedException.class)
    public ResponseEntity<ProblemDetail> handleTransactionBlocked(TransactionBlockedException ignored) {
        return ResponseEntity.unprocessableEntity()
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(apiProblemFactory.transactionBlocked());
    }

    @ExceptionHandler(TransactionNotAllowedForAccountStatusException.class)
    public ResponseEntity<ProblemDetail> handleTransactionNotAllowedForAccountStatus(
        TransactionNotAllowedForAccountStatusException ignored
    ) {
        return ResponseEntity.unprocessableEntity()
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(apiProblemFactory.transactionNotAllowedForAccountStatus());
    }

    @ExceptionHandler(InsufficientAvailableBalanceException.class)
    public ResponseEntity<ProblemDetail> handleInsufficientAvailableBalance(
        InsufficientAvailableBalanceException ignored
    ) {
        return ResponseEntity.unprocessableEntity()
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(apiProblemFactory.insufficientAvailableBalance());
    }

    @ExceptionHandler(TransactionIdempotencyConflictException.class)
    public ResponseEntity<ProblemDetail> handleTransactionIdempotencyConflict(
        TransactionIdempotencyConflictException ignored
    ) {
        return ResponseEntity.status(409)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(apiProblemFactory.transactionIdempotencyConflict());
    }

    @ExceptionHandler({
        MissingRequestHeaderException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class
    })
    public ResponseEntity<ProblemDetail> handleMalformedRequest(Exception ignored) {
        return ResponseEntity.badRequest()
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(apiProblemFactory.malformedRequest());
    }
}
