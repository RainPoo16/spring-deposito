# TICKET-004: Credit and Debit Eligibility Engine

## Summary
Implement deterministic eligibility evaluation for credit/debit requests using status, account flags, active blocks, and earmarks.

## PRD Mapping
- FR-4.1 to FR-4.5
- Business Rules (debit and credit interaction)
- Section 8.1 Interaction Matrix
- Section 8.2 Example Transaction Scenarios
- Acceptance Criteria 6

## Scope
- Evaluate eligibility from account status + account-level flags + active blocks.
- Implement spendable balance check using current balance minus holds/earmarks.
- Enforce restricted-status allowlists (for example `PENDING_VERIFICATION`) by transaction type/code.
- Apply consistent effective-time snapshot for block and earmark checks.
- Add matrix-based integration tests for status, flags, block impacts, and earmark combinations.

## Out of Scope
- New transaction products or external pricing logic.
- Upstream core banking policy redesign.

## Acceptance Criteria
- Debit rejected when debit-disabling status/flag/block applies.
- Credit rejected when credit-disabling status/flag/block applies.
- Debit rejected when spendable balance is insufficient.
- Restricted statuses accept/reject by explicit allowlist rules.
- Tests cover PRD scenario matrix combinations and earmark-driven insufficient spendable balance.

## Dependencies
- Restriction visibility outputs from TICKET-003.
- Existing transaction orchestration path.

## Definition of Done
- Rule evaluation service implemented with clear precedence.
- Unit/integration coverage added for matrix combinations and PRD section 8.2 scenarios.
- Rejection path integrates with typed failure responses (TICKET-005).
