# TICKET-002: Customer DDA Read Model Completeness

## Summary
Ensure customer DDA query response includes complete account visibility fields required by the PRD.

## PRD Mapping
- FR-1.2
- Business Rules (latest persisted state, hierarchy consistency)
- Acceptance Criteria 1 and 2
- Acceptance Criteria 5 (field-level integration verification for customer query)

## Scope
- Include account status and balance fields in response.
- Include interest summaries/rates, active blocks, active earmarks.
- Include account-level flags (`debitDisabled`, `creditDisabled`).
- Include closure metadata when applicable.
- Represent main account and child pockets consistently.
- Add integration assertions that response fields are consistent with persisted fixtures/state.

## Out of Scope
- New product/account types.
- Changes to core banking upstream behavior.

## Acceptance Criteria
- Customer query payload includes all required visibility attributes.
- Fields reflect latest persisted projection/state.
- Account hierarchy shape is stable and deterministic.
- Integration tests assert status and balance visibility fields on happy path.

## Dependencies
- Projection/read model repositories.
- Mapping layer from domain/projection to response DTO.

## Definition of Done
- Response DTO and mapper updates complete.
- Query endpoint returns required fields for known fixtures.
- Test coverage updated for full field visibility with deterministic integration assertions.
