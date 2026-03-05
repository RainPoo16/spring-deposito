# Plan: Ticket 01 Customer Main DDA Creation API and Lifecycle

**Created:** 2026-03-06
**Status:** Ready for Atlas Execution

## Summary

Implement Ticket 01 as the foundation of the DDA domain by introducing a customer-facing `POST /demand-deposit-accounts` API, idempotent create semantics, and lifecycle transition from `PENDING_VERIFICATION` to `ACTIVE`. Since the current repository is near-greenfield (only `DepositApplication` and context-load test in `src/`), this plan builds the vertical slice end-to-end: schema, domain, repository, service, controller, error handling, and tests. The implementation follows Spring Boot best practices (constructor injection, validation, transactional boundaries, DTO contracts, and Spock slice tests) and repository instruction standards (UUID v7, enum as STRING, ProblemDetail responses, no PII logging).

## Context & Analysis

**Relevant Files:**
- `tickets/TICKET-01-customer-account-creation.md`: Source ticket requirements and acceptance criteria.
- `docs/demand-deposit-account-prd.md`: Domain statuses and FR mapping (`PENDING_VERIFICATION`, `ACTIVE`, FR-AC-1..FR-AC-4).
- `src/main/java/com/examples/deposit/DepositApplication.java`: Current app entry point; no feature code exists yet.
- `src/main/resources/application.properties`: `ddl-auto=validate` + Flyway enabled; migration-first approach required.
- `src/test/groovy/com/examples/deposit/DepositApplicationContextSpec.groovy`: Existing test baseline only.
- `pom.xml`: Existing dependencies support Web, Validation, JPA, Flyway, Spock, UUID Creator.

**Key Functions/Classes (To Introduce):**
- `DemandDepositAccountController#createDemandDepositAccount(...)`: Customer API endpoint orchestration.
- `DemandDepositAccountService#createMainAccount(...)`: Idempotent account creation in `PENDING_VERIFICATION`.
- `DemandDepositAccountService#activateAccount(...)`: One-way lifecycle transition to `ACTIVE` (exactly-once semantics).
- `DemandDepositAccount#activate(...)`: Domain guard for legal state transitions.
- `DemandDepositAccountRepository#findByCustomerIdAndIdempotencyKey(...)`: Idempotency lookup.

**Dependencies:**
- Spring Web + Validation for REST contracts.
- Spring Data JPA + Flyway for persistence and migrations.
- UUID Creator (`com.github.f4b6a3.uuid.alt.GUID`) for UUID v7 IDs.
- Spock + Spring test slices for controller/service/repository verification.

**Patterns & Conventions:**
- Controllers return `ResponseEntity<T>` with explicit content type.
- Error responses use RFC7807 `ProblemDetail` with `application/problem+json`.
- Entities use UUID v7, `@Version`, and `@Enumerated(EnumType.STRING)`.
- Mutating service methods use `@Transactional(rollbackFor = Exception.class)`.
- No PII in logs; only IDs and technical metadata.
- TDD-first with deterministic Spock specs.

## Implementation Phases

### Phase 1: Establish Domain and Persistence Baseline

**Objective:** Define DDA schema, lifecycle enum, and entity model required for creation + activation.

**Files to Modify/Create:**
- `src/main/resources/db/migration/V1__create_demand_deposit_accounts.sql`: Create DDA table, constraints, and indexes.
- `src/main/java/com/examples/deposit/domain/DemandDepositAccountStatus.java`: Status enum including `PENDING_VERIFICATION` and `ACTIVE`.
- `src/main/java/com/examples/deposit/domain/DemandDepositAccount.java`: JPA aggregate with UUID v7 ID, balances, version, lifecycle methods.

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/domain/DemandDepositAccountSpec.groovy`: Factory defaults and transition guard tests.

**Steps:**
1. Write domain spec for creation defaulting to `PENDING_VERIFICATION` and for illegal/valid activation transitions.
2. Run domain spec (should fail because model does not exist).
3. Add enum/entity and migration with enum-as-string columns, optimistic lock column, and idempotency columns.
4. Re-run domain spec (should pass).
5. Run formatting/lint checks configured for project.

**Acceptance Criteria:**
- [ ] Entity creation defaults status to `PENDING_VERIFICATION`.
- [ ] `activate()` transitions exactly once and rejects non-`PENDING_VERIFICATION` states.
- [ ] Migration aligns with JPA mapping and Flyway conventions.
- [ ] Domain tests pass.

---

### Phase 2: Add Repository and Idempotency Storage Guarantees

**Objective:** Introduce repository contracts and DB-level safeguards preventing duplicate create operations.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/repository/DemandDepositAccountRepository.java`: JPA repository with idempotency finder and locking query.
- `src/main/resources/db/migration/V1__create_demand_deposit_accounts.sql`: Add/confirm unique index for `(customer_id, idempotency_key)` and customer/status indexes.

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/repository/DemandDepositAccountRepositorySpec.groovy`: Query correctness + uniqueness constraint behavior.

**Steps:**
1. Write repository slice tests for `findByCustomerIdAndIdempotencyKey` and duplicate-key rejection for same customer.
2. Run repository spec (should fail).
3. Implement repository interface and adjust migration/indexes to satisfy test behavior.
4. Re-run repository spec (should pass).
5. Verify no nondeterministic test data or shared mutable state.

**Acceptance Criteria:**
- [ ] Repository can retrieve prior account by customer + idempotency key.
- [ ] Duplicate account create for same customer/key is blocked at persistence layer.
- [ ] Repository tests pass reliably.

---

### Phase 3: Implement Application Service for Create + Activation Lifecycle

**Objective:** Implement business orchestration with idempotent create, state transition safety, and event persistence hook.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/service/DemandDepositAccountService.java`: Main create/activate business logic.
- `src/main/java/com/examples/deposit/service/dto/CreateDemandDepositAccountCommand.java`: Internal command DTO.
- `src/main/java/com/examples/deposit/service/dto/DemandDepositAccountResult.java`: Service result projection.
- `src/main/java/com/examples/deposit/exception/AccountLifecycleException.java`: Domain/service exceptions.
- `src/main/java/com/examples/deposit/exception/AccountNotFoundException.java`: Not-found exception.
- `src/main/java/com/examples/deposit/exception/AccountCreationConflictException.java`: Concurrency/idempotency conflict handling.

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/service/DemandDepositAccountServiceSpec.groovy`:
  - Create returns existing account for duplicate idempotency key.
  - Create stores `PENDING_VERIFICATION` for first request.
  - Activate transitions to `ACTIVE` once.
  - Repeat activation is a no-op or conflict based on chosen contract.

**Steps:**
1. Write service unit tests for idempotency and lifecycle scenarios.
2. Run service spec (should fail).
3. Implement transactional service methods using constructor injection and repository collaboration.
4. Add event publication persistence hook for FR-AC-4 (e.g., insert account lifecycle event record/outbox abstraction).
5. Re-run service spec (should pass) and confirm no PII in logs.

**Acceptance Criteria:**
- [ ] First create call persists main DDA in `PENDING_VERIFICATION`.
- [ ] Replay with same idempotency key does not create duplicate account.
- [ ] Activation path moves to `ACTIVE` exactly once.
- [ ] Service tests pass.

---

### Phase 4: Expose Customer API Contract and Error Mapping

**Objective:** Deliver the customer endpoint and consistent success/error API structure.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java`: `POST /demand-deposit-accounts` endpoint.
- `src/main/java/com/examples/deposit/controller/dto/CreateDemandDepositAccountReq.java`: Validated request DTO.
- `src/main/java/com/examples/deposit/controller/dto/CreateDemandDepositAccountResp.java`: Response DTO.
- `src/main/java/com/examples/deposit/controller/exception/GlobalExceptionHandler.java`: ProblemDetail mappings.
- `src/main/java/com/examples/deposit/controller/doc/CreateDemandDepositAccountDoc.java` (optional): Reusable OpenAPI meta-annotation.

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/controller/DemandDepositAccountControllerSpec.groovy`:
  - Happy path returns created account with JSON content type.
  - Duplicate idempotency replay returns deterministic payload.
  - Validation and business errors map to problem+json.

**Steps:**
1. Write `@WebMvcTest` contract tests for endpoint response and error shapes.
2. Run controller spec (should fail).
3. Implement controller + request validation + response mapping + exception advice.
4. Add OpenAPI annotations with explicit ProblemDetail schemas for 4xx/5xx.
5. Re-run controller spec (should pass).

**Acceptance Criteria:**
- [ ] `POST /demand-deposit-accounts` exists and is customer-facing.
- [ ] API success and error payloads are consistent and explicit.
- [ ] `ResponseEntity` + content types are explicitly set.
- [ ] Controller tests pass.

---

### Phase 5: Integration Coverage and Regression Baseline for Ticket 01

**Objective:** Validate full flow with persistence and idempotency behavior under real Spring context.

**Files to Modify/Create:**
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountCreationIntegrationSpec.groovy`: End-to-end create + replay + activation path.
- `src/test/groovy/com/examples/deposit/DepositApplicationContextSpec.groovy`: Extend minimally if additional bean coverage is needed.

**Tests to Write:**
- Integration scenario: first create persists one account, second create with same idempotency key returns same account, DB count remains 1.
- Integration scenario: activation criteria simulation transitions to `ACTIVE` once.

**Steps:**
1. Write integration tests for ticket acceptance behavior.
2. Run integration spec (should fail).
3. Wire any missing bean configuration (profiles, transaction boundaries, serialization settings).
4. Re-run integration spec and full relevant test suite.
5. Capture verification evidence for Atlas handoff and CI confidence.

**Acceptance Criteria:**
- [ ] End-to-end behavior satisfies ticket acceptance criteria.
- [ ] No duplicate records for idempotent replay.
- [ ] Activation transition verified as one-time.
- [ ] Integration tests pass in deterministic manner.

## Open Questions

1. What is the activation trigger contract for Ticket 01?
   - **Option A:** Internal service method (`activateAccount`) invoked by future flows/events.
   - **Option B:** Add explicit API endpoint for activation now.
   - **Recommendation:** Option A to keep ticket scope tight while proving lifecycle transition logic with service/integration tests.

2. What HTTP status should idempotent replay return for duplicate create?
   - **Option A:** Always `201 Created` with same resource payload.
   - **Option B:** `200 OK` for replay, `201 Created` for first create.
   - **Recommendation:** Option B for semantic clarity, provided API consumers can handle both statuses.

3. How should FR-AC-4 event publication be implemented in this foundation ticket?
   - **Option A:** Persist lifecycle events in a local outbox table for later publisher integration.
   - **Option B:** Publish Spring in-memory application events only.
   - **Recommendation:** Option A for auditability and reliable downstream delivery readiness.

## Risks & Mitigation

- **Risk:** Activation criteria remain ambiguous and implementation drifts from future ticket expectations.
  - **Mitigation:** Keep activation criteria in a dedicated validator component with explicit TODO contract for Ticket 04 hardening.

- **Risk:** Race conditions create duplicates under concurrent create requests.
  - **Mitigation:** Combine service-level pre-check with DB unique constraint and conflict handling.

- **Risk:** Error response inconsistency across validation/business failures.
  - **Mitigation:** Centralize mappings in `@RestControllerAdvice` and enforce through controller contract tests.

- **Risk:** Scope creep into Ticket 02/03 domain rules.
  - **Mitigation:** Restrict this ticket to main-account create + lifecycle baseline; defer block/transaction enforcement.

## Success Criteria

- [ ] Customer `POST /demand-deposit-accounts` implemented per contract.
- [ ] Main DDA created in `PENDING_VERIFICATION` for eligible customers.
- [ ] Duplicate idempotency replay does not create duplicate account.
- [ ] Lifecycle transition to `ACTIVE` is enforced exactly once.
- [ ] Event publication persistence hook included for FR-AC-4.
- [ ] Controller, service, repository, and integration tests pass.
- [ ] Code follows Spring Boot and repository instruction conventions.

## Notes for Atlas

- Start with migrations and domain model first because `spring.jpa.hibernate.ddl-auto=validate` requires schema correctness before context-based tests pass.
- Keep package names aligned with current root namespace (`com.examples.deposit`) and introduce feature folders incrementally (controller/service/repository/domain).
- Reuse existing dependencies; avoid adding new libraries unless a blocker emerges.
- Prioritize deterministic Spock specs and explicit API contracts so Ticket 02-04 can build on a stable foundation.
