# TICKET-004 — Internal DDA Query by Account ID

**Type**: Feature  
**Priority**: High  
**FR**: FR-2  
**Depends on**: TICKET-003 (shares internal response contract and access control)

## Description

Implement (or verify) the `GET /internal/demand-deposit-accounts/{demandDepositAccountId}` endpoint that allows internal service consumers to retrieve a single demand deposit account by its ID.

Aligned to: `DemandDepositAccountInfoController`

## Acceptance Criteria

1. `GET /internal/demand-deposit-accounts/{demandDepositAccountId}` returns `200` with the account detail when found.
2. Response payload matches the internal response contract defined in TICKET-003 (same fields, single-object wrapper).
3. Returns `404` with typed error payload when no account exists for the given ID.
4. Returns `400` with typed error when `demandDepositAccountId` path variable is malformed (e.g., not a valid UUID).
5. Endpoint is restricted to internal callers using the same access control as TICKET-003.
6. Emits observability attributes for `accountId` on the span.

## Technical Notes

- Path variable must be validated as a well-formed UUID before hitting the repository.
- Reuse the same internal response assembler as TICKET-003 to keep contracts in sync.
