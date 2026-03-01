package com.examples.deposit.controller.admin;

import com.examples.deposit.domain.aggregate.Account;
import com.examples.deposit.dto.response.AccountResponse;
import com.examples.deposit.mapper.AccountMapper;
import com.examples.deposit.service.account.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
public class AccountAdminController {

	private final AccountService accountService;

	@PostMapping("/{accountNumber}/freeze")
	public ResponseEntity<AccountResponse> freeze(@PathVariable String accountNumber) {
		Account account = this.accountService.freezeAccount(accountNumber);
		return ResponseEntity.ok(AccountMapper.toResponse(account));
	}

	@PostMapping("/{accountNumber}/unfreeze")
	public ResponseEntity<AccountResponse> unfreeze(@PathVariable String accountNumber) {
		Account account = this.accountService.unfreezeAccount(accountNumber);
		return ResponseEntity.ok(AccountMapper.toResponse(account));
	}

	@PostMapping("/{accountNumber}/close")
	public ResponseEntity<AccountResponse> close(@PathVariable String accountNumber) {
		Account account = this.accountService.closeAccount(accountNumber);
		return ResponseEntity.ok(AccountMapper.toResponse(account));
	}

}