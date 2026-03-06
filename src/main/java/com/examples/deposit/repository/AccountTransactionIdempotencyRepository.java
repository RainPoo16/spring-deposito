package com.examples.deposit.repository;

import com.examples.deposit.domain.AccountTransactionIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountTransactionIdempotencyRepository extends JpaRepository<AccountTransactionIdempotency, UUID> {

    Optional<AccountTransactionIdempotency> findByCustomerIdAndIdempotencyKeyAndReferenceId(
        UUID customerId,
        String idempotencyKey,
        String referenceId
    );
}
