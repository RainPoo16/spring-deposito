package com.examples.deposit.repository;

import com.examples.deposit.domain.AccountBlockStatus;
import com.examples.deposit.domain.BlockCode;
import com.examples.deposit.domain.DemandDepositAccountBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DemandDepositAccountBlockRepository extends JpaRepository<DemandDepositAccountBlock, UUID> {

    List<BlockCode> CREDIT_RESTRICTION_CODES = List.of(BlockCode.ACB, BlockCode.ACC, BlockCode.ACG);
    List<BlockCode> DEBIT_RESTRICTION_CODES = List.of(BlockCode.ADB);

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

    boolean existsByAccountIdAndBlockCodeInAndStatusAndEffectiveDateLessThanEqualAndExpiryDateGreaterThanEqual(
        UUID accountId,
        List<BlockCode> blockCodes,
        AccountBlockStatus status,
        LocalDate expiryDate,
        LocalDate effectiveDate
    );

    default boolean existsActiveCreditRestrictionOn(UUID accountId, LocalDate asOfDate) {
        return existsByAccountIdAndBlockCodeInAndStatusAndEffectiveDateLessThanEqualAndExpiryDateGreaterThanEqual(
            accountId,
            CREDIT_RESTRICTION_CODES,
            AccountBlockStatus.ACTIVE,
            asOfDate,
            asOfDate
        );
    }

    default boolean existsActiveDebitRestrictionOn(UUID accountId, LocalDate asOfDate) {
        return existsByAccountIdAndBlockCodeInAndStatusAndEffectiveDateLessThanEqualAndExpiryDateGreaterThanEqual(
            accountId,
            DEBIT_RESTRICTION_CODES,
            AccountBlockStatus.ACTIVE,
            asOfDate,
            asOfDate
        );
    }
}
