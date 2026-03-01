package com.examples.deposit.repository;

import java.util.List;
import java.util.UUID;

import com.examples.deposit.domain.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

	List<Transaction> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

}