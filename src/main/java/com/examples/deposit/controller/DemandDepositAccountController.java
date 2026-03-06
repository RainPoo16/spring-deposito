package com.examples.deposit.controller;

import com.examples.deposit.controller.dto.CreateDemandDepositAccountReq;
import com.examples.deposit.controller.dto.CreateDemandDepositAccountResp;
import com.examples.deposit.controller.dto.PostCreditTransactionReq;
import com.examples.deposit.controller.dto.PostCreditTransactionResp;
import com.examples.deposit.controller.dto.PostDebitTransactionReq;
import com.examples.deposit.controller.dto.PostDebitTransactionResp;
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
import com.examples.deposit.service.DemandDepositAccountTransactionService;
import com.examples.deposit.service.dto.CreateDemandDepositAccountBlockCommand;
import com.examples.deposit.service.dto.PostCreditTransactionCommand;
import com.examples.deposit.service.dto.PostDebitTransactionCommand;
import com.examples.deposit.service.dto.PostedTransactionResult;
import com.examples.deposit.service.dto.UpdateDemandDepositAccountBlockCommand;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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
    private final DemandDepositAccountTransactionService demandDepositAccountTransactionService;

    @PostMapping("/demand-deposit-accounts")
        @Operation(summary = "Create a demand deposit account")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "201", description = "Demand deposit account created successfully", content = @Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = CreateDemandDepositAccountResp.class),
                        examples = @ExampleObject(name = "account-created", value = """
                                {
                                    "accountId": "22222222-2222-2222-2222-222222222222",
                                    "customerId": "11111111-1111-1111-1111-111111111111",
                                    "status": "PENDING_VERIFICATION"
                                }
                                """)
                )),
                @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                        mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class),
                        examples = @ExampleObject(name = "validation-failed", value = """
                                {
                                    "type": "deposit/validation-failed",
                                    "title": "Validation failed",
                                    "status": 400,
                                    "detail": "Request validation failed"
                                }
                                """)
                )),
                @ApiResponse(responseCode = "409", description = "Idempotency conflict", content = @Content(
                        mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class),
                        examples = @ExampleObject(name = "idempotency-conflict", value = """
                                {
                                    "type": "deposit/idempotency-conflict",
                                    "title": "Idempotency conflict",
                                    "status": 409,
                                    "detail": "Unable to resolve idempotent account creation request"
                                }
                                """)
                )),
                @ApiResponse(responseCode = "422", description = "Customer not eligible for account creation", content = @Content(
                        mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class),
                        examples = @ExampleObject(name = "customer-not-eligible", value = """
                                {
                                    "type": "deposit/customer-not-eligible",
                                    "title": "Customer not eligible",
                                    "status": 422,
                                    "detail": "Customer is not eligible for account creation"
                                }
                                """)
                )),
                @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(
                        mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class),
                        examples = @ExampleObject(name = "internal-server-error", value = """
                                {
                                    "type": "deposit/internal-server-error",
                                    "title": "Unexpected server error",
                                    "status": 500,
                                    "detail": "An unexpected error occurred"
                                }
                                """)
                ))
        })
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

    @PostMapping("/transactions/credit")
    @Operation(summary = "Post a credit transaction")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Credit transaction posted successfully", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = PostCreditTransactionResp.class),
                        examples = @ExampleObject(name = "credit-posted", value = """
                                {
                                    "transactionId": "41414141-4141-4141-4141-414141414141",
                                    "accountId": "31313131-3131-3131-3131-313131313131",
                                    "customerId": "21212121-2121-2121-2121-212121212121",
                                    "transactionType": "CREDIT",
                                    "amount": 250.50,
                                    "transactionCode": "SAL",
                                    "referenceId": "ref-credit-001",
                                    "idempotencyKey": "idem-credit-001",
                                    "postedAt": "2026-03-06T10:15:30Z",
                                    "currentBalance": 1250.50,
                                    "availableBalance": 1250.50
                                }
                                """)
        )),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class),
                        examples = @ExampleObject(name = "validation-failed", value = """
                                {
                                    "type": "deposit/validation-failed",
                                    "title": "Validation failed",
                                    "status": 400,
                                    "detail": "Request validation failed"
                                }
                                """)
        )),
        @ApiResponse(responseCode = "404", description = "Account not found", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class),
                        examples = @ExampleObject(name = "account-not-found", value = """
                                {
                                    "type": "deposit/account-not-found",
                                    "title": "Account not found",
                                    "status": 404,
                                    "detail": "Demand deposit account was not found"
                                }
                                """)
        )),
        @ApiResponse(responseCode = "409", description = "Transaction idempotency conflict", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class),
                        examples = @ExampleObject(name = "transaction-idempotency-conflict", value = """
                                {
                                    "type": "deposit/transaction-idempotency-conflict",
                                    "title": "Transaction idempotency conflict",
                                    "status": 409,
                                    "detail": "Unable to resolve idempotent transaction request"
                                }
                                """)
        )),
        @ApiResponse(responseCode = "422", description = "Transaction cannot be processed", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class),
                        examples = @ExampleObject(name = "transaction-blocked", value = """
                                {
                                    "type": "deposit/transaction-blocked",
                                    "title": "Transaction blocked",
                                    "status": 422,
                                    "detail": "Transaction is blocked by active account restrictions"
                                }
                                """)
        )),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class),
                        examples = @ExampleObject(name = "internal-server-error", value = """
                                {
                                    "type": "deposit/internal-server-error",
                                    "title": "Unexpected server error",
                                    "status": 500,
                                    "detail": "An unexpected error occurred"
                                }
                                """)
        ))
    })
    public ResponseEntity<PostCreditTransactionResp> postCreditTransaction(
        @RequestHeader("x-customer-id") UUID customerId,
        @RequestBody @Valid PostCreditTransactionReq request
    ) {
        PostedTransactionResult posted = demandDepositAccountTransactionService.postCredit(
            new PostCreditTransactionCommand(
                request.accountId(),
                customerId,
                request.amount(),
                request.transactionCode(),
                request.referenceId(),
                request.idempotencyKey()
            )
        );

        PostCreditTransactionResp response = new PostCreditTransactionResp(
            posted.transactionId(),
            posted.accountId(),
            posted.customerId(),
            posted.transactionType().name(),
            posted.amount(),
            posted.transactionCode(),
            posted.referenceId(),
            posted.idempotencyKey(),
            posted.postedAt(),
            posted.currentBalance(),
            posted.availableBalance()
        );

        return ResponseEntity.status(201)
            .contentType(MediaType.APPLICATION_JSON)
            .body(response);
    }

    @PostMapping("/transactions/debit")
    @Operation(summary = "Post a debit transaction")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Debit transaction posted successfully", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = PostDebitTransactionResp.class),
                        examples = @ExampleObject(name = "debit-posted", value = """
                                {
                                    "transactionId": "cccccccc-dddd-eeee-ffff-000000000000",
                                    "accountId": "77777777-8888-9999-aaaa-bbbbbbbbbbbb",
                                    "customerId": "22222222-3333-4444-5555-666666666666",
                                    "transactionType": "DEBIT",
                                    "amount": 100.00,
                                    "transactionCode": "ATM",
                                    "referenceId": "ref-debit-001",
                                    "idempotencyKey": "idem-debit-001",
                                    "postedAt": "2026-03-06T11:15:30Z",
                                    "currentBalance": 900.00,
                                    "availableBalance": 900.00
                                }
                                """)
        )),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class),
                        examples = @ExampleObject(name = "validation-failed", value = """
                                {
                                    "type": "deposit/validation-failed",
                                    "title": "Validation failed",
                                    "status": 400,
                                    "detail": "Request validation failed"
                                }
                                """)
        )),
        @ApiResponse(responseCode = "404", description = "Account not found", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class),
                        examples = @ExampleObject(name = "account-not-found", value = """
                                {
                                    "type": "deposit/account-not-found",
                                    "title": "Account not found",
                                    "status": 404,
                                    "detail": "Demand deposit account was not found"
                                }
                                """)
        )),
        @ApiResponse(responseCode = "409", description = "Transaction idempotency conflict", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class),
                        examples = @ExampleObject(name = "transaction-idempotency-conflict", value = """
                                {
                                    "type": "deposit/transaction-idempotency-conflict",
                                    "title": "Transaction idempotency conflict",
                                    "status": 409,
                                    "detail": "Unable to resolve idempotent transaction request"
                                }
                                """)
        )),
        @ApiResponse(responseCode = "422", description = "Transaction cannot be processed", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class),
                        examples = @ExampleObject(name = "insufficient-available-balance", value = """
                                {
                                    "type": "deposit/insufficient-available-balance",
                                    "title": "Insufficient available balance",
                                    "status": 422,
                                    "detail": "Available balance is insufficient for the requested debit"
                                }
                                """)
        )),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class),
                        examples = @ExampleObject(name = "internal-server-error", value = """
                                {
                                    "type": "deposit/internal-server-error",
                                    "title": "Unexpected server error",
                                    "status": 500,
                                    "detail": "An unexpected error occurred"
                                }
                                """)
        ))
    })
    public ResponseEntity<PostDebitTransactionResp> postDebitTransaction(
        @RequestHeader("x-customer-id") UUID customerId,
        @RequestBody @Valid PostDebitTransactionReq request
    ) {
        PostedTransactionResult posted = demandDepositAccountTransactionService.postDebit(
            new PostDebitTransactionCommand(
                request.accountId(),
                customerId,
                request.amount(),
                request.transactionCode(),
                request.referenceId(),
                request.idempotencyKey()
            )
        );

        PostDebitTransactionResp response = new PostDebitTransactionResp(
            posted.transactionId(),
            posted.accountId(),
            posted.customerId(),
            posted.transactionType().name(),
            posted.amount(),
            posted.transactionCode(),
            posted.referenceId(),
            posted.idempotencyKey(),
            posted.postedAt(),
            posted.currentBalance(),
            posted.availableBalance()
        );

        return ResponseEntity.status(201)
            .contentType(MediaType.APPLICATION_JSON)
            .body(response);
    }

    @PostMapping("/demand-deposit-accounts/{accountId}/blocks")
        @Operation(summary = "Create an account block")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "201", description = "Account block created successfully", content = @Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = CreateDemandDepositAccountBlockResp.class),
                        examples = @ExampleObject(name = "block-created", value = """
                                {
                                    "blockId": "53535353-5353-5353-5353-535353535353",
                                    "accountId": "20202020-2020-2020-2020-202020202020",
                                    "blockCode": "ACC",
                                    "status": "PENDING",
                                    "effectiveDate": "2026-03-12",
                                    "expiryDate": "2026-03-22",
                                    "remark": "customer request"
                                }
                                """)
                )),
                @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                        mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class),
                        examples = @ExampleObject(name = "validation-failed", value = """
                                {
                                    "type": "deposit/validation-failed",
                                    "title": "Validation failed",
                                    "status": 400,
                                    "detail": "Request validation failed"
                                }
                                """)
                )),
                @ApiResponse(responseCode = "404", description = "Account not found", content = @Content(
                        mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class),
                        examples = @ExampleObject(name = "account-not-found", value = """
                                {
                                    "type": "deposit/account-not-found",
                                    "title": "Account not found",
                                    "status": 404,
                                    "detail": "Demand deposit account was not found"
                                }
                                """)
                )),
                @ApiResponse(responseCode = "422", description = "Block operation not allowed", content = @Content(
                        mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class),
                    examples = {
                        @ExampleObject(name = "duplicate-or-overlapping-block", value = """
                            {
                                "type": "deposit/duplicate-or-overlapping-block",
                                "title": "Duplicate or overlapping block",
                                "status": 422,
                                "detail": "An active or pending block with the same code overlaps the requested period"
                            }
                            """),
                        @ExampleObject(name = "block-not-eligible", value = """
                            {
                                "type": "deposit/block-not-eligible-for-operation",
                                "title": "Block not eligible for operation",
                                "status": 422,
                                "detail": "Block state or initiator is not eligible for the requested operation"
                            }
                            """)
                    }
                )),
                @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(
                        mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class),
                        examples = @ExampleObject(name = "internal-server-error", value = """
                                {
                                    "type": "deposit/internal-server-error",
                                    "title": "Unexpected server error",
                                    "status": 500,
                                    "detail": "An unexpected error occurred"
                                }
                                """)
                ))
        })
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
