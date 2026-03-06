package com.examples.deposit.service;

import com.examples.deposit.domain.AccountBlockStatus;
import com.examples.deposit.domain.BlockCode;
import com.examples.deposit.domain.BlockRequestedBy;
import com.examples.deposit.domain.DemandDepositAccount;
import com.examples.deposit.domain.DemandDepositAccountBlock;
import com.examples.deposit.domain.exception.AccountNotFoundException;
import com.examples.deposit.domain.exception.BlockNotFoundException;
import com.examples.deposit.domain.exception.BlockNotEligibleForOperationException;
import com.examples.deposit.domain.exception.DuplicateOrOverlappingBlockException;
import com.examples.deposit.repository.DemandDepositAccountBlockRepository;
import com.examples.deposit.repository.DemandDepositAccountRepository;
import com.examples.deposit.service.dto.CreateDemandDepositAccountBlockCommand;
import com.examples.deposit.service.dto.UpdateDemandDepositAccountBlockCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DemandDepositAccountBlockService {

    private final DemandDepositAccountRepository demandDepositAccountRepository;
    private final DemandDepositAccountBlockRepository demandDepositAccountBlockRepository;
    private final BlockLifecycleEventPublisher blockLifecycleEventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public DemandDepositAccountBlock createBlock(CreateDemandDepositAccountBlockCommand command) {
        DemandDepositAccount account = demandDepositAccountRepository
            .findByIdAndCustomerId(command.accountId(), command.customerId())
            .orElseThrow(() -> new AccountNotFoundException(command.accountId()));

        if (command.effectiveDate().isAfter(command.expiryDate())) {
            throw new BlockNotEligibleForOperationException(command.accountId(), "create");
        }

        BlockCode blockCode;
        try {
            blockCode = BlockCode.fromCode(command.blockCode());
        } catch (IllegalArgumentException exception) {
            throw new BlockNotEligibleForOperationException(command.accountId(), "create");
        }

        if (!blockCode.isEligibleForCreateBy(BlockRequestedBy.CUSTOMER)) {
            throw new BlockNotEligibleForOperationException(command.accountId(), "create");
        }

        if (demandDepositAccountBlockRepository.existsOverlappingActiveOrPendingBlock(
            account.getId(),
            blockCode,
            command.effectiveDate(),
            command.expiryDate()
        )) {
            throw new DuplicateOrOverlappingBlockException(account.getId(), blockCode.name());
        }

        DemandDepositAccountBlock block = DemandDepositAccountBlock.create(
            account.getId(),
            blockCode,
            BlockRequestedBy.CUSTOMER,
            AccountBlockStatus.PENDING,
            command.effectiveDate(),
            command.expiryDate(),
            command.remark()
        );
        DemandDepositAccountBlock savedBlock = demandDepositAccountBlockRepository.save(block);
        publishAfterCommit(() -> blockLifecycleEventPublisher.publishBlockCreated(savedBlock.getAccountId(), savedBlock.getId()));
        return savedBlock;
    }

    @Transactional(rollbackFor = Exception.class)
    public DemandDepositAccountBlock updateBlock(UpdateDemandDepositAccountBlockCommand command) {
        DemandDepositAccount account = demandDepositAccountRepository
            .findByIdAndCustomerId(command.accountId(), command.customerId())
            .orElseThrow(() -> new AccountNotFoundException(command.accountId()));

        DemandDepositAccountBlock block = findOwnedBlockForOperation(account.getId(), command.blockId(), "update");
        block.updateDetails(
            BlockRequestedBy.CUSTOMER,
            command.effectiveDate(),
            command.expiryDate(),
            command.remark()
        );
        DemandDepositAccountBlock savedBlock = demandDepositAccountBlockRepository.save(block);
        publishAfterCommit(() -> blockLifecycleEventPublisher.publishBlockUpdated(savedBlock.getAccountId(), savedBlock.getId()));
        return savedBlock;
    }

    @Transactional(rollbackFor = Exception.class)
    public DemandDepositAccountBlock cancelBlock(UpdateDemandDepositAccountBlockCommand command) {
        DemandDepositAccount account = demandDepositAccountRepository
            .findByIdAndCustomerId(command.accountId(), command.customerId())
            .orElseThrow(() -> new AccountNotFoundException(command.accountId()));

        DemandDepositAccountBlock block = findOwnedBlockForOperation(account.getId(), command.blockId(), "cancel");
        block.cancel(BlockRequestedBy.CUSTOMER);
        DemandDepositAccountBlock savedBlock = demandDepositAccountBlockRepository.save(block);
        publishAfterCommit(() -> blockLifecycleEventPublisher.publishBlockCancelled(savedBlock.getAccountId(), savedBlock.getId()));
        return savedBlock;
    }

    private DemandDepositAccountBlock findOwnedBlockForOperation(UUID accountId, UUID blockId, String operation) {
        DemandDepositAccountBlock block = demandDepositAccountBlockRepository.findById(blockId)
            .orElseThrow(() -> new BlockNotFoundException(blockId));
        if (!block.getAccountId().equals(accountId)) {
            throw new BlockNotFoundException(blockId);
        }
        return block;
    }

    private void publishAfterCommit(Runnable publishAction) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publishAction.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishAction.run();
            }
        });
    }
}
