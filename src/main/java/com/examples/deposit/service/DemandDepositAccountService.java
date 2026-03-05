package com.examples.deposit.service;

import com.examples.deposit.domain.DemandDepositAccount;
import com.examples.deposit.domain.DemandDepositAccountLifecycleEvent;
import com.examples.deposit.exception.AccountCreationConflictException;
import com.examples.deposit.exception.AccountLifecycleException;
import com.examples.deposit.exception.AccountNotFoundException;
import com.examples.deposit.repository.DemandDepositAccountLifecycleEventRepository;
import com.examples.deposit.repository.DemandDepositAccountRepository;
import com.examples.deposit.service.dto.CreateDemandDepositAccountCommand;
import com.examples.deposit.service.dto.DemandDepositAccountResult;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class DemandDepositAccountService {

    private static final String IDEMPOTENCY_UNIQUE_CONSTRAINT = "uq_demand_deposit_accounts_customer_idempotency";

    private final DemandDepositAccountRepository demandDepositAccountRepository;
    private final DemandDepositAccountLifecycleEventRepository lifecycleEventRepository;

    public DemandDepositAccountService(
        DemandDepositAccountRepository demandDepositAccountRepository,
        DemandDepositAccountLifecycleEventRepository lifecycleEventRepository
    ) {
        this.demandDepositAccountRepository = demandDepositAccountRepository;
        this.lifecycleEventRepository = lifecycleEventRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public DemandDepositAccountResult createMainAccount(CreateDemandDepositAccountCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        var existing = demandDepositAccountRepository.findByCustomerIdAndIdempotencyKey(
            command.customerId(),
            command.idempotencyKey()
        );
        if (existing.isPresent()) {
            return toResult(existing.get(), true);
        }

        DemandDepositAccount account = DemandDepositAccount.createPending(command.customerId(), command.idempotencyKey());
        DemandDepositAccount persisted;
        try {
            persisted = demandDepositAccountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException ex) {
            if (!isDuplicateIdempotencyViolation(ex)) {
                throw ex;
            }
            return demandDepositAccountRepository
                .findByCustomerIdAndIdempotencyKey(command.customerId(), command.idempotencyKey())
                .map(found -> toResult(found, true))
                .orElseThrow(() -> new AccountCreationConflictException(
                    "Unable to resolve account creation conflict for customerId=" + command.customerId(),
                    ex
                ));
        }

        lifecycleEventRepository.save(DemandDepositAccountLifecycleEvent.accountCreated(persisted));
        return toResult(persisted, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public DemandDepositAccountResult activateAccount(UUID accountId) {
        Objects.requireNonNull(accountId, "accountId must not be null");

        DemandDepositAccount account = demandDepositAccountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException("Demand deposit account not found for accountId=" + accountId));

        try {
            account.activate();
        } catch (IllegalStateException ex) {
            throw new AccountLifecycleException("Invalid account lifecycle transition for accountId=" + accountId, ex);
        }

        DemandDepositAccount persisted = demandDepositAccountRepository.saveAndFlush(account);
        lifecycleEventRepository.save(DemandDepositAccountLifecycleEvent.accountActivated(persisted));
        return toResult(persisted, false);
    }

    private static DemandDepositAccountResult toResult(DemandDepositAccount account, boolean replay) {
        return new DemandDepositAccountResult(
            account.getId(),
            account.getCustomerId(),
            account.getStatus(),
            replay
        );
    }

    private static boolean isDuplicateIdempotencyViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                boolean mentionsConstraint = normalized.contains(IDEMPOTENCY_UNIQUE_CONSTRAINT);
                boolean mentionsUniqueColumns = normalized.contains("customer_id")
                    && normalized.contains("idempotency_key")
                    && (normalized.contains("duplicate") || normalized.contains("unique"));
                if (mentionsConstraint || mentionsUniqueColumns) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }
}
