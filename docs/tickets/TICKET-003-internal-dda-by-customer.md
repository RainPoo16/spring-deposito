# TICKET-003 — Internal DDA Query by Customer ID

**Type**: Feature  
**Priority**: High  
**FR**: FR-2  
**Depends on**: TICKET-005 (restriction fields exposed in internal response)

## Description

Implement (or verify) the `GET /internal/demand-deposit-accounts` endpoint that allows internal service consumers to retrieve all demand deposit accounts for a given customer. This endpoint supports downstream orchestration flows.

Aligned to: `DemandDepositAccountInfoController`

## Acceptance Criteria

1. `GET /internal/demand-deposit-accounts?customerId={customerId}` (or equivalent query param) returns `200` with a list of DDAs for the customer.
2. Internal response payload includes at minimum:
   - Account ID and account number
   - Account status
   - Current balance and on-hold balance
   - Active earmarks (with amounts)
   - Account type
3. Endpoint is restricted to internal callers; accessible only under the appropriate Spring profile or security control (not exposed on the customer-facing route).
4. Returns an empty list (not `404`) when no DDAs exist for the customer.
5. Returns `400` with typed error when `customerId` is absent or malformed.
6. Emits observability attributes for `customerId` on the span.

## Technical Notes

- Do not reuse the customer-facing response model; internal response has its own contract.
- Downstream consumers depend on earmark amounts to derive debit-available funds — ensure earmark data is always present and accurate.
- Internal endpoint access must be validated; do not rely solely on network isolation.
