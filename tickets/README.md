# Demand Deposit Account PRD Ticket Breakdown

Source PRD: `docs/demand-deposit-account-functions-prd.md`
Date: 2026-03-05

## Ticket List

- [TICKET-001 - Customer DDA Query API Contract](./TICKET-001-customer-dda-query-api-contract.md)
- [TICKET-002 - Customer DDA Read Model Completeness](./TICKET-002-customer-dda-read-model-completeness.md)
- [TICKET-003 - Restriction Visibility (Blocks and Earmarks)](./TICKET-003-restriction-visibility-blocks-earmarks.md)
- [TICKET-004 - Credit and Debit Eligibility Engine](./TICKET-004-credit-debit-eligibility-engine.md)
- [TICKET-005 - Deterministic Rejection Reasons and Error Contract](./TICKET-005-deterministic-rejection-reasons-error-contract.md)

## Traceability Matrix

- FR-1 -> TICKET-001, TICKET-002
- FR-2 (Descoped) -> Not planned in this delivery
- FR-3 -> TICKET-003, TICKET-004
- FR-4 -> TICKET-004, TICKET-005
- FR-5 -> TICKET-001, TICKET-004, TICKET-005
- Acceptance Criteria 4 (OpenAPI) -> TICKET-001, TICKET-005
- Acceptance Criteria 5 (Customer query integration tests) -> TICKET-001, TICKET-002, TICKET-005
- Acceptance Criteria 6 and 7 (Decision matrix and typed failures) -> TICKET-004, TICKET-005

## Suggested Delivery Order

1. TICKET-001
2. TICKET-002
3. TICKET-003
4. TICKET-004
5. TICKET-005

## Notes

- TICKET-003 in this folder is implementation work for restriction visibility.
- FR-2 line item "TICKET-003 is not part of committed implementation scope" in the PRD refers to an older internal tracking item and remains descoped.
- Previous tickets 006-008 were merged into feature tickets to keep a 5-ticket delivery plan.
