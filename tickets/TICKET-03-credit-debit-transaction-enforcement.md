# Ticket 03: Credit and Debit Transaction Enforcement (Status, Blocks, Balance, Idempotency)

## Objective
Implement complete credit/debit validation and posting behavior with strict enforcement of account status, block restrictions, sufficient balance, and idempotency.

## Scope
- Implement or finalize transaction command endpoints:
  - `POST /transactions/credit`
  - `POST /transactions/debit`
- Validate account status before posting.
- Enforce block-based restrictions when transaction code requires validation.
- Enforce debit sufficient `availableBalance` checks (excluding configured fund-hold flow).
- Apply credit/debit mutations to `currentBalance` and `availableBalance` after validation.
- Enforce idempotency using request/reference identifiers to prevent duplicate posting.

## Out of Scope
- Ledger redesign or accounting engine replacement.
- New transaction types outside credit/debit contract.

## Functional Requirements Covered
- FR-TX-1
- FR-TX-2
- FR-TX-3
- FR-TX-4
- FR-TX-5
- FR-TX-6
- Validation rules in sections 6.1 and 6.2

## Acceptance Criteria
- Given active debit-disabled block and validation-required transaction code, when debit is requested, then request is rejected and balances remain unchanged.
- Given active credit-disabled block and validation-required transaction code, when credit is requested, then request is rejected and balances remain unchanged.
- Given insufficient available balance for debit, when debit is requested, then request is rejected.
- Given eligible account and valid request, when credit/debit succeeds, then balances are updated correctly.
- Given duplicate idempotency key/reference, when request is replayed, then no duplicate posting is created.

## Dependencies
- Ticket 02 for block enforcement correctness.

## Test Coverage
- Transaction service tests for status and block validation matrix.
- Repository/integration tests for row locking and concurrent mutation safety.
- Idempotency replay tests for both credit and debit endpoints.
