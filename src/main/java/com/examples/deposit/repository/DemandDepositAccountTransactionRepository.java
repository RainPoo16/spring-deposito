package com.examples.deposit.repository;

import com.examples.deposit.domain.DemandDepositAccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DemandDepositAccountTransactionRepository extends JpaRepository<DemandDepositAccountTransaction, UUID> {

    Optional<DemandDepositAccountTransaction> findByAccountIdAndReferenceId(UUID accountId, String referenceId);
}
