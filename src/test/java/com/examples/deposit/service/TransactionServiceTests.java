package com.examples.deposit.service;

import java.math.BigDecimal;
import java.util.Optional;

import com.examples.deposit.domain.aggregate.Account;
import com.examples.deposit.domain.constant.TransactionType;
import com.examples.deposit.domain.entity.Transaction;
import com.examples.deposit.exception.InsufficientBalanceException;
import com.examples.deposit.exception.InvalidAccountStateException;
import com.examples.deposit.repository.AccountRepository;
import com.examples.deposit.repository.TransactionRepository;
import com.examples.deposit.service.transaction.TransactionService;
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
class TransactionServiceTests {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private TransactionRepository transactionRepository;

	@InjectMocks
	private TransactionService transactionService;

	@Test
	void credit_addsToBalanceAndRecordsTransaction() {
		Account account = Account.create("Alice", "ACCT-TX-SVC-001");
		when(this.accountRepository.findByAccountNumber("ACCT-TX-SVC-001")).thenReturn(Optional.of(account));
		when(this.accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(this.transactionRepository.save(any(Transaction.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		Transaction transaction = this.transactionService.credit("ACCT-TX-SVC-001", new BigDecimal("100.0000"));

		assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("100.0000"));
		assertThat(transaction.getType()).isEqualTo(TransactionType.CREDIT);
		assertThat(transaction.getAmount()).isEqualByComparingTo(new BigDecimal("100.0000"));
		assertThat(transaction.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("100.0000"));
		verify(this.accountRepository).save(account);
		verify(this.transactionRepository).save(any(Transaction.class));
	}

	@Test
	void credit_rejectsNonActiveAccount() {
		Account account = Account.create("Alice", "ACCT-TX-SVC-002");
		account.freeze();
		when(this.accountRepository.findByAccountNumber("ACCT-TX-SVC-002")).thenReturn(Optional.of(account));

		assertThatThrownBy(() -> this.transactionService.credit("ACCT-TX-SVC-002", new BigDecimal("10.0000")))
			.isInstanceOf(InvalidAccountStateException.class);
	}

	@Test
	void credit_rejectsNegativeAmount() {
		Account account = Account.create("Alice", "ACCT-TX-SVC-003");
		when(this.accountRepository.findByAccountNumber("ACCT-TX-SVC-003")).thenReturn(Optional.of(account));

		assertThatThrownBy(() -> this.transactionService.credit("ACCT-TX-SVC-003", new BigDecimal("-1.0000")))
			.isInstanceOf(InvalidAccountStateException.class);
	}

	@Test
	void debit_subtractsFromBalanceAndRecordsTransaction() {
		Account account = Account.create("Alice", "ACCT-TX-SVC-004");
		account.credit(new BigDecimal("100.0000"));
		when(this.accountRepository.findByAccountNumber("ACCT-TX-SVC-004")).thenReturn(Optional.of(account));
		when(this.accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(this.transactionRepository.save(any(Transaction.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		Transaction transaction = this.transactionService.debit("ACCT-TX-SVC-004", new BigDecimal("40.0000"));

		assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("60.0000"));
		assertThat(transaction.getType()).isEqualTo(TransactionType.DEBIT);
		assertThat(transaction.getAmount()).isEqualByComparingTo(new BigDecimal("40.0000"));
		assertThat(transaction.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("60.0000"));
		verify(this.accountRepository).save(account);
		verify(this.transactionRepository).save(any(Transaction.class));
	}

	@Test
	void debit_rejectsOverdraw() {
		Account account = Account.create("Alice", "ACCT-TX-SVC-005");
		account.credit(new BigDecimal("100.0000"));
		when(this.accountRepository.findByAccountNumber("ACCT-TX-SVC-005")).thenReturn(Optional.of(account));

		assertThatThrownBy(() -> this.transactionService.debit("ACCT-TX-SVC-005", new BigDecimal("200.0000")))
			.isInstanceOf(InsufficientBalanceException.class)
			.hasMessage("Insufficient balance for debit of 200.0000");
	}

	@Test
	void debit_rejectsNonActiveAccount() {
		Account account = Account.create("Alice", "ACCT-TX-SVC-006");
		account.freeze();
		when(this.accountRepository.findByAccountNumber("ACCT-TX-SVC-006")).thenReturn(Optional.of(account));

		assertThatThrownBy(() -> this.transactionService.debit("ACCT-TX-SVC-006", new BigDecimal("10.0000")))
			.isInstanceOf(InvalidAccountStateException.class);
	}

}
