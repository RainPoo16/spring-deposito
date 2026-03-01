package com.examples.deposit.repository;

import java.math.BigDecimal;

import com.examples.deposit.domain.aggregate.Account;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class AccountRepositoryTests {

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void shouldPersistAccountWithUUID() {
		Account account = Account.create("Alice", "ACCT-001");

		Account saved = this.accountRepository.saveAndFlush(account);

		assertThat(saved.getId()).isNotNull();
		assertThat(this.accountRepository.findById(saved.getId())).isPresent();
	}

	@Test
	void shouldEnforceUniqueAccountNumber() {
		Account first = Account.create("Alice", "ACCT-UNIQUE-001");
		Account second = Account.create("Bob", "ACCT-UNIQUE-001");

		this.accountRepository.saveAndFlush(first);

		assertThatThrownBy(() -> this.accountRepository.saveAndFlush(second))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void shouldIncrementVersionOnUpdate() {
		Account account = Account.create("Alice", "ACCT-VER-001");
		Account saved = this.accountRepository.saveAndFlush(account);
		Long originalVersion = saved.getVersion();

		saved.credit(new BigDecimal("50.0000"));
		Account updated = this.accountRepository.saveAndFlush(saved);

		assertThat(updated.getVersion()).isNotNull();
		assertThat(updated.getVersion()).isGreaterThan(originalVersion);
	}

	@Test
	void shouldThrowOnStaleUpdate() {
		Account account = Account.create("Alice", "ACCT-OPT-001");
		Account saved = this.accountRepository.saveAndFlush(account);

		this.entityManager.clear();

		Account firstReader = this.accountRepository.findById(saved.getId()).orElseThrow();
		this.entityManager.detach(firstReader);

		Account secondReader = this.accountRepository.findById(saved.getId()).orElseThrow();
		this.entityManager.detach(secondReader);

		firstReader.credit(new BigDecimal("10.0000"));
		this.accountRepository.saveAndFlush(firstReader);

		secondReader.credit(new BigDecimal("5.0000"));
		assertThatThrownBy(() -> this.accountRepository.saveAndFlush(secondReader))
			.isInstanceOf(ObjectOptimisticLockingFailureException.class);
	}

}