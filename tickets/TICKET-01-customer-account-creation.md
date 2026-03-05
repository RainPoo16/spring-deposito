# Ticket 01: Customer Main DDA Creation API and Lifecycle

## Objective
Implement customer-facing main Demand Deposit Account (DDA) creation as API-driven flow with idempotency and lifecycle transition from `PENDING_VERIFICATION` to `ACTIVE`.

## Scope
- Add customer endpoint: `POST /demand-deposit-accounts`.
- Validate customer eligibility and supported product type.
- Enforce idempotency for account creation requests.
- Create main DDA with initial status `PENDING_VERIFICATION`.
- Implement activation transition to `ACTIVE` when criteria are met.
- Publish account creation and activation events for downstream consumers.

## Out of Scope
- Child pocket creation changes (existing endpoint remains).
- UI/UX implementation.
- Pricing, fee, and interest policy logic.

## Functional Requirements Covered
- FR-AC-1
- FR-AC-2
- FR-AC-3
- FR-AC-4

## Acceptance Criteria
- Given an eligible customer, when `POST /demand-deposit-accounts` is called, then a main DDA is created in `PENDING_VERIFICATION`.
- Given duplicate idempotency key/reference, when request is repeated, then no duplicate account is created.
- Given activation criteria are satisfied, when activation flow runs, then account transitions to `ACTIVE` exactly once.
- API returns consistent success and error response structure.

## Dependencies
- None (foundation ticket).

## Test Coverage
- Controller contract tests for create endpoint.
- Service tests for idempotency and lifecycle transitions.
