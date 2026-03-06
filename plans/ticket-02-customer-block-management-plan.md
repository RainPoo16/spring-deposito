# Plan: Ticket 02 Customer Block Management and Restriction Rules

**Created:** 2026-03-06
**Status:** Ready for Atlas Execution (Approved)

## Summary

Implement Ticket 02 as a full restriction-management vertical slice on top of the Ticket 01 DDA baseline: block domain model, persistence, customer API contract, and business-rule enforcement for create, update, and cancel flows. The implementation must enforce overlap prevention and lifecycle eligibility rules while keeping API error mapping consistent with existing `ProblemDetail` conventions. The four provided block codes (`ACB`, `ACC`, `ACG`, `ADB`) will be modeled as a strict catalog with code-level metadata for direction and initiator source. Delivery is split into test-first phases so each capability is introduced incrementally and verified in controller, service, repository, and integration layers.

## Context & Analysis

**Relevant Files:**
- `tickets/TICKET-02-customer-block-management.md`: Source requirements and acceptance criteria for block create/update/cancel, overlap rejection, and rule enforcement.
- `docs/demand-deposit-account-prd.md`: Product-level target endpoint (`POST /demand-deposit-accounts/{accountId}/blocks`) and restriction lifecycle context.
- `src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java`: Existing customer API style (header-driven customer context, `ResponseEntity`, explicit JSON content type).
- `src/main/java/com/examples/deposit/service/DemandDepositAccountService.java`: Existing transaction boundary and orchestration style to follow for block mutations.
- `src/main/java/com/examples/deposit/controller/exception/GlobalExceptionHandler.java`: Existing problem-details mapping style for validation and business errors.
- `src/main/java/com/examples/deposit/controller/exception/ApiProblemFactory.java`: Existing `ProblemDetail` builder style and `deposit/<problem-type>` conventions.
- `src/main/java/com/examples/deposit/domain/DemandDepositAccount.java`: Aggregate root for account ownership and status checks during block operations.
- `src/main/java/com/examples/deposit/domain/DemandDepositAccountStatus.java`: Current account statuses (currently minimal); must be aligned with status-eligibility checks.
- `src/main/java/com/examples/deposit/repository/DemandDepositAccountRepository.java`: Existing repository extension point for ownership-safe account lookup.
- `src/main/resources/db/migration/V1__create_demand_deposit_account_tables.sql`: Baseline Flyway style and naming convention to follow for new block schema migration.
- `src/test/groovy/com/examples/deposit/controller/DemandDepositAccountControllerSpec.groovy`: Web slice test style for request validation and error mapping.
- `src/test/groovy/com/examples/deposit/service/DemandDepositAccountServiceSpec.groovy`: Service spec pattern for business rules and conflict behaviors.
- `src/test/groovy/com/examples/deposit/repository/DemandDepositAccountRepositorySpec.groovy`: Repository slice pattern for mapping/constraints.
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountCreationIntegrationSpec.groovy`: End-to-end integration test pattern.

**Key Functions/Classes:**
- `DemandDepositAccountController#createDemandDepositAccount(...)` in `src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java`: Contract template for new block endpoints.
- `DemandDepositAccountService#createMainAccount(...)` in `src/main/java/com/examples/deposit/service/DemandDepositAccountService.java`: Template for conflict-safe service orchestration.
- `GlobalExceptionHandler` in `src/main/java/com/examples/deposit/controller/exception/GlobalExceptionHandler.java`: Add block-specific exception mappings.
- `ApiProblemFactory` in `src/main/java/com/examples/deposit/controller/exception/ApiProblemFactory.java`: Add typed problem constructors for block failure cases.

**Dependencies:**
- `spring-boot-starter-web`: new controller endpoints and HTTP response contracts.
- `spring-boot-starter-validation`: payload constraints for block create/update DTOs.
- `spring-boot-starter-data-jpa`: new block entity and overlap/eligibility query persistence.
- `flyway-core`: schema migration for block tables and indexes.
- `com.github.f4b6a3:uuid-creator`: UUID v7 ID generation for block entities.
- `spock-core`, `spock-spring`, `spring-boot-starter-test`: web slice, repository, service, and integration verification.

**Patterns & Conventions:**
- Controller methods return `ResponseEntity<T>` with explicit `contentType`.
- Mutating service methods use `@Transactional(rollbackFor = Exception.class)`.
- Persist enums with `@Enumerated(EnumType.STRING)` only.
- Use UUID v7 IDs (`GUID.v7().toUUID()`), not random UUIDs.
- Centralize API error responses through `GlobalExceptionHandler` + `ApiProblemFactory`.
- Never log PII; only technical metadata and UUIDs.

**Block Code Catalog (Ticket 02 Source Input):**
- `ACB` = `BLOCK_INCOMING_BY_BANK` (deposit account level, block incoming only, requested by bank)
- `ACC` = `BLOCK_INCOMING_BY_CUSTOMER` (deposit account level, block incoming only, requested by customer)
- `ACG` = `BLOCK_INCOMING_BY_GOVERNMENT` (deposit account level, block incoming only, requested by government institutions)
- `ADB` = `BLOCK_OUTGOING_BY_BANK` (deposit account level, block outgoing only, requested by bank)

## Implementation Phases

### Phase 1: Domain Contract and Rule Matrix

**Objective:** Define explicit block domain vocabulary, code catalog metadata, and lifecycle/eligibility rules before persistence or API wiring.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/domain/BlockCode.java`: enum for `ACB`, `ACC`, `ACG`, `ADB` including metadata (`direction`, `requestedBy`, customer-allowed flag).
- `src/main/java/com/examples/deposit/domain/BlockDirection.java`: enum (`INCOMING`, `OUTGOING`).
- `src/main/java/com/examples/deposit/domain/BlockRequestedBy.java`: enum (`BANK`, `CUSTOMER`, `GOVERNMENT`).
- `src/main/java/com/examples/deposit/domain/AccountBlockStatus.java`: enum for lifecycle (`PENDING`, `ACTIVE`, `CANCELLED`, optionally `EXPIRED` if needed by date handling).
- `src/main/java/com/examples/deposit/domain/exception/DuplicateOrOverlappingBlockException.java`: business exception.
- `src/main/java/com/examples/deposit/domain/exception/BlockNotEligibleForOperationException.java`: business exception for ineligible status/initiator.
- `src/main/java/com/examples/deposit/domain/exception/AccountNotFoundException.java`: explicit not-found exception to avoid generic `IllegalArgumentException` handling.

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/domain/BlockCodeSpec.groovy`: verifies parsing and metadata for all four codes.
- `src/test/groovy/com/examples/deposit/domain/AccountBlockEligibilitySpec.groovy`: verifies status/initiator rule matrix for create/update/cancel decisions.

**Steps:**
1. Write domain specs that encode the complete block-code and eligibility matrix.
2. Run domain specs (expected fail).
3. Implement enum/value-object and exception classes with minimum behavior to satisfy the matrix.
4. Run domain specs (expected pass).
5. Refactor only naming/readability where needed; keep behavior unchanged.

**Acceptance Criteria:**
- [ ] All four block codes are represented as strict typed values.
- [ ] Direction and requester-source semantics are explicitly encoded in domain metadata.
- [ ] Eligibility decisions are captured in deterministic tests.
- [ ] Domain specs pass.

---

### Phase 2: Database Schema and Repository Support

**Objective:** Persist blocks and support fast overlap checks and lifecycle lookups.

**Files to Modify/Create:**
- `src/main/resources/db/migration/V2__create_demand_deposit_account_block_tables.sql`: create block table, FK to account, lifecycle/date columns, indexes for overlap checks.
- `src/main/java/com/examples/deposit/domain/DemandDepositAccountBlock.java`: JPA entity with UUID v7 ID, enum-string fields, optimistic lock version.
- `src/main/java/com/examples/deposit/repository/DemandDepositAccountBlockRepository.java`: overlap and account-scoped lookup queries.
- `src/main/java/com/examples/deposit/repository/DemandDepositAccountRepository.java`: add `findByIdAndCustomerId(...)` for ownership-safe lookups.

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/repository/DemandDepositAccountBlockRepositorySpec.groovy`:
  - enum persistence as strings.
  - overlap query truth table (same code + active/pending overlap returns true).
  - cancelled block excluded from overlap rejection.
  - ownership lookup by account+customer.

**Steps:**
1. Write failing `@DataJpaTest` specs for block persistence and overlap detection.
2. Run repository specs (expected fail due to missing schema/entity/repository).
3. Add migration, entity, and repository methods with minimal query logic.
4. Run repository specs (expected pass).
5. Validate no unnecessary schema scope creep beyond Ticket 02.

**Acceptance Criteria:**
- [ ] Flyway migration applies cleanly.
- [ ] Block rows persist with string enum values.
- [ ] Repository can detect duplicate/overlapping active or pending blocks by account+code.
- [ ] Repository specs pass.

---

### Phase 3: Block Creation Service Rules (FR-BK-1, FR-BK-2)

**Objective:** Implement create-block orchestration with ownership checks, payload validation rules, and overlap prevention.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/service/DemandDepositAccountBlockService.java`: create operation and rule enforcement.
- `src/main/java/com/examples/deposit/service/dto/CreateDemandDepositAccountBlockCommand.java`: internal service command object.
- `src/main/java/com/examples/deposit/domain/DemandDepositAccountStatus.java`: extend only if required for status-eligibility checks used in Ticket 02 logic.

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/service/DemandDepositAccountBlockServiceSpec.groovy`:
  - creates block for owned account with valid payload.
  - rejects unknown/non-customer-allowed code for customer endpoint.
  - rejects duplicate/overlapping block for same account and code.
  - rejects invalid date range (`effectiveDate > expiryDate`).
  - rejects account ownership mismatch.

**Steps:**
1. Write service specs for happy path and failure matrix.
2. Run service specs (expected fail).
3. Implement minimal transactional create flow: account ownership lookup, block-code validation, overlap check, persist.
4. Run service specs (expected pass).
5. Add non-PII log lines for create success/failure metadata.

**Acceptance Criteria:**
- [ ] Create path enforces FR-BK-1 payload constraints.
- [ ] Create path enforces FR-BK-2 duplicate/overlap rejection.
- [ ] Ownership is enforced by account+customer lookup.
- [ ] Service specs pass.

---

### Phase 4: Update and Cancel Lifecycle Rules (FR-BK-3)

**Objective:** Add block update and cancel operations with status/initiator eligibility enforcement.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/service/DemandDepositAccountBlockService.java`: add update/cancel methods.
- `src/main/java/com/examples/deposit/service/dto/UpdateDemandDepositAccountBlockCommand.java`: command for update/cancel inputs.
- `src/main/java/com/examples/deposit/domain/DemandDepositAccountBlock.java`: add guarded domain methods (`updateDetails(...)`, `cancel(...)`).

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/service/DemandDepositAccountBlockServiceSpec.groovy` additions:
  - update allowed only for eligible status+initiator combinations.
  - cancel allowed only for eligible status+initiator combinations.
  - reject update/cancel when block is ineligible (already cancelled/expired/etc.).
  - idempotent cancel behavior rules (either reject or no-op per chosen contract, asserted explicitly).

**Steps:**
1. Add failing tests for update/cancel eligibility matrix.
2. Run service specs (expected fail).
3. Implement minimal guarded domain/service logic and persistence updates.
4. Run service specs (expected pass).
5. Refactor to keep rule checks in domain methods and orchestration in service.

**Acceptance Criteria:**
- [ ] Update/cancel paths enforce initiator/status eligibility exactly as defined by the rule matrix.
- [ ] Ineligible update/cancel requests fail with clear business exceptions.
- [ ] Service specs pass.

---

### Phase 5: Customer API Endpoints and Error Mapping

**Objective:** Expose customer block create/update/cancel endpoints with consistent request validation and problem-detail responses.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java`:
  - `POST /demand-deposit-accounts/{accountId}/blocks`
  - `PUT /demand-deposit-accounts/{accountId}/blocks/{blockId}`
  - `PATCH /demand-deposit-accounts/{accountId}/blocks/{blockId}/cancel`
- `src/main/java/com/examples/deposit/controller/dto/CreateDemandDepositAccountBlockReq.java`: request DTO with bean validation.
- `src/main/java/com/examples/deposit/controller/dto/CreateDemandDepositAccountBlockResp.java`: response DTO.
- `src/main/java/com/examples/deposit/controller/dto/UpdateDemandDepositAccountBlockReq.java`: update request DTO.
- `src/main/java/com/examples/deposit/controller/dto/UpdateDemandDepositAccountBlockResp.java`: update/cancel response DTO (or use 204 for cancel if preferred contract).
- `src/main/java/com/examples/deposit/controller/exception/GlobalExceptionHandler.java`: add handlers for block domain exceptions.
- `src/main/java/com/examples/deposit/controller/exception/ApiProblemFactory.java`: add problem builders for block overlap/ineligible/not-found errors.

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/controller/DemandDepositAccountControllerSpec.groovy` additions:
  - create/update/cancel happy-path status and payload assertions.
  - validation errors map to 400 + `application/problem+json`.
  - overlap errors map consistently to chosen business status code.
  - ineligible update/cancel maps consistently to chosen business status code.
  - not-found/ownership mismatch maps to 404 or configured policy.

**Steps:**
1. Write `@WebMvcTest` failing specs for new endpoint contracts and error mapping.
2. Run controller specs (expected fail).
3. Implement controller methods + DTOs + exception mappings.
4. Run controller specs (expected pass).
5. Ensure all responses explicitly set JSON or problem+json content type.

**Acceptance Criteria:**
- [ ] Customer block create/update/cancel endpoints exist and are test-covered.
- [ ] Validation and business errors are consistently mapped via `ProblemDetail`.
- [ ] Controller specs pass.

---

### Phase 6: End-to-End Integration and Regression Safety

**Objective:** Validate full stack behavior and ensure no regressions to Ticket 01 flows.

**Files to Modify/Create:**
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountBlockIntegrationSpec.groovy`: full HTTP + DB flows.
- `src/test/groovy/com/examples/deposit/integration/DemandDepositAccountCreationIntegrationSpec.groovy`: minor updates only if setup/fixtures require block cleanup.

**Tests to Write:**
- full create-block flow persists expected fields/status.
- duplicate/overlap request rejected end-to-end.
- update and cancel flow transitions persisted state correctly.
- malformed and business-rule violations return consistent problem details.
- regression: Ticket 01 account creation integration still passes.

**Steps:**
1. Add failing integration specs for Ticket 02 acceptance criteria.
2. Run integration tests (expected fail until wiring complete).
3. Fill any remaining wiring gaps only (bean config, serialization, exception mappings).
4. Run target integration specs (expected pass).
5. Run full test suite (`./mvnw test`) and confirm green.

**Acceptance Criteria:**
- [ ] Ticket 02 acceptance criteria are covered by integration tests.
- [ ] Ticket 01 flows continue to pass unchanged.
- [ ] Full suite passes.

---

### Phase 7: FR-BK-4 Propagation Hook and Ticket 03 Readiness

**Objective:** Add minimal, explicit extension point for block propagation behavior without over-implementing Ticket 03 transaction enforcement.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/service/BlockLifecycleEventPublisher.java`: abstraction for block lifecycle events.
- `src/main/java/com/examples/deposit/service/SpringBlockLifecycleEventPublisher.java`: default Spring implementation (if eventing is in scope now).
- `src/main/java/com/examples/deposit/service/DemandDepositAccountBlockService.java`: invoke hook after create/update/cancel commits.

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/service/DemandDepositAccountBlockServiceSpec.groovy` additions:
  - event/hook invoked once after successful create/update/cancel.
  - not invoked when operation fails validation/business checks.

**Steps:**
1. Write failing tests for post-commit hook semantics.
2. Run service tests (expected fail).
3. Implement minimal abstraction + invocation points.
4. Run service tests (expected pass).
5. Keep payload/event contract minimal and non-PII.

**Acceptance Criteria:**
- [ ] Block lifecycle emits a deterministic extension signal for downstream behavior.
- [ ] Hook does not fire on failed operations.
- [ ] No transaction posting logic is implemented in Ticket 02 scope.

## Resolved Decisions

1. Customer-allowed block codes on `POST /demand-deposit-accounts/{accountId}/blocks`
  - **Selected:** Only `ACC` is customer-allowed.
  - **Implication:** `ACB`, `ACG`, and `ADB` remain non-customer codes and must be rejected for this endpoint.

2. HTTP status for duplicate/overlapping block conflicts
  - **Selected:** `422 Unprocessable Entity`.
  - **Implication:** Overlap/duplicate violations and eligibility-rule failures should both map to 422 with distinct problem types.

3. Cancel endpoint response contract
  - **Default for execution:** `204 No Content` unless changed during implementation by explicit user request.

## Risks & Mitigation

- **Risk:** Race conditions allow two concurrent overlapping blocks.
  - **Mitigation:** Pair transactional service checks with DB constraints/indexes where feasible; add concurrent service tests.

- **Risk:** Eligibility matrix ambiguity causes inconsistent behavior.
  - **Mitigation:** Encode matrix in dedicated domain specs first (Phase 1) before service/API implementation.

- **Risk:** Status expansion for account lifecycle introduces unintended regression.
  - **Mitigation:** Keep account-status changes minimal and covered by existing Ticket 01 activation specs.

- **Risk:** API error mapping drift across new endpoints.
  - **Mitigation:** Route all block exceptions through `GlobalExceptionHandler` + `ApiProblemFactory` only.

- **Risk:** Scope creep into Ticket 03 transaction enforcement.
  - **Mitigation:** Restrict Ticket 02 to block CRUD/rules + propagation hook, no transaction mutation logic.

## Success Criteria

- [ ] `POST /demand-deposit-accounts/{accountId}/blocks` implemented with strict code and payload validation.
- [ ] Update and cancel flows implemented with lifecycle and initiator eligibility rules.
- [ ] Duplicate/overlapping active or pending blocks of same code on same account are always rejected.
- [ ] API error responses are consistent and problem-detail based.
- [ ] All new repository/service/controller/integration tests pass.
- [ ] Full `./mvnw test` passes without Ticket 01 regressions.

## Notes for Atlas

Implement in strict phase order; do not skip Phase 1 matrix tests because later behavior depends on those decisions. Prefer minimal changes to existing Ticket 01 flow and avoid refactoring unrelated classes. Keep all block codes centralized in one enum/catalog and avoid string literals in controller/service logic. If any open question remains unresolved during execution, default to recommendations in this plan and document the chosen behavior in tests so contract semantics are explicit.
