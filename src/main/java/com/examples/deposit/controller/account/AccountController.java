package com.examples.deposit.controller.account;

import com.examples.deposit.domain.aggregate.Account;
import com.examples.deposit.domain.entity.Transaction;
import com.examples.deposit.dto.request.CreditRequest;
import com.examples.deposit.dto.request.DebitRequest;
import com.examples.deposit.dto.request.OpenAccountRequest;
import com.examples.deposit.dto.response.AccountResponse;
import com.examples.deposit.dto.response.TransactionResponse;
import com.examples.deposit.mapper.AccountMapper;
import com.examples.deposit.service.account.AccountService;
import com.examples.deposit.service.transaction.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

	private final AccountService accountService;

	private final TransactionService transactionService;

	@PostMapping
	public ResponseEntity<AccountResponse> openAccount(@Valid @RequestBody OpenAccountRequest request) {
		Account account = this.accountService.openAccount(request.ownerName(), request.accountNumber());
		return ResponseEntity.status(HttpStatus.CREATED).body(AccountMapper.toResponse(account));
	}

	@GetMapping("/{accountNumber}")
	public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber) {
		Account account = this.accountService.getAccount(accountNumber);
		return ResponseEntity.ok(AccountMapper.toResponse(account));
	}

	@PostMapping("/{accountNumber}/credit")
	public ResponseEntity<TransactionResponse> credit(@PathVariable String accountNumber,
			@Valid @RequestBody CreditRequest request) {
		Transaction transaction = this.transactionService.credit(accountNumber, request.amount());
		return ResponseEntity.ok(AccountMapper.toResponse(transaction));
	}

	@PostMapping("/{accountNumber}/debit")
	public ResponseEntity<TransactionResponse> debit(@PathVariable String accountNumber,
			@Valid @RequestBody DebitRequest request) {
		Transaction transaction = this.transactionService.debit(accountNumber, request.amount());
		return ResponseEntity.ok(AccountMapper.toResponse(transaction));
	}

}