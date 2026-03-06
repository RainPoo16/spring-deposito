package com.examples.deposit.service;

import com.examples.deposit.domain.AccountCreationIdempotency;
import com.examples.deposit.domain.DemandDepositAccount;
import com.examples.deposit.domain.DemandDepositAccountStatus;
import com.examples.deposit.domain.exception.CustomerNotEligibleForAccountCreationException;
import com.examples.deposit.domain.exception.IdempotencyConflictException;
import com.examples.deposit.repository.AccountCreationIdempotencyRepository;
import com.examples.deposit.repository.DemandDepositAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static com.github.f4b6a3.uuid.alt.GUID.v7;

@Service
@RequiredArgsConstructor
public class DemandDepositAccountService {

    private final DemandDepositAccountRepository demandDepositAccountRepository;
    private final AccountCreationIdempotencyRepository accountCreationIdempotencyRepository;
    private final AccountCreationEligibilityService accountCreationEligibilityService;
    private final AccountLifecycleEventPublisher accountLifecycleEventPublisher;
    private final TransactionTemplate transactionTemplate;

    @Transactional(rollbackFor = Exception.class)
    public DemandDepositAccount activateAccountIfEligible(UUID accountId) {
        DemandDepositAccount account = demandDepositAccountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        if (account.getStatus() != DemandDepositAccountStatus.PENDING_VERIFICATION) {
            return account;
        }

        if (!accountCreationEligibilityService.isEligibleForMainAccountCreation(account.getCustomerId())) {
            return account;
        }

        boolean transitioned = account.activate();
        if (!transitioned) {
            return account;
        }

        DemandDepositAccount activatedAccount = demandDepositAccountRepository.save(account);
        publishAccountActivatedAfterCommit(activatedAccount.getId(), activatedAccount.getCustomerId());
        return activatedAccount;
    }

    private void publishAccountActivatedAfterCommit(UUID accountId, UUID customerId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            accountLifecycleEventPublisher.publishAccountActivated(accountId, customerId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                accountLifecycleEventPublisher.publishAccountActivated(accountId, customerId);
            }
        });
    }

    public DemandDepositAccount createMainAccount(UUID customerId, String idempotencyKey)
        throws CustomerNotEligibleForAccountCreationException, IdempotencyConflictException {

        try {
            return transactionTemplate.execute(status -> {
                try {
                    return createMainAccountInSingleTransaction(customerId, idempotencyKey);
                } catch (CustomerNotEligibleForAccountCreationException | IdempotencyConflictException exception) {
                    throw new CreateMainAccountRuntimeException(exception);
                }
            });
        } catch (DataIntegrityViolationException exception) {
            return loadExistingAccountByIdempotencyKey(customerId, idempotencyKey);
        } catch (CreateMainAccountRuntimeException exception) {
            if (exception.getCause() instanceof CustomerNotEligibleForAccountCreationException customerNotEligible) {
                throw customerNotEligible;
            }
            if (exception.getCause() instanceof IdempotencyConflictException idempotencyConflict) {
                throw idempotencyConflict;
            }
            throw exception;
        }
    }

    private DemandDepositAccount createMainAccountInSingleTransaction(UUID customerId, String idempotencyKey)
        throws CustomerNotEligibleForAccountCreationException, IdempotencyConflictException {

        AccountCreationIdempotency existingIdempotency = accountCreationIdempotencyRepository
            .findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey)
            .orElse(null);
        if (existingIdempotency != null) {
            return loadAccount(existingIdempotency.getAccountId(), customerId, idempotencyKey);
        }

        if (!accountCreationEligibilityService.isEligibleForMainAccountCreation(customerId)) {
            throw new CustomerNotEligibleForAccountCreationException(customerId);
        }

        UUID accountId = v7().toUUID();

        accountCreationIdempotencyRepository.saveAndFlush(
            AccountCreationIdempotency.create(customerId, idempotencyKey, accountId)
        );

        DemandDepositAccount savedAccount = demandDepositAccountRepository.save(
            DemandDepositAccount.createWithId(accountId, customerId, DemandDepositAccountStatus.PENDING_VERIFICATION)
        );

        accountLifecycleEventPublisher.publishAccountCreated(savedAccount.getId(), customerId);
        return savedAccount;
    }

    private DemandDepositAccount loadExistingAccountByIdempotencyKey(UUID customerId, String idempotencyKey)
        throws IdempotencyConflictException {

        AccountCreationIdempotency idempotencyRecord = accountCreationIdempotencyRepository
            .findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey)
            .orElseThrow(() -> new IdempotencyConflictException(customerId, idempotencyKey));
        return loadAccount(idempotencyRecord.getAccountId(), customerId, idempotencyKey);
    }

    private DemandDepositAccount loadAccount(UUID accountId, UUID customerId, String idempotencyKey)
        throws IdempotencyConflictException {

        return demandDepositAccountRepository.findById(accountId)
            .orElseThrow(() -> new IdempotencyConflictException(customerId, idempotencyKey));
    }

    private static final class CreateMainAccountRuntimeException extends RuntimeException {
        private CreateMainAccountRuntimeException(Throwable cause) {
            super(cause);
        }
    }
}
