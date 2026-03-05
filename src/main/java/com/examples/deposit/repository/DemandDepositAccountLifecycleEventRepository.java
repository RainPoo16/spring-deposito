package com.examples.deposit.repository;

import com.examples.deposit.domain.DemandDepositAccountLifecycleEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DemandDepositAccountLifecycleEventRepository extends JpaRepository<DemandDepositAccountLifecycleEvent, UUID> {
}
