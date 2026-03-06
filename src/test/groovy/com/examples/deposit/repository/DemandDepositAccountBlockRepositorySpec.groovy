package com.examples.deposit.repository

import com.examples.deposit.domain.AccountBlockStatus
import com.examples.deposit.domain.BlockCode
import com.examples.deposit.domain.BlockRequestedBy
import com.examples.deposit.domain.DemandDepositAccount
import com.examples.deposit.domain.DemandDepositAccountBlock
import com.examples.deposit.domain.DemandDepositAccountStatus
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import spock.lang.Specification

import java.time.LocalDate
import java.util.UUID

@DataJpaTest
class DemandDepositAccountBlockRepositorySpec extends Specification {

    @Autowired
    DemandDepositAccountRepository demandDepositAccountRepository

    @Autowired
    DemandDepositAccountBlockRepository demandDepositAccountBlockRepository

    @Autowired
    EntityManager entityManager

    def "persists account block enums as strings"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.PENDING_VERIFICATION)
        )

        and:
        DemandDepositAccountBlock block = DemandDepositAccountBlock.create(
            account.id,
            BlockCode.ACC,
            BlockRequestedBy.CUSTOMER,
            AccountBlockStatus.PENDING,
            LocalDate.of(2026, 3, 6),
            LocalDate.of(2026, 3, 31),
            "customer requested hold"
        )

        when:
        DemandDepositAccountBlock persisted = demandDepositAccountBlockRepository.saveAndFlush(block)
        entityManager.clear()

        then:
        String persistedBlockCode = (String) entityManager
            .createNativeQuery("SELECT block_code FROM demand_deposit_account_block WHERE id = :id")
            .setParameter("id", persisted.id)
            .getSingleResult()
        String persistedRequestedBy = (String) entityManager
            .createNativeQuery("SELECT requested_by FROM demand_deposit_account_block WHERE id = :id")
            .setParameter("id", persisted.id)
            .getSingleResult()
        String persistedStatus = (String) entityManager
            .createNativeQuery("SELECT status FROM demand_deposit_account_block WHERE id = :id")
            .setParameter("id", persisted.id)
            .getSingleResult()

        persistedBlockCode == "ACC"
        persistedRequestedBy == "CUSTOMER"
        persistedStatus == "PENDING"
    }

    def "detects overlapping active or pending block with same account and code"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.PENDING_VERIFICATION)
        )

        and:
        demandDepositAccountBlockRepository.saveAndFlush(
            DemandDepositAccountBlock.create(
                account.id,
                BlockCode.ACC,
                BlockRequestedBy.CUSTOMER,
                AccountBlockStatus.ACTIVE,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 10),
                "existing active block"
            )
        )

        when:
        boolean overlaps = demandDepositAccountBlockRepository.existsOverlappingActiveOrPendingBlock(
            account.id,
            BlockCode.ACC,
            LocalDate.of(2026, 3, 5),
            LocalDate.of(2026, 3, 15)
        )

        and:
        boolean notOverlapping = demandDepositAccountBlockRepository.existsOverlappingActiveOrPendingBlock(
            account.id,
            BlockCode.ACC,
            LocalDate.of(2026, 3, 11),
            LocalDate.of(2026, 3, 20)
        )

        then:
        overlaps
        !notOverlapping
    }

    def "does not reject overlap when existing block is cancelled"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.PENDING_VERIFICATION)
        )

        and:
        demandDepositAccountBlockRepository.saveAndFlush(
            DemandDepositAccountBlock.create(
                account.id,
                BlockCode.ACC,
                BlockRequestedBy.CUSTOMER,
                AccountBlockStatus.CANCELLED,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                "cancelled block"
            )
        )

        when:
        boolean overlaps = demandDepositAccountBlockRepository.existsOverlappingActiveOrPendingBlock(
            account.id,
            BlockCode.ACC,
            LocalDate.of(2026, 3, 10),
            LocalDate.of(2026, 3, 20)
        )

        then:
        !overlaps
    }

    def "detects active incoming restriction for credit postings"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )

        and:
        demandDepositAccountBlockRepository.saveAndFlush(
            DemandDepositAccountBlock.create(
                account.id,
                BlockCode.ACB,
                BlockRequestedBy.BANK,
                AccountBlockStatus.ACTIVE,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                "incoming restriction"
            )
        )

        when:
        boolean hasCreditRestriction = demandDepositAccountBlockRepository.existsActiveCreditRestrictionOn(
            account.id,
            LocalDate.of(2026, 3, 10)
        )
        boolean noRestrictionAfterExpiry = demandDepositAccountBlockRepository.existsActiveCreditRestrictionOn(
            account.id,
            LocalDate.of(2026, 4, 1)
        )

        then:
        hasCreditRestriction
        !noRestrictionAfterExpiry
    }

    def "detects active outgoing restriction for debit postings"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )

        and:
        demandDepositAccountBlockRepository.saveAndFlush(
            DemandDepositAccountBlock.create(
                account.id,
                BlockCode.ADB,
                BlockRequestedBy.BANK,
                AccountBlockStatus.ACTIVE,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                "outgoing restriction"
            )
        )

        when:
        boolean hasDebitRestriction = demandDepositAccountBlockRepository.existsActiveDebitRestrictionOn(
            account.id,
            LocalDate.of(2026, 3, 10)
        )
        boolean noRestrictionAfterExpiry = demandDepositAccountBlockRepository.existsActiveDebitRestrictionOn(
            account.id,
            LocalDate.of(2026, 4, 1)
        )

        then:
        hasDebitRestriction
        !noRestrictionAfterExpiry
    }
}
