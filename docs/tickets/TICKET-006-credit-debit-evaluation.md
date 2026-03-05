# TICKET-006 — Credit/Debit Eligibility Evaluation

**Type**: Feature  
**Priority**: High  
**FR**: FR-4  
**Depends on**: TICKET-005 (restriction data), TICKET-007 (status-restricted allowlist)

## Description

Implement the credit and debit eligibility evaluation logic for demand deposit accounts. Evaluation must consider account status, account-level flags, active blocks, earmarks, and spendable balance. All rejection outcomes must be deterministic and return typed failure reasons.

## Acceptance Criteria

1. Debit is rejected when **any** of the following is true:
   - Account `debitDisabled = true`
   - Any active block has `debitDisabled = true`
   - Account status is in the debit-prohibited status list
   - Spendable balance (current balance − on-hold − earmarked amounts) is less than the requested debit amount
2. Credit is rejected when **any** of the following is true:
   - Account `creditDisabled = true`
   - Any active block has `creditDisabled = true`
   - Account status is in the credit-prohibited status list
3. Debit/credit for accounts in a status-restricted state (e.g., `PENDING_VERIFICATION`) follows the allowlist rules defined in TICKET-007.
4. Spendable balance is computed as: `currentBalance - onHoldBalance - sum(activeEarmarkAmounts)`.
5. Each rejection returns a **typed failure reason** that identifies the specific rule that caused the rejection (e.g., `DEBIT_BLOCK`, `INSUFFICIENT_SPENDABLE_BALANCE`, `CREDIT_FLAG_DISABLED`).
6. Evaluation is deterministic: same input always produces same outcome.
7. The evaluation logic is unit-testable in isolation (not tied to HTTP layer).

## Business Rules Reference (PRD §8.1)

| Condition | Credit | Debit |
|---|---|---|
| `creditDisabled = true` | Reject | Evaluate other rules |
| `debitDisabled = true` | Evaluate other rules | Reject |
| Active block disables credit | Reject | Evaluate other rules |
| Active block disables debit | Evaluate other rules | Reject |
| Active earmark reduces spendable below amount | Allow | Reject (insufficient spendable) |
| Status is transaction-restricted | Allowlist applies | Allowlist applies |

## Example Scenarios

- Active debit block + sufficient spendable balance → **Reject** (block takes precedence over funds)
- No debit block, earmark reduces spendable below request → **Reject** (`INSUFFICIENT_SPENDABLE_BALANCE`)
- `PENDING_VERIFICATION` + allowed interbank credit code → **Allow**
- `PENDING_VERIFICATION` + disallowed debit type → **Reject** (`TRANSACTION_TYPE_NOT_PERMITTED_FOR_STATUS`)

## Technical Notes

- Implement as a pure domain service / evaluation function with no side effects.
- Evaluation must use the same effective-time-restricted view of blocks/earmarks as TICKET-005.
- Failure reason enum must be exhaustive and stable — new reasons require a version bump if consumed externally.
