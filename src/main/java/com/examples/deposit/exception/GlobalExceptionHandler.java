package com.examples.deposit.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AccountNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleNotFound(AccountNotFoundException ex) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problemDetail.setTitle("Account Not Found");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
	}

	@ExceptionHandler(InvalidAccountStateException.class)
	public ResponseEntity<ProblemDetail> handleInvalidState(InvalidAccountStateException ex) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problemDetail.setTitle("Invalid Account State");
		return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
	}

	@ExceptionHandler(InsufficientBalanceException.class)
	public ResponseEntity<ProblemDetail> handleInsufficientBalance(InsufficientBalanceException ex) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT,
				ex.getMessage());
		problemDetail.setTitle("Insufficient Balance");
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(problemDetail);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
		problemDetail.setTitle("Validation Error");
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult()
			.getFieldErrors()
			.forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
		problemDetail.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(problemDetail);
	}

}