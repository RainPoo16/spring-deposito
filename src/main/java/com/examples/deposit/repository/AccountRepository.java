package com.examples.deposit.repository;

import java.util.Optional;
import java.util.UUID;

import com.examples.deposit.domain.aggregate.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

	Optional<Account> findByAccountNumber(String accountNumber);

}