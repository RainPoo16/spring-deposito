# Plan: Ticket 03 Credit and Debit Transaction Enforcement

**Created:** 2026-03-06
**Status:** Ready for Atlas Execution

## Summary

Implement Ticket 03 as a full transaction-processing vertical slice on top of Ticket 01 and Ticket 02: customer-facing credit/debit endpoints, strict status and block enforcement, sufficient-balance checks for debit, and idempotent posting behavior under replay and concurrency. The implementation should introduce explicit transaction persistence and idempotency records so duplicate requests do not create duplicate postings while preserving auditability. Delivery is split into incremental, test-first phases across domain, repository, service, controller, and integration layers. The plan also hardens concurrency with row locking for balance mutation paths and keeps error responses consistent with existing `ProblemDetail` patterns.

## Context & Analysis

**Relevant Files:**
- `tickets/TICKET-03-credit-debit-transaction-enforcement.md`: Source ticket scope and acceptance criteria for transaction validation and posting.
- `docs/demand-deposit-account-prd.md`: FR-TX-1..FR-TX-6 plus section 6.1/6.2 validation matrix and non-functional row-locking/idempotency requirements.
- `src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java`: Existing customer API style (`x-customer-id`, `ResponseEntity`, explicit JSON content type).
- `src/main/java/com/examples/deposit/service/DemandDepositAccountService.java`: Existing idempotency handling template and transactional orchestration pattern.
- `src/main/java/com/examples/deposit/service/DemandDepositAccountBlockService.java`: Existing block semantics used by transaction restriction checks.
- `src/main/java/com/examples/deposit/domain/DemandDepositAccount.java`: Current account aggregate that must be extended with balances and transaction mutation guards.
- `src/main/java/com/examples/deposit/domain/DemandDepositAccountStatus.java`: Current status enum; must be expanded for TX validation matrix.
- `src/main/java/com/examples/deposit/domain/BlockCode.java`: Existing directional block metadata used to enforce credit/debit restrictions.
- `src/main/java/com/examples/deposit/repository/DemandDepositAccountRepository.java`: Add lock-safe lookup for account mutation.
- `src/main/java/com/examples/deposit/repository/DemandDepositAccountBlockRepository.java`: Add active restriction lookup for credit/debit validation.
- `src/main/java/com/examples/deposit/controller/exception/GlobalExceptionHandler.java`: Extend exception mapping for transaction failures.
- `src/main/java/com/examples/deposit/controller/exception/ApiProblemFactory.java`: Extend problem types for transaction-specific errors.
- `src/main/resources/db/migration/V1__create_demand_deposit_account_tables.sql`: Existing account schema that currently lacks balance columns.
- `src/main/resources/db/migration/V2__create_demand_deposit_account_block_tables.sql`: Existing block schema and overlap index.
- `src/test/groovy/com/examples/deposit/controller/DemandDepositAccountControllerSpec.groovy`: Existing controller slice style for success/error mapping.
- `src/test/groovy/com/examples/deposit/service/DemandDepositAccountServiceSpec.groovy`: Existing idempotency and concurrency test style.
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountBlockIntegrationSpec.groovy`: Existing end-to-end style and problem-detail assertions.

**Key Functions/Classes:**
- `DemandDepositAccountService#createMainAccount(...)` in `src/main/java/com/examples/deposit/service/DemandDepositAccountService.java`: Idempotency template to replicate for transaction posting.
- `DemandDepositAccountBlockService#createBlock(...)` in `src/main/java/com/examples/deposit/service/DemandDepositAccountBlockService.java`: Pattern for ownership-safe account lookup and business exception flow.
- `GlobalExceptionHandler` in `src/main/java/com/examples/deposit/controller/exception/GlobalExceptionHandler.java`: Central error mapping extension point.
- `ApiProblemFactory` in `src/main/java/com/examples/deposit/controller/exception/ApiProblemFactory.java`: Canonical place for `deposit/<problem-type>` definitions.

**Dependencies:**
- `spring-boot-starter-web`: transaction endpoint exposure.
- `spring-boot-starter-validation`: request payload constraints.
- `spring-boot-starter-data-jpa`: transaction persistence and lock-safe account mutation.
- `flyway-core`: schema migration for balances and transaction/idempotency tables.
- `uuid-creator`: UUID v7 generation for transaction/idempotency entities.
- `spock-core`, `spock-spring`, `spring-boot-starter-test`: controller/service/repository/integration tests.

**Patterns & Conventions:**
- `ResponseEntity<T>` with explicit `contentType` for success; `application/problem+json` for errors.
- Service mutations must use `@Transactional(rollbackFor = Exception.class)`.
- UUID v7 IDs for new entities.
- No PII in logs; log IDs and technical metadata only.
- Deterministic Spock tests (`given/when/then`, content type assertions, parallel-safe data).
- Concurrency safety for transaction paths via repository row locking and/or optimistic version checks.

## Implementation Phases

### Phase 1: Transaction Domain Contract and Validation Matrix

**Objective:** Define transaction domain vocabulary and encode credit/debit validation rules before persistence and API wiring.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/domain/DemandDepositAccountStatus.java`: expand status set to include `DORMANT`, `CLOSE_INITIATED`, `CLOSED`, `UNVERIFIED` as needed by FR-TX validations.
- `src/main/java/com/examples/deposit/domain/DemandDepositAccount.java`: add `currentBalance` and `availableBalance` fields plus guarded `applyCredit(...)`/`applyDebit(...)` mutation methods.
- `src/main/java/com/examples/deposit/domain/TransactionType.java`: enum for `CREDIT`, `DEBIT`.
- `src/main/java/com/examples/deposit/domain/TransactionValidationMode.java`: enum/value for whether block/status validation is required by transaction code.
- `src/main/java/com/examples/deposit/domain/TransactionCodePolicy.java`: centralized mapping of transaction code to validation behavior and pending-verification allowance.
- `src/main/java/com/examples/deposit/domain/exception/TransactionBlockedException.java`: active block violation.
- `src/main/java/com/examples/deposit/domain/exception/InsufficientAvailableBalanceException.java`: debit balance violation.
- `src/main/java/com/examples/deposit/domain/exception/TransactionNotAllowedForAccountStatusException.java`: status rule violation.

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/domain/DemandDepositAccountTransactionSpec.groovy`:
  - credit increases current + available balances.
  - debit decreases current + available balances.
  - insufficient available balance throws domain exception.
- `src/test/groovy/com/examples/deposit/domain/TransactionCodePolicySpec.groovy`:
  - transaction code validation-required matrix.
  - pending-verification allowlist behavior.

**Steps:**
1. Write failing domain specs for status, balance, and transaction-code policy matrix.
2. Run domain specs (expected fail).
3. Implement minimal domain fields/enums/policy and exception classes.
4. Run domain specs (expected pass).
5. Refactor only for readability while keeping matrix behavior explicit.

**Acceptance Criteria:**
- [ ] Account aggregate supports balance mutations with invariant checks.
- [ ] Validation-required vs bypass transaction-code behavior is centralized and test-covered.
- [ ] PRD 6.1/6.2 status/balance rules are expressed in tests.
- [ ] Domain specs pass.

---

### Phase 2: Persistence Model, Migration, and Locking Primitives

**Objective:** Add schema and repositories required for durable transaction posting, replay-safe idempotency, and concurrent mutation safety.

**Files to Modify/Create:**
- `src/main/resources/db/migration/V3__add_transaction_posting_and_balance_columns.sql`:
  - add `current_balance` and `available_balance` to `demand_deposit_account` (with deterministic defaults/backfill).
  - create `demand_deposit_account_transaction` table for posted entries.
  - create `account_transaction_idempotency` table with unique constraint on idempotency identity.
  - add indexes for account/time and idempotency lookups.
- `src/main/java/com/examples/deposit/domain/DemandDepositAccountTransaction.java`: transaction entity with account ID, type, amount, transaction code, reference IDs, and timestamps.
- `src/main/java/com/examples/deposit/domain/AccountTransactionIdempotency.java`: idempotency entity linking unique request identity to transaction ID.
- `src/main/java/com/examples/deposit/repository/DemandDepositAccountTransactionRepository.java`: persistence + lookup methods.
- `src/main/java/com/examples/deposit/repository/AccountTransactionIdempotencyRepository.java`: lookup by idempotency identity.
- `src/main/java/com/examples/deposit/repository/DemandDepositAccountRepository.java`: add lock-based account lookup method (e.g., `findByIdAndCustomerIdForUpdate`).
- `src/main/java/com/examples/deposit/repository/DemandDepositAccountBlockRepository.java`: add active restriction existence queries by account and block direction/status/date.

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/repository/DemandDepositAccountRepositorySpec.groovy` additions:
  - balance columns persist and load correctly.
  - lock-query method returns only owned account.
- `src/test/groovy/com/examples/deposit/repository/DemandDepositAccountTransactionRepositorySpec.groovy`:
  - transaction rows persist with enum-string mappings and required fields.
- `src/test/groovy/com/examples/deposit/repository/AccountTransactionIdempotencyRepositorySpec.groovy`:
  - uniqueness and lookup behavior for replay detection.
- `src/test/groovy/com/examples/deposit/repository/DemandDepositAccountBlockRepositorySpec.groovy` additions:
  - active debit/credit restriction queries match direction-based semantics.

**Steps:**
1. Write failing repository slice tests for schema, idempotency uniqueness, and lock/restriction queries.
2. Run repository specs (expected fail).
3. Implement Flyway migration + entities + repository methods.
4. Run repository specs (expected pass).
5. Validate schema remains Ticket 03-scoped (no speculative ledger redesign).

**Acceptance Criteria:**
- [ ] Account schema includes persistent balances.
- [ ] Transaction and transaction-idempotency tables are created with proper uniqueness/indexing.
- [ ] Repository layer supports lock-safe account fetch and active restriction lookup.
- [ ] Repository specs pass.

---

### Phase 3: Credit/Debit Service Orchestration and Idempotent Posting

**Objective:** Implement transaction service that enforces status/block/balance rules and guarantees replay-safe posting.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/service/DemandDepositAccountTransactionService.java`:
  - `postCredit(...)` and `postDebit(...)` operations.
  - lock account row, apply validation matrix, mutate balances, persist transaction + idempotency.
- `src/main/java/com/examples/deposit/service/dto/PostCreditTransactionCommand.java`.
- `src/main/java/com/examples/deposit/service/dto/PostDebitTransactionCommand.java`.
- `src/main/java/com/examples/deposit/service/dto/PostedTransactionResult.java`.
- `src/main/java/com/examples/deposit/service/TransactionPostingEventPublisher.java` and `SpringTransactionPostingEventPublisher.java` (if publishing is in scope this ticket).
- `src/main/java/com/examples/deposit/domain/exception/TransactionIdempotencyConflictException.java` (if existing `IdempotencyConflictException` is too account-creation specific).

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/service/DemandDepositAccountTransactionServiceSpec.groovy`:
  - successful credit updates balances and persists transaction.
  - successful debit updates balances and persists transaction.
  - debit rejected for insufficient available balance with unchanged balances.
  - credit/debit rejected when account status not eligible per matrix.
  - credit/debit rejected when active restriction exists and transaction code requires validation.
  - replay with same idempotency identity returns existing posting without duplicate mutation.
  - concurrent replay requests produce one posted transaction and one balance mutation.

**Steps:**
1. Write failing service tests covering FR-TX-3..FR-TX-6 matrix and concurrency cases.
2. Run service tests (expected fail).
3. Implement minimal transactional orchestration and idempotency fallback behavior.
4. Run service tests (expected pass).
5. Add concise non-PII service logs and after-commit publishing hook if required.

**Acceptance Criteria:**
- [ ] Credit/debit rule enforcement matches PRD section 6.1 and 6.2.
- [ ] Failed validations never mutate balances.
- [ ] Replay-safe idempotency prevents duplicate postings.
- [ ] Service specs pass, including concurrency scenario.

---

### Phase 4: Transaction API Endpoints and ProblemDetail Mapping

**Objective:** Expose customer transaction endpoints with consistent request validation and standardized error semantics.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java` OR `src/main/java/com/examples/deposit/controller/TransactionController.java`:
  - `POST /transactions/credit`
  - `POST /transactions/debit`
- `src/main/java/com/examples/deposit/controller/dto/PostCreditTransactionReq.java`.
- `src/main/java/com/examples/deposit/controller/dto/PostCreditTransactionResp.java`.
- `src/main/java/com/examples/deposit/controller/dto/PostDebitTransactionReq.java`.
- `src/main/java/com/examples/deposit/controller/dto/PostDebitTransactionResp.java`.
- `src/main/java/com/examples/deposit/controller/exception/GlobalExceptionHandler.java`: map status/block/balance/idempotency transaction exceptions.
- `src/main/java/com/examples/deposit/controller/exception/ApiProblemFactory.java`: add transaction-specific problem builders (status not allowed, blocked, insufficient funds, transaction idempotency conflict).

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/controller/DemandDepositAccountControllerSpec.groovy` additions (or `TransactionControllerSpec.groovy` if separate controller):
  - credit/debit happy path response status + JSON fields.
  - validation failures map to 400 + `application/problem+json`.
  - blocked/status/balance failures map to configured 4xx problem details.
  - duplicate idempotency replay does not produce duplicate posting response drift.

**Steps:**
1. Write failing web-slice tests for endpoint contracts and error mapping.
2. Run controller tests (expected fail).
3. Implement controller methods, DTOs, and exception mappings.
4. Run controller tests (expected pass).
5. Ensure explicit success content type and problem+json error content type.

**Acceptance Criteria:**
- [ ] `/transactions/credit` and `/transactions/debit` are implemented and customer-ownership aware.
- [ ] Error mapping is consistent with existing `ProblemDetail` conventions.
- [ ] Controller specs pass.

---

### Phase 5: End-to-End Integration, Replay Safety, and Regression Coverage

**Objective:** Validate Ticket 03 acceptance criteria end-to-end and protect prior-ticket behavior.

**Files to Modify/Create:**
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountTransactionIntegrationSpec.groovy`:
  - full credit/debit paths with status/block/balance/idempotency scenarios.
  - balance invariance on rejected transactions.
  - replay/concurrency safety assertions (single posting).
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountBlockIntegrationSpec.groovy`: add block + transaction interaction regression checks.
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountCreationIntegrationSpec.groovy`: update only if required by status/balance defaults.

**Tests to Write:**
- given active debit-disabled block + validation-required transaction code, debit rejected and balances unchanged.
- given active credit-disabled block + validation-required transaction code, credit rejected and balances unchanged.
- given insufficient available balance, debit rejected and balances unchanged.
- given eligible account and valid transaction, balances mutate correctly.
- given duplicate idempotency key/reference replay, no duplicate posting row and no duplicate balance mutation.

**Steps:**
1. Write failing integration specs for each ticket acceptance criterion.
2. Run integration specs (expected fail).
3. Fill any remaining wiring/config gaps only.
4. Run transaction integration specs (expected pass).
5. Run full suite (`./mvnw test`) and ensure Ticket 01/02 regressions stay green.

**Acceptance Criteria:**
- [ ] All Ticket 03 acceptance scenarios are covered by integration tests.
- [ ] No regression in Ticket 01 and Ticket 02 flows.
- [ ] Full test suite passes.

---

### Phase 6: API Documentation and Observability Finalization

**Objective:** Align endpoint documentation and instrumentation conventions for Ticket 04 readiness.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java` or `src/main/java/com/examples/deposit/controller/TransactionController.java`: add `@Operation`, `@ApiResponse`, and `@Schema(implementation = ProblemDetail.class)` for 4xx/5xx.
- `src/main/java/com/examples/deposit/service/DemandDepositAccountTransactionService.java`: add OpenTelemetry spans around post-credit/post-debit paths with non-PII attributes.
- `README.md` or endpoint docs section if repository currently documents API contracts there.

**Tests to Write:**
- No dedicated unit tests required for annotations.
- Add/adjust existing contract tests only if response shape/status changed.

**Steps:**
1. Add API documentation annotations for both transaction endpoints and error responses.
2. Add span instrumentation at service boundary with safe metadata only.
3. Re-run controller/integration transaction specs to verify no contract regressions.
4. Run full suite (`./mvnw test`).
5. Confirm logs/spans contain no PII.

**Acceptance Criteria:**
- [ ] Transaction endpoints are documented with success and problem-detail responses.
- [ ] Service paths are instrumented with non-PII spans.
- [ ] All tests continue to pass.

## Open Questions

1. What is the canonical transaction idempotency identity?
   - **Option A:** `x-idempotency-key` request header only.
   - **Option B:** Request payload fields (`idempotencyKey` + `referenceId`).
   - **Recommendation:** Option B to stay consistent with current account-creation request style and to make replay identity explicit in persisted transaction records.

2. Which HTTP status should represent insufficient available balance?
   - **Option A:** `422 Unprocessable Entity` (business rule violation, aligns with block rule mapping).
   - **Option B:** `409 Conflict` (state conflict semantics).
   - **Recommendation:** Option A for consistency with existing business-rule responses in Ticket 02.

3. How should pending-verification credit allowance be modeled when transaction code rules are incomplete?
   - **Option A:** Explicit code allowlist in `TransactionCodePolicy` (default deny).
   - **Option B:** Allow all credits for pending accounts unless blocked.
   - **Recommendation:** Option A (default deny) to satisfy PRD rule safely and avoid accidental permissive behavior.

4. Controller placement for transaction endpoints?
   - **Option A:** Add methods to `DemandDepositAccountController`.
   - **Option B:** Introduce dedicated `TransactionController`.
   - **Recommendation:** Option B for clearer responsibility boundaries and easier Ticket 04 contract hardening.

## Risks & Mitigation

- **Risk:** Lost-update race causes duplicate balance mutation under concurrent requests.
  - **Mitigation:** Lock account row during posting (`FOR UPDATE`/pessimistic write) and keep idempotency uniqueness at DB level.

- **Risk:** Ambiguous transaction-code validation rules create inconsistent behavior.
  - **Mitigation:** Encode policy matrix in dedicated tests first, with explicit defaults for unknown codes.

- **Risk:** Schema migration from no-balance to balance columns may break existing tests.
  - **Mitigation:** Use safe defaults in migration and update repository/integration fixtures deterministically.

- **Risk:** Error mapping drift between Ticket 03 and existing Ticket 02 responses.
  - **Mitigation:** Route all new transaction exceptions through `GlobalExceptionHandler` + `ApiProblemFactory` only.

- **Risk:** PII leakage in transaction logs/spans.
  - **Mitigation:** Log only UUIDs/codes/statuses; avoid request body dumps and sensitive identifiers.

## Success Criteria

- [ ] `POST /transactions/credit` and `POST /transactions/debit` implemented and test-covered.
- [ ] Status and block restrictions enforced per FR-TX-3 and FR-TX-4.
- [ ] Debit available-balance checks enforced per FR-TX-5.
- [ ] Idempotent replay behavior enforced per FR-TX-6 with no duplicate posting.
- [ ] Rejected transactions never mutate balances.
- [ ] Ticket 03 integration acceptance scenarios pass.
- [ ] Full `./mvnw test` passes without Ticket 01/02 regressions.

## Notes for Atlas

Implement in strict phase order and do not skip Phase 1 policy tests; they anchor all downstream behavior. Keep changes surgical and ticket-scoped: no ledger redesign, no new transaction types beyond credit/debit, and no unrelated refactoring. Prefer one authoritative transaction service for orchestration, with domain methods enforcing mutation invariants and repository methods handling lock-safe persistence. If an open question remains unresolved during execution, apply the recommendations above and codify the decision in tests so behavior stays explicit and reviewable.
