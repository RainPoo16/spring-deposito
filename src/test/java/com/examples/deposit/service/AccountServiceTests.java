package com.examples.deposit.service;

import java.math.BigDecimal;
import java.util.Optional;

import com.examples.deposit.domain.aggregate.Account;
import com.examples.deposit.domain.constant.AccountStatus;
import com.examples.deposit.exception.AccountNotFoundException;
import com.examples.deposit.exception.InvalidAccountStateException;
import com.examples.deposit.repository.AccountRepository;
import com.examples.deposit.service.account.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTests {

	@Mock
	private AccountRepository accountRepository;

	@InjectMocks
	private AccountService accountService;

	@Test
	void openAccount_createsActiveAccountWithZeroBalance() {
		when(this.accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Account account = this.accountService.openAccount("Alice", "ACCT-SVC-001");

		assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO.setScale(4));
		verify(this.accountRepository).save(any(Account.class));
	}

	@Test
	void freezeAccount_transitionsActiveToFrozen() {
		Account account = Account.create("Alice", "ACCT-SVC-002");
		when(this.accountRepository.findByAccountNumber("ACCT-SVC-002")).thenReturn(Optional.of(account));
		when(this.accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Account result = this.accountService.freezeAccount("ACCT-SVC-002");

		assertThat(result.getStatus()).isEqualTo(AccountStatus.FROZEN);
		verify(this.accountRepository).save(account);
	}

	@Test
	void unfreezeAccount_transitionsFrozenToActive() {
		Account account = Account.create("Alice", "ACCT-SVC-003");
		account.freeze();
		when(this.accountRepository.findByAccountNumber("ACCT-SVC-003")).thenReturn(Optional.of(account));
		when(this.accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Account result = this.accountService.unfreezeAccount("ACCT-SVC-003");

		assertThat(result.getStatus()).isEqualTo(AccountStatus.ACTIVE);
		verify(this.accountRepository).save(account);
	}

	@Test
	void closeAccount_closesZeroBalanceActiveAccount() {
		Account account = Account.create("Alice", "ACCT-SVC-004");
		when(this.accountRepository.findByAccountNumber("ACCT-SVC-004")).thenReturn(Optional.of(account));
		when(this.accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Account result = this.accountService.closeAccount("ACCT-SVC-004");

		assertThat(result.getStatus()).isEqualTo(AccountStatus.CLOSED);
		verify(this.accountRepository).save(account);
	}

	@Test
	void closeAccount_rejectsNonZeroBalance() {
		Account account = Account.create("Alice", "ACCT-SVC-005");
		account.credit(new BigDecimal("10.0000"));
		when(this.accountRepository.findByAccountNumber("ACCT-SVC-005")).thenReturn(Optional.of(account));

		assertThatThrownBy(() -> this.accountService.closeAccount("ACCT-SVC-005"))
			.isInstanceOf(InvalidAccountStateException.class);
	}

	@Test
	void getAccount_throwsWhenNotFound() {
		when(this.accountRepository.findByAccountNumber("ACCT-NOT-FOUND")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.accountService.getAccount("ACCT-NOT-FOUND"))
			.isInstanceOf(AccountNotFoundException.class)
			.hasMessage("Account not found: ACCT-NOT-FOUND");
	}

}
