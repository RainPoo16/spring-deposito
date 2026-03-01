package com.examples.deposit.mapper;

import java.math.BigDecimal;

import com.examples.deposit.domain.aggregate.Account;
import com.examples.deposit.domain.constant.TransactionType;
import com.examples.deposit.domain.entity.Transaction;
import com.examples.deposit.dto.response.AccountResponse;
import com.examples.deposit.dto.response.TransactionResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountMapperTests {

	@Test
	void toAccountResponse_mapsAllFields() {
		Account account = Account.create("Alice", "ACCT-MAP-001");
		account.credit(new BigDecimal("10.0000"));

		AccountResponse response = AccountMapper.toResponse(account);

		assertThat(response.id()).isEqualTo(account.getId());
		assertThat(response.accountNumber()).isEqualTo(account.getAccountNumber());
		assertThat(response.ownerName()).isEqualTo(account.getOwnerName());
		assertThat(response.balance()).isEqualByComparingTo(account.getBalance());
		assertThat(response.status()).isEqualTo(account.getStatus().name());
		assertThat(response.createdAt()).isEqualTo(account.getCreatedAt());
		assertThat(response.updatedAt()).isEqualTo(account.getUpdatedAt());
	}

	@Test
	void toTransactionResponse_mapsAllFields() {
		Account account = Account.create("Alice", "ACCT-MAP-002");
		Transaction transaction = Transaction.create(account, TransactionType.CREDIT, new BigDecimal("20.0000"),
				new BigDecimal("20.0000"));

		TransactionResponse response = AccountMapper.toResponse(transaction);

		assertThat(response.id()).isEqualTo(transaction.getId());
		assertThat(response.type()).isEqualTo(transaction.getType().name());
		assertThat(response.amount()).isEqualByComparingTo(transaction.getAmount());
		assertThat(response.balanceAfter()).isEqualByComparingTo(transaction.getBalanceAfter());
		assertThat(response.createdAt()).isEqualTo(transaction.getCreatedAt());
	}

}