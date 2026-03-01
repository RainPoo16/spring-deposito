package com.examples.deposit.repository;

import java.math.BigDecimal;
import java.util.List;

import com.examples.deposit.domain.aggregate.Account;
import com.examples.deposit.domain.constant.TransactionType;
import com.examples.deposit.domain.entity.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class TransactionRepositoryTests {

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Test
	void shouldPersistTransactionLinkedToAccount() {
		Account account = this.accountRepository.saveAndFlush(Account.create("Alice", "ACCT-TXN-001"));
		Transaction transaction = Transaction.create(account, TransactionType.CREDIT, new BigDecimal("100.0000"),
				new BigDecimal("100.0000"));

		Transaction saved = this.transactionRepository.saveAndFlush(transaction);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getAccount().getId()).isEqualTo(account.getId());
	}

	@Test
	void shouldFindTransactionsByAccountId() {
		Account account = this.accountRepository.saveAndFlush(Account.create("Alice", "ACCT-TXN-002"));
		this.transactionRepository.saveAndFlush(Transaction.create(account, TransactionType.CREDIT,
				new BigDecimal("100.0000"), new BigDecimal("100.0000")));
		this.transactionRepository.saveAndFlush(Transaction.create(account, TransactionType.DEBIT,
				new BigDecimal("40.0000"), new BigDecimal("60.0000")));

		List<Transaction> transactions = this.transactionRepository
			.findByAccountIdOrderByCreatedAtDesc(account.getId());

		assertThat(transactions).hasSize(2);
		assertThat(transactions.get(0).getCreatedAt()).isAfterOrEqualTo(transactions.get(1).getCreatedAt());
	}

}