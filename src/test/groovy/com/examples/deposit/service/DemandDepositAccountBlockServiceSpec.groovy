package com.examples.deposit.service

import com.examples.deposit.domain.AccountBlockStatus
import com.examples.deposit.domain.BlockCode
import com.examples.deposit.domain.BlockRequestedBy
import com.examples.deposit.domain.DemandDepositAccount
import com.examples.deposit.domain.DemandDepositAccountBlock
import com.examples.deposit.domain.DemandDepositAccountStatus
import com.examples.deposit.domain.exception.AccountNotFoundException
import com.examples.deposit.domain.exception.BlockNotEligibleForOperationException
import com.examples.deposit.domain.exception.DuplicateOrOverlappingBlockException
import com.examples.deposit.repository.DemandDepositAccountBlockRepository
import com.examples.deposit.repository.DemandDepositAccountRepository
import com.examples.deposit.service.dto.CreateDemandDepositAccountBlockCommand
import com.examples.deposit.service.dto.UpdateDemandDepositAccountBlockCommand
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional
import org.spockframework.spring.SpringBean
import spock.lang.Specification

import java.time.LocalDate
import java.util.UUID

@SpringBootTest
@Transactional
class DemandDepositAccountBlockServiceSpec extends Specification {

    @Autowired
    DemandDepositAccountBlockService demandDepositAccountBlockService

    @Autowired
    DemandDepositAccountRepository demandDepositAccountRepository

    @Autowired
    DemandDepositAccountBlockRepository demandDepositAccountBlockRepository

    @SpringBean
    BlockLifecycleEventPublisher blockLifecycleEventPublisher = Mock()

    def "creates block for owned account with valid payload"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )

        CreateDemandDepositAccountBlockCommand command = new CreateDemandDepositAccountBlockCommand(
            account.id,
            customerId,
            "ACC",
            LocalDate.of(2026, 3, 10),
            LocalDate.of(2026, 3, 20),
            "customer requested temporary incoming block"
        )

        when:
        def createdBlock = demandDepositAccountBlockService.createBlock(command)

        then:
        createdBlock.accountId == account.id
        createdBlock.blockCode == BlockCode.ACC
        createdBlock.status == AccountBlockStatus.PENDING
        createdBlock.effectiveDate == LocalDate.of(2026, 3, 10)
        createdBlock.expiryDate == LocalDate.of(2026, 3, 20)
        demandDepositAccountBlockRepository.count() == 1
    }

    def "rejects #caseDescription for customer endpoint context"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )

        CreateDemandDepositAccountBlockCommand command = new CreateDemandDepositAccountBlockCommand(
            account.id,
            customerId,
            blockCode,
            LocalDate.of(2026, 3, 10),
            LocalDate.of(2026, 3, 20),
            "invalid for customer endpoint"
        )

        when:
        demandDepositAccountBlockService.createBlock(command)

        then:
        thrown(BlockNotEligibleForOperationException)
        demandDepositAccountBlockRepository.count() == 0

        where:
        caseDescription                 | blockCode
        "unknown block code"           | "ZZZ"
        "non-customer-allowed code"   | "ACB"
    }

    def "rejects duplicate or overlapping block for same account and code"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )

        demandDepositAccountBlockService.createBlock(new CreateDemandDepositAccountBlockCommand(
            account.id,
            customerId,
            "ACC",
            LocalDate.of(2026, 3, 10),
            LocalDate.of(2026, 3, 20),
            "first"
        ))

        CreateDemandDepositAccountBlockCommand overlappingCommand = new CreateDemandDepositAccountBlockCommand(
            account.id,
            customerId,
            "ACC",
            LocalDate.of(2026, 3, 15),
            LocalDate.of(2026, 3, 25),
            "overlapping"
        )

        when:
        demandDepositAccountBlockService.createBlock(overlappingCommand)

        then:
        thrown(DuplicateOrOverlappingBlockException)
        demandDepositAccountBlockRepository.count() == 1
    }

    def "rejects invalid date range when effective date is after expiry date"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )

        CreateDemandDepositAccountBlockCommand command = new CreateDemandDepositAccountBlockCommand(
            account.id,
            customerId,
            "ACC",
            LocalDate.of(2026, 3, 21),
            LocalDate.of(2026, 3, 20),
            "invalid date range"
        )

        when:
        demandDepositAccountBlockService.createBlock(command)

        then:
        thrown(BlockNotEligibleForOperationException)
        demandDepositAccountBlockRepository.count() == 0
    }

    def "rejects account ownership mismatch"() {
        given:
        UUID accountOwnerId = UUID.randomUUID()
        UUID anotherCustomerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(accountOwnerId, DemandDepositAccountStatus.ACTIVE)
        )

        CreateDemandDepositAccountBlockCommand command = new CreateDemandDepositAccountBlockCommand(
            account.id,
            anotherCustomerId,
            "ACC",
            LocalDate.of(2026, 3, 10),
            LocalDate.of(2026, 3, 20),
            "wrong owner"
        )

        when:
        demandDepositAccountBlockService.createBlock(command)

        then:
        thrown(AccountNotFoundException)
        demandDepositAccountBlockRepository.count() == 0
    }

    def "updates block when status and initiator combination is eligible"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )
        def existingBlock = demandDepositAccountBlockRepository.saveAndFlush(
            DemandDepositAccountBlock.create(
                account.id,
                BlockCode.ACC,
                BlockRequestedBy.CUSTOMER,
                AccountBlockStatus.PENDING,
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 20),
                "before update"
            )
        )

        UpdateDemandDepositAccountBlockCommand command = new UpdateDemandDepositAccountBlockCommand(
            account.id,
            customerId,
            existingBlock.id,
            LocalDate.of(2026, 3, 12),
            LocalDate.of(2026, 3, 22),
            "after update"
        )

        when:
        def updatedBlock = demandDepositAccountBlockService.updateBlock(command)

        then:
        updatedBlock.id == existingBlock.id
        updatedBlock.status == AccountBlockStatus.PENDING
        updatedBlock.effectiveDate == LocalDate.of(2026, 3, 12)
        updatedBlock.expiryDate == LocalDate.of(2026, 3, 22)
        updatedBlock.remark == "after update"
    }

    def "cancels block when status and initiator combination is eligible"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )
        def existingBlock = demandDepositAccountBlockRepository.saveAndFlush(
            DemandDepositAccountBlock.create(
                account.id,
                BlockCode.ACC,
                BlockRequestedBy.CUSTOMER,
                AccountBlockStatus.ACTIVE,
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 20),
                "active block"
            )
        )

        UpdateDemandDepositAccountBlockCommand command = new UpdateDemandDepositAccountBlockCommand(
            account.id,
            customerId,
            existingBlock.id,
            LocalDate.of(2026, 3, 10),
            LocalDate.of(2026, 3, 20),
            "cancel"
        )

        when:
        def cancelledBlock = demandDepositAccountBlockService.cancelBlock(command)

        then:
        cancelledBlock.status == AccountBlockStatus.CANCELLED
    }

    def "rejects update and cancel for ineligible status or initiator combinations"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )
        def cancelledBlock = demandDepositAccountBlockRepository.saveAndFlush(
            DemandDepositAccountBlock.create(
                account.id,
                BlockCode.ACC,
                BlockRequestedBy.CUSTOMER,
                AccountBlockStatus.CANCELLED,
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 20),
                "already cancelled"
            )
        )
        def bankInitiatedBlock = demandDepositAccountBlockRepository.saveAndFlush(
            DemandDepositAccountBlock.create(
                account.id,
                BlockCode.ACB,
                BlockRequestedBy.BANK,
                AccountBlockStatus.ACTIVE,
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 20),
                "bank block"
            )
        )

        when:
        demandDepositAccountBlockService.updateBlock(new UpdateDemandDepositAccountBlockCommand(
            account.id,
            customerId,
            cancelledBlock.id,
            LocalDate.of(2026, 3, 11),
            LocalDate.of(2026, 3, 21),
            "invalid"
        ))

        then:
        thrown(BlockNotEligibleForOperationException)

        when:
        demandDepositAccountBlockService.cancelBlock(new UpdateDemandDepositAccountBlockCommand(
            account.id,
            customerId,
            bankInitiatedBlock.id,
            LocalDate.of(2026, 3, 10),
            LocalDate.of(2026, 3, 20),
            "invalid"
        ))

        then:
        thrown(BlockNotEligibleForOperationException)
    }

    def "rejects repeated cancel operation explicitly"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )
        def existingBlock = demandDepositAccountBlockRepository.saveAndFlush(
            DemandDepositAccountBlock.create(
                account.id,
                BlockCode.ACC,
                BlockRequestedBy.CUSTOMER,
                AccountBlockStatus.ACTIVE,
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 20),
                "to cancel twice"
            )
        )
        UpdateDemandDepositAccountBlockCommand command = new UpdateDemandDepositAccountBlockCommand(
            account.id,
            customerId,
            existingBlock.id,
            LocalDate.of(2026, 3, 10),
            LocalDate.of(2026, 3, 20),
            "cancel"
        )

        when:
        def firstCancel = demandDepositAccountBlockService.cancelBlock(command)

        then:
        firstCancel.status == AccountBlockStatus.CANCELLED

        when:
        demandDepositAccountBlockService.cancelBlock(command)

        then:
        thrown(BlockNotEligibleForOperationException)
        demandDepositAccountBlockRepository.findById(existingBlock.id).orElseThrow().status == AccountBlockStatus.CANCELLED
    }

    def "invokes lifecycle hook once after successful create update and cancel"() {
        given:
        UUID customerId = UUID.randomUUID()
        UUID createdBlockId = null
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )
        def existingBlock = demandDepositAccountBlockRepository.saveAndFlush(
            DemandDepositAccountBlock.create(
                account.id,
                BlockCode.ACC,
                BlockRequestedBy.CUSTOMER,
                AccountBlockStatus.PENDING,
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 20),
                "before update"
            )
        )

        when:
        def createdBlock = demandDepositAccountBlockService.createBlock(new CreateDemandDepositAccountBlockCommand(
            account.id,
            customerId,
            "ACC",
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 4, 10),
            "created"
        ))
        createdBlockId = createdBlock.id

        and:
        def updatedBlock = demandDepositAccountBlockService.updateBlock(new UpdateDemandDepositAccountBlockCommand(
            account.id,
            customerId,
            existingBlock.id,
            LocalDate.of(2026, 3, 12),
            LocalDate.of(2026, 3, 22),
            "updated"
        ))

        and:
        def cancelledBlock = demandDepositAccountBlockService.cancelBlock(new UpdateDemandDepositAccountBlockCommand(
            account.id,
            customerId,
            existingBlock.id,
            LocalDate.of(2026, 3, 12),
            LocalDate.of(2026, 3, 22),
            "cancelled"
        ))

        and:
        TestTransaction.flagForCommit()
        TestTransaction.end()

        then:
        demandDepositAccountBlockRepository.findById(existingBlock.id).orElseThrow().status == AccountBlockStatus.CANCELLED
        1 * blockLifecycleEventPublisher.publishBlockCreated(account.id, { UUID id -> id == createdBlockId })
        1 * blockLifecycleEventPublisher.publishBlockUpdated(account.id, existingBlock.id)
        1 * blockLifecycleEventPublisher.publishBlockCancelled(account.id, existingBlock.id)

        cleanup:
        demandDepositAccountBlockRepository.deleteAll()
        demandDepositAccountRepository.deleteAll()
    }

    def "does not invoke lifecycle hook when operation fails validation or business checks"() {
        given:
        UUID customerId = UUID.randomUUID()
        DemandDepositAccount account = demandDepositAccountRepository.saveAndFlush(
            DemandDepositAccount.create(customerId, DemandDepositAccountStatus.ACTIVE)
        )
        def cancelledBlock = demandDepositAccountBlockRepository.saveAndFlush(
            DemandDepositAccountBlock.create(
                account.id,
                BlockCode.ACC,
                BlockRequestedBy.CUSTOMER,
                AccountBlockStatus.CANCELLED,
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 20),
                "already cancelled"
            )
        )

        when:
        demandDepositAccountBlockService.createBlock(new CreateDemandDepositAccountBlockCommand(
            account.id,
            customerId,
            "ACC",
            LocalDate.of(2026, 4, 11),
            LocalDate.of(2026, 4, 10),
            "invalid date range"
        ))

        then:
        thrown(BlockNotEligibleForOperationException)

        when:
        demandDepositAccountBlockService.updateBlock(new UpdateDemandDepositAccountBlockCommand(
            account.id,
            customerId,
            cancelledBlock.id,
            LocalDate.of(2026, 3, 11),
            LocalDate.of(2026, 3, 21),
            "invalid update"
        ))

        then:
        thrown(BlockNotEligibleForOperationException)

        when:
        demandDepositAccountBlockService.cancelBlock(new UpdateDemandDepositAccountBlockCommand(
            account.id,
            customerId,
            cancelledBlock.id,
            LocalDate.of(2026, 3, 11),
            LocalDate.of(2026, 3, 21),
            "invalid cancel"
        ))

        then:
        thrown(BlockNotEligibleForOperationException)
        0 * blockLifecycleEventPublisher._
    }
}
