package com.examples.deposit.repository;

import com.examples.deposit.domain.DemandDepositAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DemandDepositAccountRepository extends JpaRepository<DemandDepositAccount, UUID> {

    Optional<DemandDepositAccount> findByCustomerId(UUID customerId);

    Optional<DemandDepositAccount> findByIdAndCustomerId(UUID id, UUID customerId);
}
