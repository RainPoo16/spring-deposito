package com.examples.deposit.service.transaction;

import java.math.BigDecimal;
import java.util.List;

import com.examples.deposit.domain.aggregate.Account;
import com.examples.deposit.domain.constant.TransactionType;
import com.examples.deposit.domain.entity.Transaction;
import com.examples.deposit.exception.AccountNotFoundException;
import com.examples.deposit.exception.InsufficientBalanceException;
import com.examples.deposit.exception.InvalidAccountStateException;
import com.examples.deposit.repository.AccountRepository;
import com.examples.deposit.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {

	private final AccountRepository accountRepository;

	private final TransactionRepository transactionRepository;

	@Transactional(rollbackFor = Exception.class)
	public Transaction credit(String accountNumber, BigDecimal amount) {
		Account account = this.accountRepository.findByAccountNumber(accountNumber)
			.orElseThrow(() -> new AccountNotFoundException(accountNumber));
		try {
			account.credit(amount);
		}
		catch (IllegalStateException ex) {
			throw new InvalidAccountStateException(ex.getMessage());
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidAccountStateException(ex.getMessage());
		}
		this.accountRepository.save(account);
		Transaction transaction = Transaction.create(account, TransactionType.CREDIT, amount, account.getBalance());
		return this.transactionRepository.save(transaction);
	}

	@Transactional(rollbackFor = Exception.class)
	public Transaction debit(String accountNumber, BigDecimal amount) {
		Account account = this.accountRepository.findByAccountNumber(accountNumber)
			.orElseThrow(() -> new AccountNotFoundException(accountNumber));
		try {
			account.debit(amount);
		}
		catch (InsufficientBalanceException ex) {
			throw ex;
		}
		catch (IllegalStateException ex) {
			throw new InvalidAccountStateException(ex.getMessage());
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidAccountStateException(ex.getMessage());
		}
		this.accountRepository.save(account);
		Transaction transaction = Transaction.create(account, TransactionType.DEBIT, amount, account.getBalance());
		return this.transactionRepository.save(transaction);
	}

	@Transactional(readOnly = true)
	public List<Transaction> getTransactions(String accountNumber) {
		Account account = this.accountRepository.findByAccountNumber(accountNumber)
			.orElseThrow(() -> new AccountNotFoundException(accountNumber));
		return this.transactionRepository.findByAccountIdOrderByCreatedAtDesc(account.getId());
	}

}
