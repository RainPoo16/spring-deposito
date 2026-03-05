# Product Requirements Document (PRD): Demand Deposit Account
### Updated: 05/03/2026

## 1. Purpose
Define the functional requirements for Demand Deposit Account (DDA) across:
- Account creation
- Account restrictions (block codes)
- Credit and debit transactions
- Enforcement of transaction restrictions based on account blocks

This document provides a product-level reference for engineering, API consumers, and operations.

## 2. Product Goals
- Provide a reliable and auditable account lifecycle for demand deposit accounts.
- Prevent unauthorized or risky money movement by enforcing block rules on credit/debit transactions.
- Expose clear endpoints for account and transaction operations.

## 3. Scope
### In scope
- Main DDA lifecycle states and transitions
- Block application/update/cancel flows
- Credit/debit transaction validation and posting
- API requirements for customer and admin/internal channels

### Out of scope
- UI/UX design for channels
- Pricing/fees/interest product policy details
- Operational dashboards and analytics implementation

## 4. Domain Summary
A DDA has:
- `currentBalance`
- `availableBalance`
- status (`PENDING_VERIFICATION`, `ACTIVE`, `DORMANT`, `CLOSE_INITIATED`, `CLOSED`, `UNVERIFIED`)
- restrictions:
  - Block codes (can disable credit, debit, or both)

### Key behavior
- `credit` increases current and available balance after validation.
- `debit` decreases current and available balance after validation.
- Active debit-disabled blocks reject debit.
- Active credit-disabled blocks reject credit.

## 5. Functional Requirements

## 5.1 Account Creation
### FR-AC-1
System shall create a main DDA for eligible customers with supported product types.

### FR-AC-2
Creation shall be idempotent to avoid duplicate account setup.

### FR-AC-3
New account shall start as `PENDING_VERIFICATION` and transition to `ACTIVE` after activation criteria are met.

### FR-AC-4
System shall publish account creation/activation events for downstream consumers.

### FR-AC-5
Customer may create child pockets (where enabled) within configured limits.

## 5.2 Block Code Management
### FR-BK-1
System shall allow block creation with:
- block code
- initiator
- effective date
- expiry date
- remark

### FR-BK-2
System shall reject duplicate or overlapping active/pending blocks of the same code.

### FR-BK-3
System shall allow update/cancel only for eligible statuses and initiators.

### FR-BK-4
For main accounts, system may propagate block operations to eligible active/dormant child accounts.

### FR-BK-5
If account is debit-restricted and transaction requires validation, debit shall be rejected.

### FR-BK-6
If account is credit-restricted and transaction requires validation, credit shall be rejected.

## 5.3 Transaction Processing
### FR-TX-1
System shall expose credit transaction execution.

### FR-TX-2
System shall expose debit transaction execution.

### FR-TX-3
System shall perform account status validation before posting.

### FR-TX-4
System shall enforce block restrictions for credit/debit when transaction code requires validation.

### FR-TX-5
System shall enforce sufficient available balance for debit (except specific flows such as debit with fund hold).

### FR-TX-6
System shall enforce idempotency using request/reference identifiers.

## 6. Validation Rules

## 6.1 Credit
Reject credit when:
- account is `CLOSED`
- account is `PENDING_VERIFICATION` and transaction code is not allowed for pending verification
- account has active credit restriction and transaction requires validation

## 6.2 Debit
Reject debit when:
- account status is ineligible (for example `PENDING_VERIFICATION` or `CLOSED`)
- account is `DORMANT` and transaction requires validation
- account has active debit restriction and transaction requires validation
- available balance is insufficient (except configured fund-hold flow)

## 7. Endpoint Requirements

## 7.1 Existing Endpoints in Current Service
### Customer/self-service and internal
- `GET /demand-deposit-accounts`
- `POST /demand-deposit-accounts/{demandDepositAccountId}/pockets`

### Internal transaction commands
- `POST /transactions/credit`
- `POST /transactions/debit`

### Admin restriction endpoints
- `POST /admin/demand-deposit-accounts/{accountId}/blocks`
- `PUT /admin/demand-deposit-accounts/{accountId}/blocks/{blockId}`
- `PATCH /admin/demand-deposit-accounts/{accountId}/blocks/{blockId}/cancel`

## 7.2 Customer Endpoint Requirements (Target Product Contract)
To satisfy customer-facing requirements explicitly:
- `POST /demand-deposit-accounts`
  - Create main DDA for authenticated customer (or trigger account creation workflow)
- `POST /demand-deposit-accounts/{accountId}/blocks`
  - Apply customer-allowed block codes only
- `POST /transactions/credit`
  - Credit with ownership/security and block/status validation
- `POST /transactions/debit`
  - Debit with ownership/security, block/status/balance validation

Note: In current implementation, main DDA creation is primarily event-driven and restriction mutation endpoints are under admin paths. Customer-facing mutation endpoints may require additional controller/profile and authorization work.

## 8. Non-Functional Requirements
- Idempotent transaction and account-creation behavior
- Full auditability via events and persisted status changes
- Consistent error mapping for restriction and balance failures
- Concurrency-safe account mutation with row locking for transaction paths

## 9. Success Metrics
- 100% debit rejection when active debit-disabled block exists and validation is required.
- 100% credit rejection when active credit-disabled block exists and validation is required.
- 0 duplicate overlapping active/pending blocks for same code on same account.
- No duplicate ledger posting for duplicate idempotent requests.

## 10. Acceptance Criteria
- Given account with active debit-disabled block, when debit is submitted, then request is rejected and balances remain unchanged.
- Given account with active credit-disabled block, when credit is submitted, then request is rejected and balances remain unchanged.
- Given duplicate overlapping block request, when apply block is called, then request is rejected.
- Given duplicate transaction idempotency key, when submit again, then no duplicate posting occurs.

## 11. Open Decisions
- Should customer main account creation be direct API-driven, or remain event-driven only?
  - API-driven
- Which block codes are customer-allowed versus admin-only?
  - Customer
- Should customer mutation endpoints be versioned separately from internal/admin APIs?
  - No, all by customer
