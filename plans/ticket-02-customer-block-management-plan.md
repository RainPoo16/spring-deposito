# Plan: Ticket 02 Customer Block Management and Restriction Rules

**Created:** 2026-03-06
**Status:** Ready for Atlas Execution

## Summary

Implement Ticket 02 by extending the existing Ticket 01 DDA baseline with customer block management, overlap prevention, lifecycle update/cancel rules, and child-account propagation for eligible statuses. The implementation will follow current repository conventions: Spring Boot layered architecture, constructor injection, DTO validation at API boundary, transactional service orchestration, Flyway-first schema changes, RFC7807 `ProblemDetail` error responses, and Spock-based deterministic tests. The plan is incremental and test-driven so each phase delivers verifiable behavior without breaking existing account creation flows.

## Context & Analysis

**Relevant Files:**
- `tickets/TICKET-02-customer-block-management.md`: Source scope and acceptance criteria for FR-BK-1..FR-BK-4.
- `docs/demand-deposit-account-prd.md`: Product rules for block fields, overlap rejection, and active/dormant child propagation.
- `src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java`: Existing customer controller pattern and ResponseEntity/OpenAPI style.
- `src/main/java/com/examples/deposit/controller/exception/GlobalExceptionHandler.java`: Centralized `ProblemDetail` mapping pattern.
- `src/main/java/com/examples/deposit/service/DemandDepositAccountService.java`: Existing transactional orchestration and conflict-handling pattern.
- `src/main/java/com/examples/deposit/domain/DemandDepositAccount.java`: Current aggregate (no child-account or block model yet).
- `src/main/java/com/examples/deposit/domain/DemandDepositAccountStatus.java`: Current statuses only include `PENDING_VERIFICATION`, `ACTIVE`.
- `src/main/java/com/examples/deposit/repository/DemandDepositAccountRepository.java`: Existing repository baseline.
- `src/main/resources/db/migration/V1__create_demand_deposit_accounts.sql`: Current account table/check constraints.
- `src/main/resources/db/migration/V2__create_demand_deposit_account_lifecycle_events.sql`: Event persistence pattern.
- `src/test/groovy/com/examples/deposit/controller/DemandDepositAccountControllerSpec.groovy`: Web slice assertions for status/content-type/problem details.
- `src/test/groovy/com/examples/deposit/service/DemandDepositAccountServiceSpec.groovy`: Service-level transactional/exception test pattern.
- `src/test/groovy/com/examples/deposit/repository/DemandDepositAccountRepositorySpec.groovy`: Repository slice pattern for DB constraints.
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountCreationIntegrationSpec.groovy`: Full-stack integration style.
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountTransactionalIntegrationSpec.groovy`: Transaction rollback verification pattern.

**Key Functions/Classes:**
- `DemandDepositAccountController#createDemandDepositAccount(...)`: Template for customer endpoint style and header handling.
- `DemandDepositAccountService#createMainAccount(...)`: Pattern for idempotent create with race-safe conflict resolution.
- `DemandDepositAccountService#activateAccount(...)`: Pattern for lifecycle enforcement and event persistence.
- `GlobalExceptionHandler#problem(...)`: Canonical error response formatter.
- `DemandDepositAccountRepository#findByCustomerIdAndIdempotencyKey(...)`: Repository naming and query style baseline.

**Dependencies:**
- Spring Boot 3.5 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`): API, service, persistence, validation.
- Flyway: versioned schema migrations.
- UUID Creator (`GUID.v7()`): monotonic UUID generation.
- Spock + Spring test slices: deterministic controller/service/repository/integration testing.
- H2 test runtime + optional Spring Boot Testcontainers dependency available when PostgreSQL-specific validation is required.

**Patterns & Conventions:**
- Constructor injection with immutable dependencies.
- API DTOs as `record` types with Bean Validation.
- Controllers return `ResponseEntity<T>` with explicit content types.
- Error responses use `application/problem+json` and `ProblemDetail` with stable type strings.
- Mutating service methods use `@Transactional(rollbackFor = Exception.class)`.
- No PII in logs; log only UUIDs/error codes/technical metadata.
- Flyway migration-first (`spring.jpa.hibernate.ddl-auto=validate`).

## Implementation Phases

### Phase 1: Expand Account Domain Baseline for Ticket 02 Prerequisites

**Objective:** Prepare account model and schema for child-account propagation and eligible status checks.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/domain/DemandDepositAccountStatus.java`: Add statuses needed by Ticket 02 propagation rules (`DORMANT` at minimum).
- `src/main/java/com/examples/deposit/domain/DemandDepositAccount.java`: Add optional parent-child linkage fields/behavior required for propagation lookups.
- `src/main/resources/db/migration/V3__extend_demand_deposit_accounts_for_block_propagation.sql`: Add columns/constraints/indexes for hierarchy and expanded status checks.
- `src/main/java/com/examples/deposit/repository/DemandDepositAccountRepository.java`: Add query methods for fetching eligible child accounts by parent and status.

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/domain/DemandDepositAccountSpec.groovy`: Parent-child and status eligibility behavior.
- `src/test/groovy/com/examples/deposit/repository/DemandDepositAccountRepositorySpec.groovy`: Child-account query behavior.

**Steps:**
1. Write failing domain and repository tests for child-account retrieval and active/dormant filtering.
2. Run targeted specs (expect failure).
3. Implement status enum/domain/repository/migration updates minimally.
4. Re-run targeted specs (expect pass).
5. Verify existing Ticket 01 account creation tests still pass.

**Acceptance Criteria:**
- [ ] Account model supports representing parent and child account relationships.
- [ ] Eligible child selection supports active/dormant filtering.
- [ ] Flyway migration remains compatible with `ddl-auto=validate`.
- [ ] Existing Ticket 01 behaviors are unchanged.

---

### Phase 2: Introduce Block Domain Model and Persistence Schema

**Objective:** Add first-class block aggregate, enums, and persistence model for create/update/cancel lifecycle.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/domain/DemandDepositAccountBlock.java`: New block entity with status transitions and field invariants.
- `src/main/java/com/examples/deposit/domain/DemandDepositAccountBlockCode.java`: Customer-allowed block code enum.
- `src/main/java/com/examples/deposit/domain/DemandDepositAccountBlockInitiator.java`: Initiator enum.
- `src/main/java/com/examples/deposit/domain/DemandDepositAccountBlockStatus.java`: Block status enum (`PENDING`, `ACTIVE`, `CANCELLED`, `EXPIRED` or chosen final set).
- `src/main/java/com/examples/deposit/repository/DemandDepositAccountBlockRepository.java`: Block repository queries for overlap and lifecycle operations.
- `src/main/resources/db/migration/V4__create_demand_deposit_account_blocks.sql`: New block table, foreign keys, check constraints, and indexes.

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/domain/DemandDepositAccountBlockSpec.groovy`: Field validation and lifecycle method guards.
- `src/test/groovy/com/examples/deposit/repository/DemandDepositAccountBlockRepositorySpec.groovy`: Overlap query behavior and uniqueness rules.

**Steps:**
1. Write failing specs for block creation invariants and overlap query semantics.
2. Run specs (expect failure).
3. Implement entity/enums/repository and migration.
4. Re-run specs (expect pass).
5. Validate migration order and schema checks.

**Acceptance Criteria:**
- [ ] Block records persist required FR-BK-1 fields (code, initiator, effective/expiry date, remark).
- [ ] Repository supports lookup patterns needed for overlap rejection and propagation linkage.
- [ ] Domain lifecycle methods prevent illegal transitions.
- [ ] Domain/repository specs pass.

---

### Phase 3: Implement Create Block Service Logic With Overlap Rejection

**Objective:** Implement create flow for customer block requests with overlap checks and idempotent-safe behavior.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/service/DemandDepositAccountBlockService.java`: New service orchestrating create/update/cancel operations.
- `src/main/java/com/examples/deposit/service/dto/CreateDemandDepositAccountBlockCommand.java`: Internal command DTO.
- `src/main/java/com/examples/deposit/service/dto/DemandDepositAccountBlockResult.java`: Service response projection.
- `src/main/java/com/examples/deposit/exception/BlockOverlapConflictException.java`: Business conflict exception.
- `src/main/java/com/examples/deposit/exception/BlockCodeNotAllowedException.java`: Customer-allowed code enforcement exception.
- `src/main/java/com/examples/deposit/exception/BlockLifecycleException.java`: Invalid block transition exception.

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/service/DemandDepositAccountBlockServiceSpec.groovy`:
  - valid create persists expected status/dates
  - duplicate or overlapping active/pending same-code rejected
  - disallowed customer block code rejected

**Steps:**
1. Write service tests for happy path + overlap conflict matrix.
2. Run service spec (expect failure).
3. Implement transactional create logic with overlap detection and exception mapping.
4. Add race-safe protection: handle DB integrity violations for duplicate/overlap paths where applicable.
5. Re-run service spec (expect pass).

**Acceptance Criteria:**
- [ ] Create block flow stores expected status and dates.
- [ ] Duplicate/overlapping active/pending same-code blocks are rejected.
- [ ] Customer-allowed block code policy is enforced.
- [ ] Service tests pass deterministically.

---

### Phase 4: Implement Update/Cancel Lifecycle Rules and Propagation

**Objective:** Implement FR-BK-3 and FR-BK-4 for lifecycle eligibility and propagation to eligible children.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/service/DemandDepositAccountBlockService.java`: Add update/cancel and propagation orchestration.
- `src/main/java/com/examples/deposit/service/dto/UpdateDemandDepositAccountBlockCommand.java`: Update command DTO.
- `src/main/java/com/examples/deposit/service/dto/CancelDemandDepositAccountBlockCommand.java`: Cancel command DTO.
- `src/main/java/com/examples/deposit/repository/DemandDepositAccountBlockRepository.java`: Add propagated-block lookup and update support.
- `src/main/java/com/examples/deposit/exception/BlockNotFoundException.java`: Not-found exception for block operations.

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/service/DemandDepositAccountBlockServiceSpec.groovy` additions:
  - eligible status/initiator matrices for update/cancel (`@Unroll`)
  - propagation to active/dormant children only
  - rollback behavior if propagation fails in same transaction

**Steps:**
1. Write failing service tests for update/cancel eligibility and propagation scenarios.
2. Run spec (expect failure).
3. Implement update/cancel logic with strict eligibility checks.
4. Implement propagation for main account actions to eligible child accounts.
5. Re-run spec (expect pass) and verify transactional atomicity behavior.

**Acceptance Criteria:**
- [ ] Update/cancel allowed only for eligible statuses and initiators.
- [ ] Main account block operations propagate to eligible active/dormant children according to rule.
- [ ] Propagation behavior is deterministic and transactionally safe.
- [ ] Service tests pass.

---

### Phase 5: Expose Customer Block APIs and Consistent Error Contracts

**Objective:** Deliver customer API endpoints for create/update/cancel with validated payloads and standardized `ProblemDetail` mapping.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java`: Add block endpoints under customer path.
- `src/main/java/com/examples/deposit/controller/dto/CreateDemandDepositAccountBlockReq.java`: Create request DTO.
- `src/main/java/com/examples/deposit/controller/dto/UpdateDemandDepositAccountBlockReq.java`: Update request DTO.
- `src/main/java/com/examples/deposit/controller/dto/CancelDemandDepositAccountBlockReq.java` or path-only cancel contract.
- `src/main/java/com/examples/deposit/controller/dto/DemandDepositAccountBlockResp.java`: Response DTO.
- `src/main/java/com/examples/deposit/controller/exception/GlobalExceptionHandler.java`: Add block exception mappings.

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/controller/DemandDepositAccountBlockControllerSpec.groovy`:
  - create success and validation failures
  - overlap/lifecycle/not-found mappings to 409/422/404
  - content type assertions for success (`application/json`) and errors (`application/problem+json`)

**Steps:**
1. Write web-slice tests for endpoint contracts and error mapping.
2. Run spec (expect failure).
3. Implement DTO validation + controller methods + OpenAPI annotations + exception mappings.
4. Re-run web-slice tests (expect pass).
5. Verify existing Ticket 01 controller spec remains green.

**Acceptance Criteria:**
- [ ] `POST /demand-deposit-accounts/{accountId}/blocks` implemented.
- [ ] Update/cancel block request endpoints implemented with finalized path/method contract.
- [ ] Validation and business-rule failures map consistently using `ProblemDetail`.
- [ ] Controller tests pass.

---

### Phase 6: Integration and Regression Coverage for Ticket 02 Acceptance

**Objective:** Prove end-to-end behavior for create, overlap rejection, update/cancel eligibility, and propagation.

**Files to Modify/Create:**
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountBlockIntegrationSpec.groovy`: Core Ticket 02 E2E scenarios.
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountBlockPropagationIntegrationSpec.groovy`: Focused propagation scenarios.
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountBlockTransactionalIntegrationSpec.groovy`: Transaction rollback/atomicity checks.

**Tests to Write:**
- create block success with expected persisted state
- duplicate/overlap rejection scenario
- ineligible update/cancel scenario
- propagation to eligible children and skip ineligible children
- API error response contract consistency

**Steps:**
1. Write integration specs aligned to Ticket 02 acceptance criteria.
2. Run targeted integration tests (expect failure).
3. Fill integration gaps in service/repository/controller wiring.
4. Re-run targeted integration tests (expect pass).
5. Run broader regression suite for Ticket 01 and Ticket 02 specs.

**Acceptance Criteria:**
- [ ] All Ticket 02 acceptance scenarios are covered by integration tests.
- [ ] Ticket 01 behavior remains stable.
- [ ] Regression suite is deterministic in CI.

---

### Phase 7: Observability, Documentation Alignment, and Final Quality Gate

**Objective:** Ensure production-readiness via observability hygiene, API docs alignment, and verification evidence.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java`: Final OpenAPI response docs/examples for block endpoints.
- `src/main/java/com/examples/deposit/service/DemandDepositAccountBlockService.java`: Add span/observation instrumentation consistent with project capabilities and avoid PII attributes.
- `tickets/TICKET-02-customer-block-management.md` (optional status notes only if workflow requires).

**Tests to Write:**
- No new functional tests required if prior phases complete; add targeted assertions only for any instrumentation-related side effects if introduced.

**Steps:**
1. Align endpoint docs with actual status codes and problem types.
2. Add/verify service-level observability hooks for create/update/cancel operations.
3. Run full relevant test suite and capture verification evidence.
4. Ensure no PII is included in logs/events/exception details.
5. Prepare change summary for Ticket 04 hardening handoff.

**Acceptance Criteria:**
- [ ] API documentation matches implemented behavior.
- [ ] Block operations are observable without exposing sensitive data.
- [ ] Full verification evidence is available.
- [ ] Code remains compliant with repository instruction files.

## Open Questions

1. Which exact block codes are customer-allowed for Ticket 02?
   - **Option A:** Start with a minimal enum set (`DEBIT_DISABLED`, `CREDIT_DISABLED`, `BOTH_DISABLED`) and mark others admin-only.
   - **Option B:** Accept any code now and enforce policy in later ticket.
   - **Recommendation:** Option A to satisfy explicit “customer-allowed block codes” requirement and prevent policy drift.

2. What is the final customer API contract for update/cancel flows?
   - **Option A:** Add customer endpoints now (`PUT /demand-deposit-accounts/{accountId}/blocks/{blockId}`, `PATCH /.../cancel`).
   - **Option B:** Implement lifecycle logic in service only and defer customer endpoints.
   - **Recommendation:** Option A because Ticket 02 acceptance references update/cancel request outcomes.

3. How should propagation behave on partial child failures?
   - **Option A:** Atomic all-or-nothing transaction across parent and eligible children.
   - **Option B:** Best-effort propagation with partial success reporting.
   - **Recommendation:** Option A for financial consistency and simpler invariants.

4. Should overlap enforcement include PostgreSQL-specific exclusion constraints in Ticket 02?
   - **Option A:** Service/repository overlap checks only (DB-agnostic, simpler with H2 tests).
   - **Option B:** Add PostgreSQL exclusion constraint for race-proof overlap safety (plus containerized DB tests).
   - **Recommendation:** Option B if CI can run PostgreSQL-backed tests; otherwise implement Option A now and schedule Option B hardening in Ticket 04.

## Risks & Mitigation

- **Risk:** Ambiguous eligibility matrix (status + initiator) leads to inconsistent implementation.
  - **Mitigation:** Encode matrix as explicit parameterized tests before implementing service logic.

- **Risk:** Race conditions still allow overlap under concurrency.
  - **Mitigation:** Use transactional checks plus DB constraints where feasible; map `DataIntegrityViolationException` to conflict error.

- **Risk:** Propagation introduces transaction complexity and rollback surprises.
  - **Mitigation:** Add dedicated transactional integration tests validating atomicity and rollback behavior.

- **Risk:** Expanding status enum impacts existing check constraints and tests.
  - **Mitigation:** Update Flyway checks and regression-test Ticket 01 flows immediately after migration changes.

- **Risk:** Inconsistent error taxonomy across old/new endpoints.
  - **Mitigation:** Keep all mappings centralized in `GlobalExceptionHandler` and assert `type/detail/status` in controller specs.

## Success Criteria

- [ ] Customer block create endpoint is implemented with validated payload and policy checks.
- [ ] Duplicate/overlapping active or pending same-code blocks are rejected.
- [ ] Update/cancel flows enforce eligible status and initiator rules.
- [ ] Main account operations propagate correctly to eligible active/dormant child accounts.
- [ ] API validation and business-rule errors return consistent `ProblemDetail` responses.
- [ ] Repository/service/controller/integration tests for Ticket 02 pass.
- [ ] Existing Ticket 01 tests remain green.

## Notes for Atlas

- Implement phases sequentially and keep each phase independently verifiable (red-green-refactor).
- Prefer minimal, surgical changes to existing Ticket 01 code; add new block-specific classes rather than overloading existing account creation paths.
- Reuse established naming conventions (`*Req`, `*Resp`, `*Command`, `*Result`, exception suffixes) for consistency.
- Keep all logging/observability metadata non-PII (UUIDs/status/error types only).
- If PostgreSQL-specific overlap constraints are selected, add corresponding test profile strategy before claiming completion.
