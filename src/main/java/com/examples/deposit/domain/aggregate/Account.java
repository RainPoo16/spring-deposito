package com.examples.deposit.domain.aggregate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.examples.deposit.domain.constant.AccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

	@Id
	private UUID id;

	@Column(name = "account_number", nullable = false, unique = true)
	private String accountNumber;

	@Column(name = "owner_name", nullable = false)
	private String ownerName;

	@Column(nullable = false, precision = 19, scale = 4, columnDefinition = "DECIMAL(19,4) DEFAULT 0")
	private BigDecimal balance;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AccountStatus status;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Version
	private Long version;

	public static Account create(String ownerName, String accountNumber) {
		Account account = new Account();
		account.id = UUID.randomUUID();
		account.ownerName = ownerName;
		account.accountNumber = accountNumber;
		account.balance = BigDecimal.ZERO.setScale(4);
		account.status = AccountStatus.ACTIVE;
		account.createdAt = LocalDateTime.now();
		return account;
	}

	public void credit(BigDecimal amount) {
		requireActiveStatus();
		requirePositiveAmount(amount);
		this.balance = this.balance.add(amount);
		this.updatedAt = LocalDateTime.now();
	}

	public void debit(BigDecimal amount) {
		requireActiveStatus();
		requirePositiveAmount(amount);
		if (this.balance.compareTo(amount) < 0) {
			throw new IllegalStateException("Insufficient balance");
		}
		this.balance = this.balance.subtract(amount);
		this.updatedAt = LocalDateTime.now();
	}

	public void freeze() {
		if (this.status != AccountStatus.ACTIVE) {
			throw new IllegalStateException("Only active accounts can be frozen");
		}
		this.status = AccountStatus.FROZEN;
		this.updatedAt = LocalDateTime.now();
	}

	public void unfreeze() {
		if (this.status != AccountStatus.FROZEN) {
			throw new IllegalStateException("Only frozen accounts can be unfrozen");
		}
		this.status = AccountStatus.ACTIVE;
		this.updatedAt = LocalDateTime.now();
	}

	public void close() {
		if (this.status != AccountStatus.ACTIVE) {
			throw new IllegalStateException("Only active accounts can be closed");
		}
		if (this.balance.compareTo(BigDecimal.ZERO) != 0) {
			throw new IllegalStateException("Account balance must be zero before close");
		}
		this.status = AccountStatus.CLOSED;
		this.updatedAt = LocalDateTime.now();
	}

	private void requireActiveStatus() {
		if (this.status != AccountStatus.ACTIVE) {
			throw new IllegalStateException("Account must be active");
		}
	}

	private void requirePositiveAmount(BigDecimal amount) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Amount must be positive");
		}
	}

}