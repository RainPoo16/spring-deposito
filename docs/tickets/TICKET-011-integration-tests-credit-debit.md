# TICKET-011 — Integration Tests: Credit/Debit Decision Matrix

**Type**: Test  
**Priority**: High  
**FR**: FR-4  
**AC**: PRD AC-7, AC-8  
**Depends on**: TICKET-006, TICKET-007

## Description

Write integration/unit tests (Spock) that verify the credit and debit eligibility evaluation covers all meaningful combinations from the decision matrix, and that rejected transactions return deterministic, typed failure reasons.

## Test Scenarios

### Account-Level Flags
- [ ] `debitDisabled = true` → debit rejected with `DEBIT_FLAG_DISABLED`
- [ ] `creditDisabled = true` → credit rejected with `CREDIT_FLAG_DISABLED`
- [ ] Both flags false, no blocks → evaluate further rules

### Active Blocks
- [ ] Active block with `debitDisabled = true` → debit rejected with `DEBIT_BLOCK`
- [ ] Active block with `creditDisabled = true` → credit rejected with `CREDIT_BLOCK`
- [ ] Block outside effective window (expired) → block has no effect; not treat as active
- [ ] Multiple blocks: one credit-disabling + one debit-disabling → both credit and debit rejected

### Earmarks and Spendable Balance
- [ ] No earmark, balance sufficient → debit allowed
- [ ] Earmark reduces spendable balance below request amount → debit rejected with `INSUFFICIENT_SPENDABLE_BALANCE`
- [ ] Earmark present but spendable still sufficient → debit allowed
- [ ] Earmark has no effect on credit eligibility → credit allowed when no other restriction

### Status-Based Rules (TICKET-007)
- [ ] `PENDING_VERIFICATION` + credit type/code in allowlist → credit allowed
- [ ] `PENDING_VERIFICATION` + credit type/code not in allowlist → rejected with `TRANSACTION_TYPE_NOT_PERMITTED_FOR_STATUS`
- [ ] `PENDING_VERIFICATION` + debit type/code in allowlist → debit allowed (subject to balance rules)
- [ ] `PENDING_VERIFICATION` + debit type/code not in allowlist → rejected with `TRANSACTION_TYPE_NOT_PERMITTED_FOR_STATUS`

### Combined / Priority Scenarios (PRD §8.2)
- [ ] Active debit block + sufficient spendable balance → **reject** (block takes precedence)
- [ ] Active credit block + otherwise eligible account → **reject** (block takes precedence)
- [ ] No debit block, no disabled flag, earmark reduces spendable below request → **reject** (`INSUFFICIENT_SPENDABLE_BALANCE`)
- [ ] `PENDING_VERIFICATION` + allowed interbank credit code → **allow**
- [ ] `PENDING_VERIFICATION` + disallowed debit type → **reject** (`TRANSACTION_TYPE_NOT_PERMITTED_FOR_STATUS`)

## Acceptance Criteria

1. Each scenario above has a corresponding Spock `where:` table or individual `then:` assertion.
2. Every rejected outcome asserts the **exact typed failure reason** — not just that it was rejected.
3. Evaluation is exercised against real domain objects (not mocked), even if tested at the service/domain level without HTTP.
4. Tests are deterministic: same input always produces same output.

## Technical Notes

- Prefer unit-testing the evaluation domain service directly (not via HTTP) to keep tests fast and isolated.
- Cover the full matrix in a single spec using Spock `@Unroll` / `where:` data tables where inputs vary only in one dimension.
- Verify effective-time window logic: use a fixed clock in tests so time-sensitive restriction evaluations are deterministic.
