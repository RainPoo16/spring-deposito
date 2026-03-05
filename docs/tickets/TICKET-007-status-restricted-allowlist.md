# TICKET-007 — Status-Restricted Transaction Allowlist

**Type**: Feature  
**Priority**: Medium  
**FR**: FR-4 (item 5)  
**Depends on**: TICKET-006 (plugs into the eligibility evaluation)  
**Blocks**: TICKET-006 (must be defined before evaluation is complete)

## Description

Define and enforce the transaction type / transaction code allowlist for accounts in a status-restricted state (e.g., `PENDING_VERIFICATION`). When an account is in such a state, only explicitly permitted transaction type + code combinations may proceed; all others are rejected with a typed failure reason.

## Acceptance Criteria

1. A configurable allowlist maps each restricted status to its permitted `(transactionType, transactionCode)` pairs for credit and debit separately.
2. For a `PENDING_VERIFICATION` account:
   - A credit request with a transaction type/code in the credit allowlist is **allowed**.
   - A credit request with a type/code **not** in the allowlist is **rejected** with reason `TRANSACTION_TYPE_NOT_PERMITTED_FOR_STATUS`.
   - A debit request with a type/code in the debit allowlist is **allowed** (subject to other rules in TICKET-006).
   - A debit request with a type/code **not** in the allowlist is **rejected** with reason `TRANSACTION_TYPE_NOT_PERMITTED_FOR_STATUS`.
3. Statuses not in the restricted list are unaffected by this allowlist logic.
4. Allowlist configuration is externalisable (e.g., properties or a config class) — not hardcoded inline.
5. Adding a new restricted status or new permitted pair does not require logic changes, only config changes.

## Technical Notes

- The allowlist is a domain policy decision, not a per-request input. It must not be controllable by the API caller.
- Keep allowlist configuration close to the domain (not in the HTTP layer).
- Ensure the allowlist check runs after status-based hard rejection (e.g., a fully closed account is rejected before even checking the allowlist).
