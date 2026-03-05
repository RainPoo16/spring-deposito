package com.examples.deposit.controller;

import com.examples.deposit.controller.dto.CreateDemandDepositAccountReq;
import com.examples.deposit.controller.dto.CreateDemandDepositAccountResp;
import com.examples.deposit.service.DemandDepositAccountService;
import com.examples.deposit.service.dto.CreateDemandDepositAccountCommand;
import com.examples.deposit.service.dto.DemandDepositAccountResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Demand Deposit Account", description = "API endpoints for customer demand deposit account operations.")
public class DemandDepositAccountController {

    private final DemandDepositAccountService demandDepositAccountService;

    public DemandDepositAccountController(DemandDepositAccountService demandDepositAccountService) {
        this.demandDepositAccountService = demandDepositAccountService;
    }

    @Operation(summary = "Create demand deposit account")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Demand deposit account created",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CreateDemandDepositAccountResp.class))
        ),
        @ApiResponse(
            responseCode = "200",
            description = "Idempotent replay returns existing demand deposit account",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CreateDemandDepositAccountResp.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Account not found",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Account creation conflict",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Business lifecycle violation",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PostMapping(path = "/demand-deposit-accounts", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateDemandDepositAccountResp> createDemandDepositAccount(
        @Parameter(description = "Customer identifier", required = true)
        @RequestHeader("x-customer-id") UUID customerId,
        @Valid @RequestBody CreateDemandDepositAccountReq request
    ) {
        DemandDepositAccountResult result = demandDepositAccountService.createMainAccount(
            new CreateDemandDepositAccountCommand(customerId, request.idempotencyKey())
        );

        HttpStatus status = result.replay() ? HttpStatus.OK : HttpStatus.CREATED;
        CreateDemandDepositAccountResp response = new CreateDemandDepositAccountResp(
            result.accountId(),
            result.customerId(),
            result.status()
        );
        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(response);
    }
}
