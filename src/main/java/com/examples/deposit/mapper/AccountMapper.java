package com.examples.deposit.mapper;

import com.examples.deposit.domain.aggregate.Account;
import com.examples.deposit.domain.entity.Transaction;
import com.examples.deposit.dto.response.AccountResponse;
import com.examples.deposit.dto.response.TransactionResponse;

public final class AccountMapper {

	private AccountMapper() {
	}

	public static AccountResponse toResponse(Account account) {
		return new AccountResponse(account.getId(), account.getAccountNumber(), account.getOwnerName(),
				account.getBalance(), account.getStatus().name(), account.getCreatedAt(), account.getUpdatedAt());
	}

	public static TransactionResponse toResponse(Transaction transaction) {
		return new TransactionResponse(transaction.getId(), transaction.getType().name(), transaction.getAmount(),
				transaction.getBalanceAfter(), transaction.getCreatedAt());
	}

}