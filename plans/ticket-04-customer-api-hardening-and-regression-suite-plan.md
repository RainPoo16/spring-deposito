# Plan: Ticket 04 Customer API Hardening and Regression Suite

**Created:** 2026-03-06
**Status:** Ready for Atlas Execution

## Summary

Implement Ticket 04 as a hardening and verification pass across the existing customer-facing DDA mutation APIs delivered in Tickets 01-03. The work will align endpoint behavior and OpenAPI documentation, close security/ownership regression gaps, standardize and verify error contracts, and expand end-to-end regression coverage for PRD acceptance criteria. A key risk discovered in analysis is the current `transactionCode` request validation pattern (`^[A-Z]{3}$`) conflicting with domain policy codes (for example `CASH_DEPOSIT`, `FUND_HOLD_DEBIT`), so this plan includes an explicit contract-alignment phase. Delivery is incremental and test-first, with each phase producing verifiable evidence for CI and auditability.

## Context & Analysis

**Relevant Files:**
- `tickets/TICKET-04-customer-api-hardening-and-regression-suite.md`: Source ticket scope and acceptance criteria.
- `docs/demand-deposit-account-prd.md`: PRD acceptance and success metric source for regression matrix.
- `src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java`: All required customer mutation endpoints and partial OpenAPI annotations.
- `src/main/java/com/examples/deposit/controller/exception/GlobalExceptionHandler.java`: Central exception-to-ProblemDetail mappings.
- `src/main/java/com/examples/deposit/controller/exception/ApiProblemFactory.java`: Canonical problem `type/title/detail/status` builders.
- `src/main/java/com/examples/deposit/service/DemandDepositAccountService.java`: Account creation flow and ownership boundary entry point.
- `src/main/java/com/examples/deposit/service/DemandDepositAccountBlockService.java`: Block creation/update/cancel ownership enforcement.
- `src/main/java/com/examples/deposit/service/DemandDepositAccountTransactionService.java`: Credit/debit orchestration, replay logic, and restriction/balance checks.
- `src/main/java/com/examples/deposit/domain/TransactionCodePolicy.java`: Domain transaction-code validation intent.
- `src/main/java/com/examples/deposit/controller/dto/PostCreditTransactionReq.java`: Incoming credit payload validation constraints.
- `src/main/java/com/examples/deposit/controller/dto/PostDebitTransactionReq.java`: Incoming debit payload validation constraints.
- `src/main/resources/db/migration/V1__create_demand_deposit_account_tables.sql`: Base account persistence.
- `src/main/resources/db/migration/V2__create_demand_deposit_account_block_tables.sql`: Block persistence.
- `src/main/resources/db/migration/V3__add_transaction_posting_and_balance_columns.sql`: Transaction and idempotency durability guarantees.
- `src/test/groovy/com/examples/deposit/controller/DemandDepositAccountControllerSpec.groovy`: Current web-slice contract coverage.
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountCreationIntegrationSpec.groovy`: Account creation E2E coverage.
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountBlockIntegrationSpec.groovy`: Block management E2E coverage.
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountTransactionIntegrationSpec.groovy`: Transaction E2E coverage.
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountActivationIntegrationSpec.groovy`: Status transition E2E coverage.

**Key Functions/Classes:**
- `DemandDepositAccountController#createDemandDepositAccount` in `src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java`: Customer account creation endpoint.
- `DemandDepositAccountController#createDemandDepositAccountBlock` in `src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java`: Block creation endpoint.
- `DemandDepositAccountController#postCreditTransaction` in `src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java`: Credit endpoint.
- `DemandDepositAccountController#postDebitTransaction` in `src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java`: Debit endpoint.
- `DemandDepositAccountTransactionService#postCredit` in `src/main/java/com/examples/deposit/service/DemandDepositAccountTransactionService.java`: Credit rules + idempotency logic.
- `DemandDepositAccountTransactionService#postDebit` in `src/main/java/com/examples/deposit/service/DemandDepositAccountTransactionService.java`: Debit rules + idempotency logic.
- `DemandDepositAccountTransactionService#resolveReplayResult` in `src/main/java/com/examples/deposit/service/DemandDepositAccountTransactionService.java`: Idempotency replay/conflict decision point.
- `GlobalExceptionHandler#handleTransactionBlocked` in `src/main/java/com/examples/deposit/controller/exception/GlobalExceptionHandler.java`: Restriction failure mapping.
- `GlobalExceptionHandler#handleInsufficientAvailableBalance` in `src/main/java/com/examples/deposit/controller/exception/GlobalExceptionHandler.java`: Balance failure mapping.
- `ApiProblemFactory#transactionBlocked` in `src/main/java/com/examples/deposit/controller/exception/ApiProblemFactory.java`: 422 blocked problem payload.
- `ApiProblemFactory#insufficientAvailableBalance` in `src/main/java/com/examples/deposit/controller/exception/ApiProblemFactory.java`: 422 insufficient balance payload.

**Dependencies:**
- `spring-boot-starter-web`: REST endpoint behavior and content negotiation.
- `spring-boot-starter-validation`: Header/body validation and 400 mapping.
- `spring-boot-starter-data-jpa`: Ownership-scoped queries, locking, and persistence.
- `flyway-core`: Existing migration chain and any needed schema additions.
- `spock-core`, `spock-spring`, `spring-boot-starter-test`: web-slice, service, repository, and integration regression tests.
- Swagger/OpenAPI annotations currently present via `swagger-annotations-jakarta` in `pom.xml`.

**Patterns & Conventions:**
- Controller responses must use `ResponseEntity<T>` with explicit `MediaType.APPLICATION_JSON` on success.
- Error responses must use `ProblemDetail` with `application/problem+json` and documented `@Schema(implementation = ProblemDetail.class)` for 4xx/5xx.
- Ownership constraints are enforced at repository/service layer through `customerId`-scoped lookups.
- Transaction idempotency must prevent duplicate posting and return stable replay responses.
- PII must never be logged; test and prod assertions should rely on IDs and technical metadata only.
- Spock tests must assert status, content type, and key payload fields deterministically.

## Implementation Phases

### Phase 1: Contract Baseline and Gap Reproduction

**Objective:** Codify currently missing or weakly covered Ticket 04 behaviors as failing tests before changing production code.

**Files to Modify/Create:**
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountTransactionIntegrationSpec.groovy`: add ownership, not-found, and idempotency-conflict E2E cases.
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountBlockIntegrationSpec.groovy`: add foreign-customer ownership boundary case.
- `src/test/groovy/com/examples/deposit/controller/DemandDepositAccountControllerSpec.groovy`: add/strengthen full RFC7807 field assertions for problem responses.

**Tests to Write:**
- `foreign customer cannot post credit/debit to another customer's account`.
- `credit/debit to non-existent account returns 404 account-not-found`.
- `same idempotency identity with changed payload returns 409 transaction-idempotency-conflict` (HTTP-level integration).
- `problem response includes stable type/title/status/detail and application/problem+json` for block and balance failures.

**Steps:**
1. Write failing integration and web-slice tests that represent uncovered Ticket 04 acceptance behaviors.
2. Run targeted specs (should fail).
3. Confirm each failure maps to a concrete implementation/doc gap (no speculative tests).
4. Keep failure evidence for Phase 2-4 implementation validation.

**Acceptance Criteria:**
- [ ] Missing Ticket 04 behaviors are represented as failing tests.
- [ ] Failures are isolated to ownership/contract/documentation gaps, not test setup instability.
- [ ] Baseline is ready for incremental green phases.

---

### Phase 2: OpenAPI and Endpoint Documentation Alignment

**Objective:** Bring all required customer mutation endpoints to a consistent, explicit OpenAPI contract with representative success and error examples.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java`: add/complete `@Operation`, `@ApiResponses`, `@Schema(implementation = ProblemDetail.class)`, and examples for:
  - `POST /demand-deposit-accounts`
  - `POST /demand-deposit-accounts/{accountId}/blocks`
  - `POST /transactions/credit`
  - `POST /transactions/debit`
- Optional if needed for reuse: `src/main/java/com/examples/deposit/controller/swagger/` meta-annotations for repeated error response blocks.

**Tests to Write:**
- Web-slice contract assertions verifying documented response semantics stay aligned with actual status/content-type.
- If OpenAPI endpoint generation exists, add contract test to assert required endpoint paths and response codes are present.

**Steps:**
1. Write/adjust tests for documentation-backed contract invariants.
2. Apply endpoint-level OpenAPI annotations and examples for missing endpoints.
3. Ensure 4xx/5xx responses reference `ProblemDetail` schema consistently.
4. Run web-slice tests and any OpenAPI contract checks.

**Acceptance Criteria:**
- [ ] All four Ticket 04 customer mutation endpoints are documented with explicit operation and responses.
- [ ] Error documentation consistently uses `ProblemDetail` schema for 4xx/5xx.
- [ ] Contract assertions pass without changing endpoint functional behavior.

---

### Phase 3: Transaction Code Validation Contract Fix

**Objective:** Resolve DTO/domain mismatch so customer HTTP API accepts domain-valid transaction codes required by PRD scenarios.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/controller/dto/PostCreditTransactionReq.java`: relax/align `transactionCode` validation pattern.
- `src/main/java/com/examples/deposit/controller/dto/PostDebitTransactionReq.java`: relax/align `transactionCode` validation pattern.
- `src/main/java/com/examples/deposit/domain/TransactionCodePolicy.java`: verify canonical allowed code format and consistency.
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountTransactionIntegrationSpec.groovy`: add HTTP-level tests using policy codes (for example `CASH_DEPOSIT`, `FUND_HOLD_DEBIT`) and expected outcomes.

**Tests to Write:**
- `pending verification account accepts allowlisted credit code via HTTP`.
- `pending verification account rejects non-allowlisted code via HTTP`.
- `bypass-validation transaction codes are accepted through API when intended`.

**Steps:**
1. Write failing HTTP integration tests reproducing current pattern mismatch.
2. Update DTO validation to match policy-driven code model.
3. Confirm domain/service rules remain source of truth for behavioral gating.
4. Run transaction integration and relevant controller tests.

**Acceptance Criteria:**
- [ ] API accepts transaction-code formats required by domain policy.
- [ ] PRD-related policy scenarios are reachable and verified through HTTP tests.
- [ ] No regression in 400-validation behavior for truly malformed codes.

---

### Phase 4: Error Mapping and Ownership Hardening Verification

**Objective:** Ensure ownership/security boundaries and error mappings are consistently enforced across all required mutation endpoints.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/controller/exception/GlobalExceptionHandler.java`: only if mappings need normalization.
- `src/main/java/com/examples/deposit/controller/exception/ApiProblemFactory.java`: only if response detail consistency needs tightening.
- `src/main/java/com/examples/deposit/service/DemandDepositAccountBlockService.java`: only if ownership path behavior differs from expected contract.
- `src/main/java/com/examples/deposit/service/DemandDepositAccountTransactionService.java`: only if ownership/idempotency conflict edge handling needs fixes.
- Tests from Phase 1 as primary verification sources.

**Tests to Write:**
- Cross-endpoint matrix ensuring consistent 404 ownership/not-found semantics.
- Cross-endpoint matrix ensuring 422 mapping for restriction and insufficient balance is stable and precise.
- Verify 409 idempotency conflicts never mutate posting or balances.

**Steps:**
1. Run Phase 1 failing tests and identify smallest code/doc changes needed to satisfy each.
2. Normalize any divergent exception mapping details while preserving existing API contracts.
3. Re-run targeted controller and integration suites.
4. Validate no new exception type leaks as 500 for known business cases.

**Acceptance Criteria:**
- [ ] Ownership boundary behavior is proven at HTTP integration level for block and transaction endpoints.
- [ ] Restriction and insufficient-balance failures return consistent 422 ProblemDetail contracts.
- [ ] Idempotency conflict behavior is deterministic and side-effect free.

---

### Phase 5: Auditability Evidence Expansion

**Objective:** Provide explicit regression evidence for persisted status changes and durable transaction posting artifacts required by Ticket 04.

**Files to Modify/Create:**
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountActivationIntegrationSpec.groovy`: strengthen persisted status transition assertions.
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountBlockIntegrationSpec.groovy`: strengthen persisted block status lifecycle assertions.
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountTransactionIntegrationSpec.groovy`: strengthen posted transaction + idempotency persistence assertions under replay/conflict.
- Optional (only if explicitly required by interpreted acceptance):
  - `src/main/resources/db/migration/V4__create_transaction_event_outbox.sql`
  - event entity/repository/test files for durable transactional event persistence.

**Tests to Write:**
- `status transitions are persisted exactly once for activation flow`.
- `block status lifecycle transitions are persisted and queryable`.
- `replayed transaction request does not create duplicate transaction or idempotency rows`.
- If outbox introduced: `successful transaction persists exactly one event record`; `replay does not duplicate event records`.

**Steps:**
1. Add integration assertions that explicitly prove persistence state before/after mutation scenarios.
2. If current persistence already satisfies ticket wording, stop at evidence tests only.
3. If explicit durable event persistence is required, introduce minimal outbox/event persistence and matching tests.
4. Re-run integration suites.

**Acceptance Criteria:**
- [ ] Test evidence shows persisted status changes and transaction audit artifacts.
- [ ] Duplicate idempotent requests do not produce duplicate persisted postings.
- [ ] Any added event persistence is deterministic and replay-safe.

---

### Phase 6: Comprehensive Regression Suite and CI Gate

**Objective:** Assemble and validate the final Ticket 04 regression matrix against PRD acceptance criteria and success metrics.

**Files to Modify/Create:**
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountRegressionIntegrationSpec.groovy` (or equivalent additions to existing integration specs): chained end-to-end customer journey across create, block, transaction, replay.
- Existing integration specs under `src/test/groovy/com/examples/deposit/integration/` for final matrix completeness.
- `tickets/TICKET-04-customer-api-hardening-and-regression-suite.md` (optional evidence notes update only if requested).

**Tests to Write:**
- Chained journey: create account -> activate -> create block via API -> blocked transaction -> unblock/cancel -> successful posting -> replay same request returns stable response/no duplicates.
- Full negative-path matrix: malformed request, ownership mismatch, account not found, restriction blocked, insufficient balance, idempotency conflict.
- Content-type matrix: `application/json` for success and `application/problem+json` for all error responses.

**Steps:**
1. Build a PRD acceptance checklist mapped to concrete test names.
2. Implement missing regression tests and de-duplicate overlap with existing suite.
3. Run targeted integration specs first.
4. Run full test suite (`./mvnw test`) as final gate.
5. Capture evidence summary for ticket closure.

**Acceptance Criteria:**
- [ ] Regression suite covers all Ticket 04 acceptance criteria and PRD-linked scenarios.
- [ ] All relevant tests pass in CI-equivalent run.
- [ ] Evidence explicitly includes no duplicate posting for idempotent duplicates.

## Open Questions

1. Does "auditability guarantees" in Ticket 04 require durable transactional event persistence (for example outbox table), or are persisted transaction rows + lifecycle state transitions sufficient?
   - **Option A:** Treat existing persisted transaction/idempotency records as sufficient evidence.
   - **Option B:** Add explicit outbox/event persistence for transaction posting and assert it in integration tests.
   - **Recommendation:** Start with Option A evidence tests, escalate to Option B only if product/architecture owner explicitly requires event durability as a deliverable in this ticket.

2. Should customer endpoint exposure be profile-gated (`application-self-service`) for stricter environment control?
   - **Option A:** Keep current always-on controller behavior and focus on ownership tests.
   - **Option B:** Introduce profile gating and corresponding configuration/test updates.
   - **Recommendation:** Use Option A for minimal-risk hardening unless profile gating is an explicit non-functional requirement in Ticket 04 scope.

3. Should 404 remain the ownership-mismatch response to avoid account existence leakage, or should ownership mismatch return 403?
   - **Option A:** Keep 404 (current behavior, privacy-preserving pattern).
   - **Option B:** Switch to 403 for authorization semantics.
   - **Recommendation:** Keep 404 to preserve existing behavior and avoid backward-incompatible contract changes during hardening.

## Risks & Mitigation

- **Risk:** Contract drift between DTO validation and domain policy (already observed with `transactionCode`).
  - **Mitigation:** Lock with HTTP integration tests for policy-driven codes before making validation changes.

- **Risk:** Over-broad changes in exception mapping could regress existing clients.
  - **Mitigation:** Use focused, existing-problem-type-preserving changes and controller/integration contract assertions.

- **Risk:** Regression suite duplication increases maintenance overhead.
  - **Mitigation:** Reuse existing specs where possible and add only missing matrix cases with clear scenario naming.

- **Risk:** Event durability expectation is ambiguous and may expand scope late.
  - **Mitigation:** Resolve Open Question 1 early; if required, isolate outbox/event work to a contained sub-phase.

## Success Criteria

- [ ] Customer-facing mutation endpoints are verified for ownership/security constraints at HTTP integration level.
- [ ] Error mapping for block and balance failures is consistent and contract-tested.
- [ ] OpenAPI documentation is complete and aligned with implemented behavior for all required endpoints.
- [ ] Regression tests cover PRD acceptance scenarios and pass in CI-equivalent runs.
- [ ] Evidence confirms no duplicate posting for duplicate idempotent transaction requests.
- [ ] Ticket 01-03 behaviors remain regression-safe.

## Notes for Atlas

- Prioritize minimal-impact hardening: Ticket 04 should mostly be a verification and alignment pass over existing Ticket 01-03 implementations.
- Execute strictly test-first in each phase and keep production changes tightly scoped to failing acceptance tests.
- Keep PII protection strict in any new logs/assertions.
- If Open Question 1 resolves to durable event persistence required, implement a small, explicit outbox-style persistence path with deterministic integration assertions rather than broad architecture refactor.
- Before closing, produce a compact evidence table mapping each Ticket 04 acceptance criterion to exact passing test names.
