# TICKET-002 — Customer DDA Basic Detail Query

**Type**: Feature  
**Priority**: High  
**FR**: FR-1  
**Depends on**: TICKET-001 (shares auth/customer resolution logic)

## Description

Implement (or verify) the `GET /demand-deposit-accounts/basic` endpoint that returns a lightweight DDA summary for an authenticated customer. This endpoint targets use cases where the full detail response is unnecessary.

Aligned to: `DemandDepositAccountCustomerQueryController`

## Acceptance Criteria

1. `GET /demand-deposit-accounts/basic` returns `200` with minimal DDA detail for the requesting customer.
2. Response payload is a proper subset of the full detail response and includes at minimum:
   - Account ID and account number
   - Account status
   - Current balance
   - Account type
3. `x-customer-id` header is required; request is rejected (`400`) when absent or malformed.
4. Scope check is enforced when configured.
5. Returns `404` with typed error payload when no DDA exists for the customer.
6. Response shape is independently versioned — it must not silently expand to match the full detail endpoint.

## Technical Notes

- Reuse the same customer resolution and scope-check logic as TICKET-001.
- Do not re-fetch restriction records; basic response intentionally omits restriction detail.
- Keep the projection query lightweight: select only the fields included in the basic response.
