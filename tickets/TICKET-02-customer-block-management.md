# Ticket 02: Customer Block Code Management and Restriction Rules

## Objective
Implement customer block management for DDA and enforce block overlap and lifecycle rules for restriction creation, update, and cancellation.

## Scope
- Add customer endpoint: `POST /demand-deposit-accounts/{accountId}/blocks`.
- Define and enforce customer-allowed block codes.
- Validate block payload fields: block code, initiator, effective date, expiry date, remark.
- Reject duplicate or overlapping active/pending blocks of same code on same account.
- Implement update and cancel flows for eligible statuses and initiators.
- For main accounts, propagate block operations to eligible active/dormant child accounts where required.

## Out of Scope
- Admin-only block APIs redesign.
- New block code catalog governance process.

## Functional Requirements Covered
- FR-BK-1
- FR-BK-2
- FR-BK-3
- FR-BK-4

## Acceptance Criteria
- Given valid request, when block is created, then block is stored with expected status and dates.
- Given duplicate/overlapping block of same code, when create is called, then request is rejected.
- Given ineligible status or initiator, when update/cancel is requested, then request is rejected.
- Given main account with eligible children, when block create/update/cancel occurs, then propagation behavior matches rule.
- API responses map validation and business-rule failures consistently.

## Dependencies
- Ticket 01 for customer account creation baseline.

## Test Coverage
- Service tests for overlap checks and eligibility rules.
- Integration tests for block propagation behavior.
- API tests for create/update/cancel validation errors.
