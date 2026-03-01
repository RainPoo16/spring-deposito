package com.examples.deposit.service.account;

import com.examples.deposit.domain.aggregate.Account;
import com.examples.deposit.exception.AccountNotFoundException;
import com.examples.deposit.exception.InvalidAccountStateException;
import com.examples.deposit.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

	private final AccountRepository accountRepository;

	@Transactional(rollbackFor = Exception.class)
	public Account openAccount(String ownerName, String accountNumber) {
		Account account = Account.create(ownerName, accountNumber);
		return this.accountRepository.save(account);
	}

	@Transactional(readOnly = true)
	public Account getAccount(String accountNumber) {
		return this.accountRepository.findByAccountNumber(accountNumber)
			.orElseThrow(() -> new AccountNotFoundException(accountNumber));
	}

	@Transactional(rollbackFor = Exception.class)
	public Account freezeAccount(String accountNumber) {
		Account account = getAccountForUpdate(accountNumber);
		try {
			account.freeze();
		}
		catch (IllegalStateException ex) {
			throw new InvalidAccountStateException(ex.getMessage());
		}
		return this.accountRepository.save(account);
	}

	@Transactional(rollbackFor = Exception.class)
	public Account unfreezeAccount(String accountNumber) {
		Account account = getAccountForUpdate(accountNumber);
		try {
			account.unfreeze();
		}
		catch (IllegalStateException ex) {
			throw new InvalidAccountStateException(ex.getMessage());
		}
		return this.accountRepository.save(account);
	}

	@Transactional(rollbackFor = Exception.class)
	public Account closeAccount(String accountNumber) {
		Account account = getAccountForUpdate(accountNumber);
		try {
			account.close();
		}
		catch (IllegalStateException ex) {
			throw new InvalidAccountStateException(ex.getMessage());
		}
		return this.accountRepository.save(account);
	}

	private Account getAccountForUpdate(String accountNumber) {
		return this.accountRepository.findByAccountNumber(accountNumber)
			.orElseThrow(() -> new AccountNotFoundException(accountNumber));
	}

}
