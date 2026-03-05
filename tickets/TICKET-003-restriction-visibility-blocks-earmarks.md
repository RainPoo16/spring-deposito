# TICKET-003: Restriction Visibility (Blocks and Earmarks)

## Summary
Expose effective restriction state for blocks and earmarks in DDA read models for downstream transaction decisions.

## PRD Mapping
- FR-3.1
- FR-3.2
- FR-3.3
- FR-3.4

## Scope
- Surface active blocks with effective debit/credit impact flags.
- Surface active earmark data needed to derive unavailable debit amount.
- Ensure effective state is based on current window/status rules.

## Out of Scope
- Introducing new restriction types not in existing domain.
- Historical timeline API for restriction events.

## Acceptance Criteria
- Active block entries contain `debitDisabled` and/or `creditDisabled` indicators when applicable.
- Earmark data is present for spendable-balance derivation.
- Expired/inactive restrictions are excluded from effective state.

## Dependencies
- Restriction projection/domain entities.
- Effective-time evaluation logic.

## Definition of Done
- Read model/retrieval logic returns effective restrictions only.
- API contract includes clear restriction fields for consumers.
- Tests validate active vs inactive window behavior.
