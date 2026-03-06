package com.examples.deposit.service;

import com.examples.deposit.domain.AccountTransactionIdempotency;
import com.examples.deposit.domain.DemandDepositAccount;
import com.examples.deposit.domain.DemandDepositAccountTransaction;
import com.examples.deposit.domain.TransactionType;
import com.examples.deposit.domain.TransactionValidationMode;
import com.examples.deposit.domain.exception.AccountNotFoundException;
import com.examples.deposit.domain.exception.TransactionBlockedException;
import com.examples.deposit.domain.exception.TransactionIdempotencyConflictException;
import com.examples.deposit.repository.AccountTransactionIdempotencyRepository;
import com.examples.deposit.repository.DemandDepositAccountBlockRepository;
import com.examples.deposit.repository.DemandDepositAccountRepository;
import com.examples.deposit.repository.DemandDepositAccountTransactionRepository;
import com.examples.deposit.service.dto.PostCreditTransactionCommand;
import com.examples.deposit.service.dto.PostDebitTransactionCommand;
import com.examples.deposit.service.dto.PostedTransactionResult;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static com.examples.deposit.domain.TransactionCodePolicy.resolveValidationMode;

@Service
@RequiredArgsConstructor
public class DemandDepositAccountTransactionService {

    private static final Tracer TRACER =
        GlobalOpenTelemetry.getTracer(DemandDepositAccountTransactionService.class.getName());

    private final DemandDepositAccountRepository demandDepositAccountRepository;
    private final DemandDepositAccountTransactionRepository demandDepositAccountTransactionRepository;
    private final AccountTransactionIdempotencyRepository accountTransactionIdempotencyRepository;
    private final DemandDepositAccountBlockRepository demandDepositAccountBlockRepository;

    @Transactional(rollbackFor = Exception.class)
    public PostedTransactionResult postCredit(PostCreditTransactionCommand command) {
        Span span = startPostingSpan(
            "DemandDepositAccountTransactionService.postCredit",
            command.accountId(),
            command.customerId(),
            TransactionType.CREDIT,
            command.transactionCode(),
            command.idempotencyKey(),
            command.referenceId()
        );

        try (Scope ignored = span.makeCurrent()) {
            DemandDepositAccount account = loadAccountForUpdate(command.accountId(), command.customerId());

            AccountTransactionIdempotency existingIdempotency = findExistingIdempotency(command);
            if (existingIdempotency != null) {
                return resolveReplayResult(
                    existingIdempotency,
                    command.customerId(),
                    command.accountId(),
                    TransactionType.CREDIT,
                    command.amount(),
                    command.transactionCode(),
                    command.idempotencyKey(),
                    command.referenceId()
                );
            }

            if (requiresValidation(command.transactionCode())
                && demandDepositAccountBlockRepository.existsActiveCreditRestrictionOn(account.getId(), LocalDate.now())) {
                throw new TransactionBlockedException(account.getId(), TransactionType.CREDIT, command.transactionCode(), "ACTIVE_CREDIT_RESTRICTION");
            }

            account.applyCredit(command.amount(), command.transactionCode());

            DemandDepositAccountTransaction transaction = demandDepositAccountTransactionRepository.save(
                DemandDepositAccountTransaction.create(
                    account.getId(),
                    account.getCustomerId(),
                    TransactionType.CREDIT,
                    command.amount(),
                    command.transactionCode(),
                    command.referenceId(),
                    command.idempotencyKey(),
                    Instant.now()
                )
            );

            accountTransactionIdempotencyRepository.save(
                AccountTransactionIdempotency.create(
                    account.getCustomerId(),
                    account.getId(),
                    command.idempotencyKey(),
                    command.referenceId(),
                    transaction.getId()
                )
            );

            demandDepositAccountRepository.save(account);
            return toResult(transaction, account);
        } catch (RuntimeException exception) {
            span.setStatus(StatusCode.ERROR);
            span.setAttribute("error.type", exception.getClass().getSimpleName());
            throw exception;
        } finally {
            span.end();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public PostedTransactionResult postDebit(PostDebitTransactionCommand command) {
        Span span = startPostingSpan(
            "DemandDepositAccountTransactionService.postDebit",
            command.accountId(),
            command.customerId(),
            TransactionType.DEBIT,
            command.transactionCode(),
            command.idempotencyKey(),
            command.referenceId()
        );

        try (Scope ignored = span.makeCurrent()) {
            DemandDepositAccount account = loadAccountForUpdate(command.accountId(), command.customerId());

            AccountTransactionIdempotency existingIdempotency = findExistingIdempotency(command);
            if (existingIdempotency != null) {
                return resolveReplayResult(
                    existingIdempotency,
                    command.customerId(),
                    command.accountId(),
                    TransactionType.DEBIT,
                    command.amount(),
                    command.transactionCode(),
                    command.idempotencyKey(),
                    command.referenceId()
                );
            }

            if (requiresValidation(command.transactionCode())
                && demandDepositAccountBlockRepository.existsActiveDebitRestrictionOn(account.getId(), LocalDate.now())) {
                throw new TransactionBlockedException(account.getId(), TransactionType.DEBIT, command.transactionCode(), "ACTIVE_DEBIT_RESTRICTION");
            }

            account.applyDebit(command.amount(), command.transactionCode());

            DemandDepositAccountTransaction transaction = demandDepositAccountTransactionRepository.save(
                DemandDepositAccountTransaction.create(
                    account.getId(),
                    account.getCustomerId(),
                    TransactionType.DEBIT,
                    command.amount(),
                    command.transactionCode(),
                    command.referenceId(),
                    command.idempotencyKey(),
                    Instant.now()
                )
            );

            accountTransactionIdempotencyRepository.save(
                AccountTransactionIdempotency.create(
                    account.getCustomerId(),
                    account.getId(),
                    command.idempotencyKey(),
                    command.referenceId(),
                    transaction.getId()
                )
            );

            demandDepositAccountRepository.save(account);
            return toResult(transaction, account);
        } catch (RuntimeException exception) {
            span.setStatus(StatusCode.ERROR);
            span.setAttribute("error.type", exception.getClass().getSimpleName());
            throw exception;
        } finally {
            span.end();
        }
    }

    private Span startPostingSpan(
        String spanName,
        UUID accountId,
        UUID customerId,
        TransactionType transactionType,
        String transactionCode,
        String idempotencyKey,
        String referenceId
    ) {
        Span span = TRACER.spanBuilder(spanName).startSpan();
        span.setAttribute("deposit.account.id", accountId.toString());
        span.setAttribute("deposit.customer.id", customerId.toString());
        span.setAttribute("deposit.transaction.type", transactionType.name());
        span.setAttribute("deposit.transaction.code", transactionCode);
        span.setAttribute("deposit.idempotency_key.present", idempotencyKey != null && !idempotencyKey.isBlank());
        span.setAttribute("deposit.reference_id.present", referenceId != null && !referenceId.isBlank());
        return span;
    }

    private DemandDepositAccount loadAccountForUpdate(UUID accountId, UUID customerId) {
        return demandDepositAccountRepository.findByIdAndCustomerIdForUpdate(accountId, customerId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    private AccountTransactionIdempotency findExistingIdempotency(PostCreditTransactionCommand command) {
        return accountTransactionIdempotencyRepository.findByCustomerIdAndIdempotencyKeyAndReferenceId(
            command.customerId(),
            command.idempotencyKey(),
            command.referenceId()
        ).orElse(null);
    }

    private AccountTransactionIdempotency findExistingIdempotency(PostDebitTransactionCommand command) {
        return accountTransactionIdempotencyRepository.findByCustomerIdAndIdempotencyKeyAndReferenceId(
            command.customerId(),
            command.idempotencyKey(),
            command.referenceId()
        ).orElse(null);
    }

    private PostedTransactionResult resolveReplayResult(
        AccountTransactionIdempotency existingIdempotency,
        UUID customerId,
        UUID accountId,
        TransactionType transactionType,
        BigDecimal amount,
        String transactionCode,
        String idempotencyKey,
        String referenceId
    ) {
        if (!existingIdempotency.getAccountId().equals(accountId)) {
            throw new TransactionIdempotencyConflictException(customerId, idempotencyKey, referenceId);
        }

        DemandDepositAccountTransaction existingTransaction = demandDepositAccountTransactionRepository
            .findById(existingIdempotency.getTransactionId())
            .orElseThrow(() -> new TransactionIdempotencyConflictException(customerId, idempotencyKey, referenceId));

        if (!existingTransaction.getCustomerId().equals(customerId)
            || !existingTransaction.getAccountId().equals(accountId)
            || existingTransaction.getTransactionType() != transactionType
            || existingTransaction.getAmount().compareTo(amount) != 0
            || !existingTransaction.getTransactionCode().equals(transactionCode)) {
            throw new TransactionIdempotencyConflictException(customerId, idempotencyKey, referenceId);
        }

        DemandDepositAccount replayAccount = demandDepositAccountRepository
            .findByIdAndCustomerIdForUpdate(existingTransaction.getAccountId(), customerId)
            .orElseThrow(() -> new TransactionIdempotencyConflictException(customerId, idempotencyKey, referenceId));
        return toResult(existingTransaction, replayAccount);
    }

    private PostedTransactionResult toResult(DemandDepositAccountTransaction transaction, DemandDepositAccount account) {
        return new PostedTransactionResult(
            transaction.getId(),
            transaction.getAccountId(),
            transaction.getCustomerId(),
            transaction.getTransactionType(),
            transaction.getAmount(),
            transaction.getTransactionCode(),
            transaction.getReferenceId(),
            transaction.getIdempotencyKey(),
            transaction.getPostedAt(),
            account.getCurrentBalance(),
            account.getAvailableBalance()
        );
    }

    private boolean requiresValidation(String transactionCode) {
        return resolveValidationMode(transactionCode) == TransactionValidationMode.REQUIRED;
    }
}
