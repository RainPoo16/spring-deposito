package com.examples.deposit.controller;

import com.examples.deposit.controller.dto.CreateDemandDepositAccountReq;
import com.examples.deposit.controller.dto.CreateDemandDepositAccountResp;
import com.examples.deposit.controller.dto.CreateDemandDepositAccountBlockReq;
import com.examples.deposit.controller.dto.CreateDemandDepositAccountBlockResp;
import com.examples.deposit.controller.dto.UpdateDemandDepositAccountBlockReq;
import com.examples.deposit.controller.dto.UpdateDemandDepositAccountBlockResp;
import com.examples.deposit.domain.DemandDepositAccount;
import com.examples.deposit.domain.DemandDepositAccountBlock;
import com.examples.deposit.domain.exception.CustomerNotEligibleForAccountCreationException;
import com.examples.deposit.domain.exception.IdempotencyConflictException;
import com.examples.deposit.service.DemandDepositAccountBlockService;
import com.examples.deposit.service.DemandDepositAccountService;
import com.examples.deposit.service.dto.CreateDemandDepositAccountBlockCommand;
import com.examples.deposit.service.dto.UpdateDemandDepositAccountBlockCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DemandDepositAccountController {

    private final DemandDepositAccountService demandDepositAccountService;
    private final DemandDepositAccountBlockService demandDepositAccountBlockService;

    @PostMapping("/demand-deposit-accounts")
    public ResponseEntity<CreateDemandDepositAccountResp> createDemandDepositAccount(
        @RequestHeader("x-customer-id") UUID customerId,
        @RequestBody @Valid CreateDemandDepositAccountReq request
    ) throws CustomerNotEligibleForAccountCreationException, IdempotencyConflictException {

        DemandDepositAccount createdAccount = demandDepositAccountService
            .createMainAccount(customerId, request.idempotencyKey());

        CreateDemandDepositAccountResp response = new CreateDemandDepositAccountResp(
            createdAccount.getId(),
            createdAccount.getCustomerId(),
            createdAccount.getStatus().name()
        );

        return ResponseEntity.status(201)
            .contentType(MediaType.APPLICATION_JSON)
            .body(response);
    }

    @PostMapping("/demand-deposit-accounts/{accountId}/blocks")
    public ResponseEntity<CreateDemandDepositAccountBlockResp> createDemandDepositAccountBlock(
        @RequestHeader("x-customer-id") UUID customerId,
        @PathVariable UUID accountId,
        @RequestBody @Valid CreateDemandDepositAccountBlockReq request
    ) {
        DemandDepositAccountBlock createdBlock = demandDepositAccountBlockService.createBlock(
            new CreateDemandDepositAccountBlockCommand(
                accountId,
                customerId,
                request.blockCode(),
                request.effectiveDate(),
                request.expiryDate(),
                request.remark()
            )
        );

        CreateDemandDepositAccountBlockResp response = new CreateDemandDepositAccountBlockResp(
            createdBlock.getId(),
            createdBlock.getAccountId(),
            createdBlock.getBlockCode().name(),
            createdBlock.getStatus().name(),
            createdBlock.getEffectiveDate(),
            createdBlock.getExpiryDate(),
            createdBlock.getRemark()
        );

        return ResponseEntity.status(201)
            .contentType(MediaType.APPLICATION_JSON)
            .body(response);
    }

    @PutMapping("/demand-deposit-accounts/{accountId}/blocks/{blockId}")
    public ResponseEntity<UpdateDemandDepositAccountBlockResp> updateDemandDepositAccountBlock(
        @RequestHeader("x-customer-id") UUID customerId,
        @PathVariable UUID accountId,
        @PathVariable UUID blockId,
        @RequestBody @Valid UpdateDemandDepositAccountBlockReq request
    ) {
        DemandDepositAccountBlock updatedBlock = demandDepositAccountBlockService.updateBlock(
            new UpdateDemandDepositAccountBlockCommand(
                accountId,
                customerId,
                blockId,
                request.effectiveDate(),
                request.expiryDate(),
                request.remark()
            )
        );

        UpdateDemandDepositAccountBlockResp response = new UpdateDemandDepositAccountBlockResp(
            updatedBlock.getId(),
            updatedBlock.getAccountId(),
            updatedBlock.getBlockCode().name(),
            updatedBlock.getStatus().name(),
            updatedBlock.getEffectiveDate(),
            updatedBlock.getExpiryDate(),
            updatedBlock.getRemark()
        );

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(response);
    }

    @PatchMapping("/demand-deposit-accounts/{accountId}/blocks/{blockId}/cancel")
    public ResponseEntity<Void> cancelDemandDepositAccountBlock(
        @RequestHeader("x-customer-id") UUID customerId,
        @PathVariable UUID accountId,
        @PathVariable UUID blockId
    ) {
        demandDepositAccountBlockService.cancelBlock(
            new UpdateDemandDepositAccountBlockCommand(
                accountId,
                customerId,
                blockId,
                null,
                null,
                null
            )
        );
        return ResponseEntity.noContent().build();
    }
}
