package com.examples.deposit.controller;

import com.examples.deposit.controller.dto.CreateDemandDepositAccountReq;
import com.examples.deposit.controller.dto.CreateDemandDepositAccountResp;
import com.examples.deposit.domain.DemandDepositAccount;
import com.examples.deposit.domain.exception.CustomerNotEligibleForAccountCreationException;
import com.examples.deposit.domain.exception.IdempotencyConflictException;
import com.examples.deposit.service.DemandDepositAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DemandDepositAccountController {

    private final DemandDepositAccountService demandDepositAccountService;

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
}
