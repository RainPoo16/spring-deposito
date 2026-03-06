package com.examples.deposit.repository;

import com.examples.deposit.domain.DemandDepositAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DemandDepositAccountRepository extends JpaRepository<DemandDepositAccount, UUID> {

    Optional<DemandDepositAccount> findByCustomerId(UUID customerId);

    Optional<DemandDepositAccount> findByIdAndCustomerId(UUID id, UUID customerId);

    @Query(
        value = "SELECT * FROM demand_deposit_account WHERE id = :id AND customer_id = :customerId FOR UPDATE",
        nativeQuery = true
    )
    Optional<DemandDepositAccount> findByIdAndCustomerIdForUpdate(@Param("id") UUID id, @Param("customerId") UUID customerId);
}
