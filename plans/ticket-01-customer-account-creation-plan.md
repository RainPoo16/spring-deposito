# Plan: Ticket 01 Customer Main DDA Creation API and Lifecycle

**Created:** 2026-03-06
**Status:** Ready for Atlas Execution

## Summary

Implement Ticket 01 as the first vertical slice of the DDA domain: persistent main account model, customer-facing create API, idempotent request handling, and one-time activation transition from `PENDING_VERIFICATION` to `ACTIVE`. The implementation should follow Spring Boot best practices already mandated in this repository: constructor injection, DTO-based API contracts, service-layer transaction boundaries, Flyway-first schema management, and Spock test slices. The delivery is split into incremental phases so each phase introduces behavior with failing tests first, then minimum code, then refactor. This plan intentionally leaves child pockets, block management, and transaction posting to later tickets.

## Context & Analysis

**Relevant Files:**
- `tickets/TICKET-01-customer-account-creation.md`: Source requirements and acceptance criteria for this ticket.
- `docs/demand-deposit-account-prd.md`: Functional requirements FR-AC-1 to FR-AC-4 and status lifecycle definitions.
- `pom.xml`: Current dependencies and test stack (Spring Boot 3.5.x, Spock, Flyway, JPA, validation).
- `src/main/resources/application.properties`: Existing baseline config; must remain compatible with Flyway + JPA validate mode.
- `src/main/java/com/examples/deposit/DepositApplication.java`: Application root package anchor for new modules.
- `src/test/groovy/com/examples/deposit/DepositApplicationContextSpec.groovy`: Existing baseline context test; keep passing.

**Key Functions/Classes (to introduce):**
- `DemandDepositAccount` in `src/main/java/com/examples/deposit/domain/DemandDepositAccount.java`: JPA aggregate root with lifecycle methods.
- `DemandDepositAccountStatus` in `src/main/java/com/examples/deposit/domain/DemandDepositAccountStatus.java`: Enum containing `PENDING_VERIFICATION`, `ACTIVE`, and future statuses from PRD.
- `DemandDepositAccountRepository` in `src/main/java/com/examples/deposit/repository/DemandDepositAccountRepository.java`: Persistence and idempotency lookups.
- `AccountCreationIdempotency` in `src/main/java/com/examples/deposit/domain/AccountCreationIdempotency.java`: Request-key ledger for duplicate suppression.
- `DemandDepositAccountService` in `src/main/java/com/examples/deposit/service/DemandDepositAccountService.java`: Transactional orchestration for create and activate flows.
- `DemandDepositAccountController` in `src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java`: `POST /demand-deposit-accounts` contract.
- `GlobalExceptionHandler` in `src/main/java/com/examples/deposit/controller/exception/GlobalExceptionHandler.java`: Consistent API error structure.

**Dependencies:**
- `spring-boot-starter-web`: REST API endpoint and `ResponseEntity` contract.
- `spring-boot-starter-validation`: request DTO validation with `@Valid`.
- `spring-boot-starter-data-jpa`: aggregate persistence and transaction boundaries.
- `flyway-core`: schema migration for account and idempotency tables.
- `com.github.f4b6a3:uuid-creator`: UUID v7 generation for account IDs.
- `spring-boot-starter-test`, `spock-core`, `spock-spring`: controller slice, service, and repository tests.

**Patterns & Conventions:**
- Controller methods return `ResponseEntity<T>` with explicit `contentType`.
- Keep business orchestration in services (`@Service`) and mutations in `@Transactional(rollbackFor = Exception.class)` methods.
- Use `@Enumerated(EnumType.STRING)` for all persisted enums.
- Use UUID v7 for entity IDs, not `UUID.randomUUID()`.
- Never log PII (customer names, email, account numbers); log IDs and technical metadata only.
- Spock tests use deterministic data and `given/when/then` structure.

## Implementation Phases

### Phase 1: Baseline Domain and Database Schema

**Objective:** Establish persistent account model and lifecycle-safe schema before exposing API behavior.

**Files to Modify/Create:**
- `src/main/resources/db/migration/V1__create_demand_deposit_account_tables.sql`: Create `demand_deposit_account` and `account_creation_idempotency` tables with uniqueness constraints.
- `src/main/java/com/examples/deposit/domain/DemandDepositAccount.java`: JPA entity with UUID v7 ID, status, customer ID, optimistic locking field.
- `src/main/java/com/examples/deposit/domain/DemandDepositAccountStatus.java`: String-mapped status enum.
- `src/main/java/com/examples/deposit/domain/AccountCreationIdempotency.java`: JPA entity for idempotency key tracking.
- `src/main/java/com/examples/deposit/repository/DemandDepositAccountRepository.java`: Repository interfaces for account lookup.
- `src/main/java/com/examples/deposit/repository/AccountCreationIdempotencyRepository.java`: Repository with `findByCustomerIdAndIdempotencyKey`.

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/repository/DemandDepositAccountRepositorySpec.groovy`: persistence and enum mapping.
- `src/test/groovy/com/examples/deposit/repository/AccountCreationIdempotencyRepositorySpec.groovy`: unique key behavior.

**Steps:**
1. Write repository specs for schema bootstrap, enum persistence, and unique idempotency constraints.
2. Run repository specs (expected fail due to missing schema/entities).
3. Add Flyway migration and JPA entities/repositories with minimum fields required by Ticket 01.
4. Run repository specs again (expected pass).
5. Refactor naming/constraints only if needed; keep schema minimal for this ticket.

**Acceptance Criteria:**
- [ ] Database starts with Flyway migration applied successfully.
- [ ] `DemandDepositAccountStatus` stored as string values.
- [ ] Duplicate `(customer_id, idempotency_key)` persistence is prevented by database-level uniqueness.
- [ ] Repository specs pass.

---

### Phase 2: Account Creation Service with Idempotency

**Objective:** Implement deterministic account creation orchestration that always creates one main DDA per unique idempotency key.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/service/DemandDepositAccountService.java`: `createMainAccount(...)` and transactional idempotency flow.
- `src/main/java/com/examples/deposit/service/AccountCreationEligibilityService.java`: eligibility abstraction with initial implementation.
- `src/main/java/com/examples/deposit/service/AccountLifecycleEventPublisher.java`: abstraction for FR-AC-4 event publishing.
- `src/main/java/com/examples/deposit/service/SpringAccountLifecycleEventPublisher.java`: initial Spring event adapter implementation.
- `src/main/java/com/examples/deposit/domain/exception/*.java`: business exceptions (ineligible customer, duplicate key conflict handling path).

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/service/DemandDepositAccountServiceSpec.groovy`:
  - creates account in `PENDING_VERIFICATION` for eligible customer.
  - returns same existing account for replayed idempotency key.
  - does not create duplicate account rows on replay.
  - emits account-created lifecycle event once.

**Steps:**
1. Write service specs with mocked collaborators (eligibility + event publisher) and real repositories where appropriate.
2. Run service specs (expected fail due to missing service logic).
3. Implement minimal transactional service logic: eligibility check, idempotency lookup, create-and-save, idempotency record insert, event publish.
4. Run service specs (expected pass) and adjust only for deterministic behavior.
5. Add concise non-PII logs with account/customer UUIDs and correlation metadata.

**Acceptance Criteria:**
- [ ] Eligible customer creation returns an account with `PENDING_VERIFICATION`.
- [ ] Replayed idempotency request returns same account ID without duplicate insert.
- [ ] Account creation event is published exactly once for first successful create.
- [ ] Service specs pass.

---

### Phase 3: Customer Create Account API Contract

**Objective:** Expose `POST /demand-deposit-accounts` with stable request/response contract and consistent error mapping.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java`: customer-facing create endpoint.
- `src/main/java/com/examples/deposit/controller/dto/CreateDemandDepositAccountReq.java`: request DTO (`@Valid`).
- `src/main/java/com/examples/deposit/controller/dto/CreateDemandDepositAccountResp.java`: success DTO.
- `src/main/java/com/examples/deposit/controller/exception/GlobalExceptionHandler.java`: map domain + validation errors to RFC7807-style responses.
- `src/main/java/com/examples/deposit/controller/exception/ApiProblemFactory.java`: reusable problem response construction.

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/controller/DemandDepositAccountControllerSpec.groovy` using `@WebMvcTest`:
  - returns 201 + JSON body for successful creation.
  - validates missing/invalid idempotency key payload.
  - maps ineligible customer to expected 4xx problem response.
  - replay call returns success without duplicate semantics leaking.

**Steps:**
1. Write controller slice specs for success and failure paths, including content type assertions.
2. Run controller specs (expected fail due to absent controller/advice).
3. Implement controller + DTO + exception advice with explicit `ResponseEntity` and content types.
4. Run controller specs (expected pass).
5. Ensure endpoint naming and request semantics match ticket + PRD language.

**Acceptance Criteria:**
- [ ] `POST /demand-deposit-accounts` exists and returns consistent JSON/problemdetail shapes.
- [ ] Validation and business-rule failures are mapped consistently.
- [ ] Controller specs pass.

---

### Phase 4: Activation Transition and Exactly-Once Semantics

**Objective:** Implement lifecycle transition from `PENDING_VERIFICATION` to `ACTIVE` with one-way, exactly-once behavior when activation criteria are satisfied.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/service/DemandDepositAccountService.java`: `activateAccountIfEligible(UUID accountId, ...)` logic.
- `src/main/java/com/examples/deposit/domain/DemandDepositAccount.java`: domain method `activate()` with guard clauses.
- `src/main/java/com/examples/deposit/domain/exception/InvalidAccountLifecycleTransitionException.java`: illegal transition handling.
- `src/test/groovy/com/examples/deposit/service/DemandDepositAccountActivationSpec.groovy`: lifecycle transition tests.

**Tests to Write:**
- activation succeeds once when criteria evaluator returns true.
- repeated activation attempt is idempotent (no second status change event).
- activation denied when criteria evaluator fails.
- non-pending account activation path is safely handled.

**Steps:**
1. Write failing service/domain specs for transition and one-time event publishing.
2. Run specs (expected fail).
3. Implement minimal domain + service transition logic and persistence updates.
4. Run specs (expected pass).
5. Refactor to keep transition rules inside domain entity and orchestration in service.

**Acceptance Criteria:**
- [ ] Account transitions from `PENDING_VERIFICATION` to `ACTIVE` exactly once.
- [ ] Duplicate activation attempts do not produce duplicate side effects/events.
- [ ] Activation specs pass.

---

### Phase 5: Integration Coverage and Hardening

**Objective:** Verify end-to-end behavior across migration, API, service, and persistence boundaries with deterministic integration tests.

**Files to Modify/Create:**
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountCreationIntegrationSpec.groovy`: end-to-end create and replay flow.
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountActivationIntegrationSpec.groovy`: end-to-end activation flow.
- `src/main/resources/application.properties` (only if needed): minimal properties for deterministic tests/profile behavior.

**Tests to Write:**
- full flow: first POST creates `PENDING_VERIFICATION`, second same idempotency key returns same account.
- activation flow updates status to `ACTIVE` once.
- regression check: baseline `DepositApplicationContextSpec` still passes.

**Steps:**
1. Write integration specs covering ticket acceptance criteria end-to-end.
2. Run integration specs (expected fail until missing wiring is complete).
3. Fill remaining wiring gaps only (bean config, transactional annotations, exception mappings).
4. Run targeted specs, then run full `./mvnw test`.
5. Confirm no PII appears in logs or assertions.

**Acceptance Criteria:**
- [ ] Ticket 01 acceptance criteria are covered by automated tests.
- [ ] Integration specs pass deterministically.
- [ ] Full test suite passes.

## Open Questions

1. What is the canonical idempotency input for `POST /demand-deposit-accounts`?
   - **Option A:** HTTP header `x-idempotency-key` (common REST practice, keeps payload business-focused).
   - **Option B:** Request field `idempotencyKey` in JSON payload (simpler controller wiring, explicit in contract).
   - **Recommendation:** Option A for API consistency and safer future replay handling via gateway/middleware.

2. What activation trigger is in scope for Ticket 01?
   - **Option A:** Internal service method only (no API endpoint), invoked by future verification workflow.
   - **Option B:** Add internal endpoint/event handler now to trigger activation.
   - **Recommendation:** Option A to keep scope minimal while satisfying lifecycle logic and tests.

3. Which statuses should be included now in enum?
   - **Option A:** Only `PENDING_VERIFICATION` and `ACTIVE` for strict ticket scope.
   - **Option B:** Full PRD status set (`PENDING_VERIFICATION`, `ACTIVE`, `DORMANT`, `CLOSE_INITIATED`, `CLOSED`, `UNVERIFIED`).
   - **Recommendation:** Option B to avoid repeated schema/enumeration churn in Ticket 02 and Ticket 03.

## Risks & Mitigation

- **Risk:** Race condition on duplicate create calls causes double insert.
  - **Mitigation:** Enforce DB unique constraint on idempotency table and resolve unique-violation path deterministically in service.

- **Risk:** Activation logic becomes duplicated across service and domain.
  - **Mitigation:** Keep transition rule in domain method (`activate()`), service only orchestrates dependencies.

- **Risk:** Scope creep into Ticket 02/03 (blocks and transactions).
  - **Mitigation:** Restrict ticket implementation to account creation + lifecycle + event publication abstractions only.

- **Risk:** Error response shape drifts before Ticket 04 hardening.
  - **Mitigation:** Introduce single global handler now and reuse problem response factory.

## Success Criteria

- [ ] `POST /demand-deposit-accounts` is implemented and test-covered.
- [ ] Account creation is idempotent and duplicate-safe under replay.
- [ ] New account starts as `PENDING_VERIFICATION`.
- [ ] Activation transition to `ACTIVE` is one-time and test-covered.
- [ ] Account lifecycle events are emitted for creation and activation.
- [ ] All phase tests and full suite pass.
- [ ] Code follows Spring Boot and repository instruction conventions.

## Notes for Atlas

Use strict TDD per phase and do not skip repository-level uniqueness tests before service implementation. Favor minimal, explicit contracts over speculative abstractions. Keep package organization consistent with repository conventions (`controller`, `service`, `repository`, `domain`, `controller.exception`) and avoid exposing entities directly via API DTOs. If OpenAPI annotations are introduced in controllers, ensure dependencies are present and tests still run without requiring Swagger UI at runtime.