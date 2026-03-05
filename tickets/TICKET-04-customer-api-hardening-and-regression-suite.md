# Ticket 04: Customer API Hardening, Documentation Alignment, and Regression Suite

## Objective
Finalize customer-facing DDA contract by aligning endpoint behavior, authorization boundaries, error mapping, and regression coverage against PRD acceptance criteria.

## Scope
- Confirm customer-facing ownership for required mutation endpoints:
  - `POST /demand-deposit-accounts`
  - `POST /demand-deposit-accounts/{accountId}/blocks`
  - `POST /transactions/credit`
  - `POST /transactions/debit`
- Align OpenAPI documentation and examples with implemented contract.
- Standardize error responses for restriction and balance failures.
- Validate auditability guarantees (status changes and transactional events persisted).
- Build end-to-end regression suite for PRD acceptance criteria and success metrics.

## Out of Scope
- Frontend channel implementation.
- Analytics and operational dashboard implementation.

## Functional Requirements Covered
- Non-functional requirements in section 8.
- Success metrics in section 9.
- Acceptance criteria in section 10.
- Open decision outcomes in section 11.

## Acceptance Criteria
- Customer-facing endpoints are exposed and enforce ownership/security constraints.
- Error mapping is consistent across block and balance validation failures.
- Regression tests cover all PRD acceptance scenarios and pass in CI.
- Documentation reflects final behavior and response contracts.
- Evidence shows no duplicate posting for duplicate idempotent transaction requests.

## Dependencies
- Ticket 01, Ticket 02, Ticket 03.

## Test Coverage
- End-to-end integration tests for core customer journeys.
- Contract tests for documented response/error schema.
- Negative path tests for restriction and insufficient-balance scenarios.
