# TICKET-005 — Restriction Visibility in Read Models

**Type**: Feature  
**Priority**: High  
**FR**: FR-3  
**Depends on**: —  
**Blocks**: TICKET-001, TICKET-003, TICKET-006

## Description

Ensure active blocks and earmarks are surfaced correctly in both customer-facing and internal read models. The restriction data must reflect the current effective time window so that both human-facing queries and downstream eligibility evaluation have a consistent view.

## Acceptance Criteria

1. Customer full-detail response (TICKET-001) includes:
   - List of active blocks, each with:
     - Block ID and type
     - `debitDisabled` flag (true/false)
     - `creditDisabled` flag (true/false)
     - Effective start/end window
   - List of active earmarks, each with:
     - Earmark ID and reference
     - Earmarked amount
     - Effective status
2. Internal response (TICKET-003, TICKET-004) includes earmarks with amounts sufficient for consuming services to derive debit-available funds.
3. Only restrictions whose effective window covers the current timestamp are included as "active".
4. Account-level `debitDisabled` and `creditDisabled` flags are included in both read models and reflect the persisted account state.
5. Restriction data is sourced from the same projection used for transaction eligibility (TICKET-006) — there is no divergence between what is displayed and what is evaluated.

## Technical Notes

- Effective window evaluation: a restriction is active when `effectiveFrom <= now < effectiveUntil` (or `effectiveUntil` is null).
- Earmark amounts must not be double-counted if multiple earmarks reference the same underlying hold.
- Changes to restriction state (new block, lifted block) must be reflected through the existing event/projection pipeline — no direct DB writes outside that pipeline.
