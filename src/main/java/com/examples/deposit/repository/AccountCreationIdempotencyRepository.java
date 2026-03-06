package com.examples.deposit.repository;

import com.examples.deposit.domain.AccountCreationIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountCreationIdempotencyRepository extends JpaRepository<AccountCreationIdempotency, UUID> {

    Optional<AccountCreationIdempotency> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);
}
