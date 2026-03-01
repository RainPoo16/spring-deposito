package com.examples.deposit.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.examples.deposit.domain.aggregate.Account;
import com.examples.deposit.domain.constant.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TransactionType type;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal amount;

	@Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
	private BigDecimal balanceAfter;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	public static Transaction create(Account account, TransactionType type, BigDecimal amount,
			BigDecimal balanceAfter) {
		Transaction transaction = new Transaction();
		transaction.id = UUID.randomUUID();
		transaction.account = account;
		transaction.type = type;
		transaction.amount = amount;
		transaction.balanceAfter = balanceAfter;
		transaction.createdAt = LocalDateTime.now();
		return transaction;
	}

}