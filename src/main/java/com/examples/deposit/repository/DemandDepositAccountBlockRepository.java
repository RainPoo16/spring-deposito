package com.examples.deposit.repository;

import com.examples.deposit.domain.AccountBlockStatus;
import com.examples.deposit.domain.BlockCode;
import com.examples.deposit.domain.DemandDepositAccountBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DemandDepositAccountBlockRepository extends JpaRepository<DemandDepositAccountBlock, UUID> {

    boolean existsByAccountIdAndBlockCodeAndStatusInAndEffectiveDateLessThanEqualAndExpiryDateGreaterThanEqual(
        UUID accountId,
        BlockCode blockCode,
        List<AccountBlockStatus> statuses,
        LocalDate expiryDate,
        LocalDate effectiveDate
    );

    default boolean existsOverlappingActiveOrPendingBlock(
        UUID accountId,
        BlockCode blockCode,
        LocalDate effectiveDate,
        LocalDate expiryDate
    ) {
        return existsByAccountIdAndBlockCodeAndStatusInAndEffectiveDateLessThanEqualAndExpiryDateGreaterThanEqual(
            accountId,
            blockCode,
            List.of(AccountBlockStatus.ACTIVE, AccountBlockStatus.PENDING),
            expiryDate,
            effectiveDate
        );
    }
}
