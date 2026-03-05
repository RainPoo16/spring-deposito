# TICKET-010 — Integration Tests: Customer & Internal APIs

**Type**: Test  
**Priority**: High  
**FR**: —  
**AC**: PRD AC-6  
**Depends on**: TICKET-001, TICKET-002, TICKET-003, TICKET-004, TICKET-008

## Description

Write integration tests (Spock) covering the happy path and key error cases for all customer-facing and internal DDA query endpoints.

## Test Scenarios

### `GET /demand-deposit-accounts`
- [ ] Returns `200` with full detail for a customer with one DDA
- [ ] Returns `200` with account hierarchy when main account has child pockets
- [ ] Returns `404` when no DDA exists for the customer
- [ ] Returns `400` when `x-customer-id` header is missing
- [ ] Returns `400` when `x-customer-id` header is malformed

### `GET /demand-deposit-accounts/basic`
- [ ] Returns `200` with minimal detail (verify only basic fields are present — full detail fields absent)
- [ ] Returns `404` when no DDA exists for the customer
- [ ] Returns `400` when `x-customer-id` header is missing

### `GET /internal/demand-deposit-accounts`
- [ ] Returns `200` with list of DDAs for a valid customer ID
- [ ] Returns `200` with empty list when customer has no DDAs
- [ ] Returns `400` when `customerId` is absent

### `GET /internal/demand-deposit-accounts/{demandDepositAccountId}`
- [ ] Returns `200` with account detail for a valid account ID
- [ ] Returns `404` for a valid UUID that does not match any account
- [ ] Returns `400` for a malformed (non-UUID) account ID

## Acceptance Criteria

1. All scenarios above have corresponding Spock `@SpringBootTest` or `@WebMvcTest` specs.
2. Tests use deterministic test fixtures — no reliance on ordering or external state.
3. Error response shapes are asserted on `type`, `status`, and absence of PII in the message body.
4. Tests are isolated: each test cleans up or uses a fresh database state.

## Technical Notes

- Follow project Spock testing patterns (see `testing-patterns.instructions.md`).
- Use `@Sql` or Spock `setup`/`cleanup` blocks for database state management.
- Do not mock the repository layer in integration tests; test against a real (in-memory or containerised) database.
