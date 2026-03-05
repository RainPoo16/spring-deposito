# TICKET-001 — Customer DDA Full Detail Query

**Type**: Feature  
**Priority**: High  
**FR**: FR-1  
**Depends on**: TICKET-005 (restriction visibility fields in response)

## Description

Implement (or verify) the `GET /demand-deposit-accounts` endpoint that returns full demand deposit account details for an authenticated customer. The response must include all account state fields required by the PRD.

Aligned to: `DemandDepositAccountCustomerQueryController`

## Acceptance Criteria

1. `GET /demand-deposit-accounts` returns `200` with full DDA detail for rows owned by the requesting customer.
2. Response payload includes:
   - Account status
   - Current balance, on-hold balance, spendable/available balance
   - Interest summaries and rates
   - Active blocks (with `debitDisabled` / `creditDisabled` per block)
   - Active earmarks (with earmarked amounts)
   - Account-level `creditDisabled` and `debitDisabled` flags
   - Closure metadata when account is in a closed/closing status
3. `x-customer-id` header is required; request is rejected (`400`) when absent or malformed.
4. Scope check is enforced when `scope-check` is configured for the endpoint.
5. Returns `404` with typed error payload when no DDA exists for the customer.
6. Account hierarchy (main account + child pockets) is represented consistently in the response.

## Technical Notes

- Read from the DDA projection/read model; do not call core banking directly per request.
- Active restrictions must reflect the current effective time window (see TICKET-005).
- Response contract must be stable — adding fields is non-breaking; removing or renaming is a breaking change.
- Emit tracing attributes for `accountId` and `customerId` on the span (see observability NFR).
