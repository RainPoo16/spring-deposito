# Plan: Spring Petclinic → Bank Deposit Microservice Migration

**Created:** 2026-03-01
**Status:** Ready for Atlas Execution

## Summary

This plan replaces the existing Petclinic domain with a REST-first bank deposit microservice on Spring Boot 4.0 and Java 21. The migration removes Owner/Pet/Visit/Vet/Specialty MVC flows, introduces the new banking domain model (Account + Transaction with UUID and BigDecimal), and implements the requested account lifecycle and balance operations with transactional safety and optimistic locking. The target package layout is `com.examples.deposit` with layered architecture (`controller`, `service`, `repository`, `domain`, `dto`, `mapper`, `exception`), with immediate hard cutover from old endpoints and inclusion of demo entities (`AuditLog`, `AccountEvent`). CI, k8s, DB schemas, and tests are updated so the system runs end-to-end as a banking microservice.

## Context & Analysis

**Relevant Files:**
- `pom.xml`: Already on Spring Boot `4.0.1` and Java `21`; still includes Thymeleaf/webjars dependencies.
- `build.gradle`: Also on Spring Boot `4.0.1` and Java toolchain `21`; includes MVC + Thymeleaf and webjars.
- `src/main/java/org/springframework/samples/deposito/owner/*`: Pet domain entities/controllers/repositories/validators to remove.
- `src/main/java/org/springframework/samples/deposito/vet/*`: Vet/Specialty domain to remove.
- `src/main/java/org/springframework/samples/deposito/model/*`: legacy base entities; evaluate removal vs retention.
- `src/main/java/org/springframework/samples/deposito/system/*`: reusable/non-domain configs; simplify for REST-only service.
- `src/main/resources/templates/**`: Thymeleaf UI to remove.
- `src/main/resources/db/postgres/schema.sql`: Replace Petclinic schema with deposit schema (PostgreSQL only).
- `src/test/java/org/springframework/samples/deposito/**`: Mostly pet-domain tests to remove/replace.
- `.github/workflows/deploy-and-test-cluster.yml`: readiness label hardcoded to `app=petclinic`.
- `k8s/petclinic.yml`, `k8s/db.yml`, `docker-compose.yml`: app/db naming and image still Petclinic-oriented.

**Key Functions/Classes:**
- `DepositoApplication` in `src/main/java/org/springframework/samples/deposito/DepositoApplication.java`: bootstrap package scan anchor.
- `OwnerController`, `PetController`, `VisitController`, `VetController`: MVC endpoints/views to remove.
- `OwnerRepository`, `PetTypeRepository`, `VetRepository`: legacy persistence interfaces to remove.
- `WebConfiguration`: locale/i18n interceptor for web templates; likely unnecessary for REST-only service.

**Dependencies:**
- `spring-boot-starter-data-jpa`: keep.
- `spring-boot-starter-webmvc`: can support REST controllers; keep unless switching to `starter-web` preference.
- `spring-boot-starter-thymeleaf` + webjars: remove for REST-only service.
- Testcontainers + PostgreSQL driver: keep for integration coverage; remove H2/MySQL dependencies.

**Patterns & Conventions:**
- Current code uses package-by-domain and constructor injection.
- Existing tests use `@WebMvcTest`, `@DataJpaTest`, and integration tests against profiles/containers.
- SQL init should be PostgreSQL-only and schema-focused (create tables/constraints; minimal or no seed data).

## Implementation Phases

### Phase 1: Baseline Cleanup & Package Bootstrap

**Objective:** Remove legacy pet/vet domain entry points and establish `com.examples.deposit` as the application root.

**Files to Modify/Create:**
- Modify `src/main/java/org/springframework/samples/deposito/DepositoApplication.java` (or replace with new app class under `com/examples/deposit`).
- Create `src/main/java/com/examples/deposit/DepositApplication.java` (preferred) with `@SpringBootApplication`.
- Remove `src/main/java/org/springframework/samples/deposito/owner/**`.
- Remove `src/main/java/org/springframework/samples/deposito/vet/**`.
- Remove `src/main/java/org/springframework/samples/deposito/system/WelcomeController.java`.
- Remove template files under `src/main/resources/templates/**`.

**Tests to Write:**
- `DepositApplicationContextTests` (context starts with new package root).

**Steps:**
1. Write context-load test for new `DepositApplication` class.
2. Run test and confirm failure before bootstrap migration.
3. Introduce new bootstrap class/package and remove legacy MVC entry controllers.
4. Run test and confirm context starts.
5. Run formatting/lint checks on modified Java sources.

**Acceptance Criteria:**
- [ ] Application bootstraps from `com.examples.deposit` package root.
- [ ] Legacy owner/vet controllers/entities are removed from build path.
- [ ] No Thymeleaf templates are required at startup.
- [ ] Test for application context passes.

---

### Phase 2: Banking Domain Model & Repository Layer

**Objective:** Introduce `Account` aggregate and `Transaction` entity using UUID, BigDecimal, enums, and optimistic locking.

**Files to Modify/Create:**
- Create `src/main/java/com/examples/deposit/domain/aggregate/Account.java`.
- Create `src/main/java/com/examples/deposit/domain/entity/Transaction.java`.
- Create `src/main/java/com/examples/deposit/domain/constant/AccountStatus.java`.
- Create `src/main/java/com/examples/deposit/domain/constant/TransactionType.java`.
- Create `src/main/java/com/examples/deposit/domain/valueobject/Money.java`.
- Create `src/main/java/com/examples/deposit/domain/valueobject/AccountNumber.java`.
- Create `src/main/java/com/examples/deposit/domain/audit/AuditLog.java`.
- Create `src/main/java/com/examples/deposit/domain/eventoutbox/AccountEvent.java`.
- Create `src/main/java/com/examples/deposit/repository/AccountRepository.java`.
- Create `src/main/java/com/examples/deposit/repository/TransactionRepository.java`.

**Tests to Write:**
- `AccountRepositoryTests`:
  - persists account with UUID primary key,
  - enforces unique `accountNumber`,
  - verifies `@Version` increments on update.
- `TransactionRepositoryTests`:
  - persists credit/debit transactions linked to account.

**Steps:**
1. Write failing `@DataJpaTest` cases for constraints and version behavior.
2. Implement entities/enums/value objects/repositories minimally.
3. Run repository tests until green.
4. Refactor mappings/indexes/column precision as needed.
5. Run lint/format.

**Acceptance Criteria:**
- [ ] `Account` uses UUID PK, BigDecimal balance, `@Version`, and status enum.
- [ ] `Transaction` uses UUID PK and many-to-one relation to `Account`.
- [ ] Unique constraint exists on `accountNumber`.
- [ ] Repository tests pass for persistence and optimistic locking basics.

---

### Phase 3: Service Layer for Account Lifecycle & Balance Operations

**Objective:** Implement business rules for open/credit/debit/freeze/unfreeze/close with transactional consistency.

**Files to Modify/Create:**
- Create `src/main/java/com/examples/deposit/service/account/AccountService.java`.
- Create `src/main/java/com/examples/deposit/service/transaction/TransactionService.java`.
- Create exceptions:
  - `src/main/java/com/examples/deposit/exception/AccountNotFoundException.java`
  - `src/main/java/com/examples/deposit/exception/InvalidAccountStateException.java`
  - `src/main/java/com/examples/deposit/exception/InsufficientBalanceException.java`

**Tests to Write:**
- `AccountServiceTests`:
  - open account => ACTIVE, zero balance,
  - freeze/unfreeze transitions,
  - close requires zero balance and sets CLOSED,
  - closed accounts reject all operations.
- `TransactionServiceTests`:
  - credit/debit only ACTIVE,
  - amount must be positive,
  - debit cannot overdraw,
  - each successful operation records transaction entry.

**Steps:**
1. Write failing service tests for each functional requirement.
2. Implement transactional service methods with invariant checks.
3. Add transaction recording in same transaction boundary.
4. Run unit/service tests to green.
5. Run lint/format.

**Acceptance Criteria:**
- [ ] All lifecycle rules are enforced exactly per requirements.
- [ ] Credit/debit methods are `@Transactional`.
- [ ] Transaction records are created for successful credit/debit.
- [ ] Service tests pass for success and failure cases.

---

### Phase 4: DTOs, Mapper, and API Contracts

**Objective:** Add explicit request/response models and mapping layer for clean REST boundaries.

**Files to Modify/Create:**
- Create request DTOs under `src/main/java/com/examples/deposit/dto/request/`:
  - `OpenAccountRequest.java`
  - `CreditRequest.java`
  - `DebitRequest.java`
  - `AccountActionRequest.java`
- Create response DTOs under `src/main/java/com/examples/deposit/dto/response/`:
  - `AccountResponse.java`
  - `TransactionResponse.java`
- Create mapper `src/main/java/com/examples/deposit/mapper/AccountMapper.java`.

**Tests to Write:**
- `AccountMapperTests` for field mapping correctness and monetary formatting consistency.
- Validation tests for DTO constraints (`@NotBlank`, `@Positive`, etc.).

**Steps:**
1. Write failing mapper/validation tests.
2. Implement DTOs with Bean Validation annotations.
3. Implement mapper methods for account and transaction responses.
4. Run tests and adjust DTO contracts.
5. Run lint/format.

**Acceptance Criteria:**
- [ ] Controllers use DTOs only (no entity exposure).
- [ ] Request validation rejects invalid payloads.
- [ ] Mapper tests pass.

---

### Phase 5: REST Controllers (Customer + Admin)

**Objective:** Expose required REST endpoints and connect them to service layer.

**Files to Modify/Create:**
- Create `src/main/java/com/examples/deposit/controller/account/AccountController.java`.
- Create `src/main/java/com/examples/deposit/controller/admin/AccountAdminController.java`.
- Add global exception mapping (if needed) in `com.examples.deposit.exception` package.

**Endpoint Scope (minimum):**
- `POST /api/accounts` open account
- `POST /api/accounts/{accountNumber}/credit` deposit
- `POST /api/accounts/{accountNumber}/debit` withdraw
- `POST /api/admin/accounts/{accountNumber}/freeze`
- `POST /api/admin/accounts/{accountNumber}/unfreeze`
- `POST /api/admin/accounts/{accountNumber}/close`
- `GET /api/accounts/{accountNumber}` account detail (recommended for verification)

**Tests to Write:**
- `AccountControllerTests` (`@WebMvcTest`): open/credit/debit success + validation errors.
- `AccountAdminControllerTests` (`@WebMvcTest`): freeze/unfreeze/close success + invalid state errors.

**Steps:**
1. Write failing controller tests for contract/status/body.
2. Implement controller endpoints + response mapping.
3. Implement exception handler to map domain errors to HTTP status codes.
4. Run controller tests to green.
5. Run lint/format.

**Acceptance Criteria:**
- [ ] All required operations are exposed as REST endpoints.
- [ ] Error conditions return deterministic HTTP responses.
- [ ] Controller tests pass for both happy and failure paths.

---

### Phase 6: Database Schema/Data and App Config Migration

**Objective:** Migrate to PostgreSQL-only schema SQL and remove non-PostgreSQL/UI-specific configuration.

**Files to Modify/Create:**
- Replace:
  - `src/main/resources/db/postgres/schema.sql`
- Remove:
  - `src/main/resources/db/h2/**`
  - `src/main/resources/db/mysql/**`
  - `src/main/resources/db/postgres/data.sql` (or keep as empty/minimal script if required by init settings)
- Update:
  - `src/main/resources/application.properties`
  - `src/main/resources/application-postgres.properties`
- Remove stale setup docs if no longer valid:
  - `src/main/resources/db/postgres/petclinic_db_setup_postgres.txt`

**Tests to Write:**
- PostgreSQL schema boot test ensuring account/transaction tables create successfully from schema SQL only.

**Steps:**
1. Write failing DB initialization tests against new schema expectations.
2. Implement SQL schema for account + transaction with constraints/indexes.
3. Keep SQL focused on DDL (create table/index/constraint); avoid value population except absolute minimum for tests.
4. Remove Thymeleaf/i18n properties that are no longer needed.
5. Run PostgreSQL integration tests.

**Acceptance Criteria:**
- [ ] PostgreSQL profile initializes banking schema successfully.
- [ ] No pet-domain tables or seed data remain.
- [ ] No H2/MySQL SQL artifacts remain.
- [ ] App config is REST-service oriented.

---

### Phase 7: Test Suite Replacement & Domain Purge Validation

**Objective:** Remove old pet-domain tests and replace with banking-domain coverage.

**Files to Modify/Create:**
- Remove pet tests under:
  - `src/test/java/org/springframework/samples/deposito/owner/**`
  - `src/test/java/org/springframework/samples/deposito/vet/**`
  - `src/test/java/org/springframework/samples/deposito/service/ClinicServiceTests.java`
  - `src/test/java/org/springframework/samples/deposito/PetClinicIntegrationTests.java`
- Create new tests under `src/test/java/com/examples/deposit/**`:
  - `account/AccountControllerTests.java`
  - `admin/AccountAdminControllerTests.java`
  - `service/AccountServiceTests.java`
  - `service/TransactionServiceTests.java`
  - `repository/AccountRepositoryTests.java`
  - `repository/TransactionRepositoryTests.java`
  - `integration/BankDepositIntegrationTests.java`

**Tests to Write:**
- End-to-end flow: open -> credit -> debit -> freeze -> unfreeze -> close.
- Negative flows: overdraw, invalid state transitions, operations on closed account.

**Steps:**
1. Remove legacy pet-domain test classes.
2. Implement banking test pyramid (repo/service/controller/integration).
3. Run targeted tests by layer first.
4. Run full test suite.
5. Fix only migration-related regressions.

**Acceptance Criteria:**
- [ ] No tests reference pet-domain classes/endpoints.
- [ ] Banking features are fully covered by automated tests.
- [ ] Full test suite passes.

---

### Phase 8: Deployment, CI, and Runtime Packaging Alignment

**Objective:** Update local/cluster deployment and CI checks to the new microservice identity.

**Files to Modify/Create:**
- Update `.github/workflows/deploy-and-test-cluster.yml`.
- Replace/rename `k8s/petclinic.yml` to banking app manifest.
- Update `k8s/db.yml` credentials/database names if needed.
- Update `docker-compose.yml` service/db names.
- Update build dependencies to PostgreSQL only (remove H2/MySQL/testcontainers-mysql where not needed).
- Update `settings.gradle` project name from `spring-petclinic` to deposit-aligned name.
- Update `README.md` runtime instructions and endpoint references.

**Tests to Write:**
- CI smoke check script/step that hits new REST health and one business endpoint (if feasible in workflow).

**Steps:**
1. Change app labels/selectors/image names in k8s manifests.
2. Update workflow wait labels and smoke assertions.
3. Align docker-compose database/user naming with app configs.
4. Remove non-PostgreSQL runtime/test dependencies and validate build.
5. Run local startup and CI-equivalent checks.
6. Validate no stale `petclinic` references remain.

**Acceptance Criteria:**
- [ ] CI workflow deploys and waits on correct app label.
- [ ] K8s manifests point to banking service image/name.
- [ ] Local compose environment matches app DB settings.
- [ ] Project runs with PostgreSQL as the only supported SQL backend.
- [ ] No blocking petclinic naming remains in deploy path.

## Open Questions

No open questions remain for execution. Scope decisions are fixed:

1. Full migration to `com.examples.deposit` as canonical package root.
2. Include both `AuditLog` and `AccountEvent` entities in this implementation.
3. Perform immediate hard cutover by removing old Petclinic endpoints.

## Risks & Mitigation

- **Risk:** Large-scale delete/add can introduce hidden startup/test breakage.
  - **Mitigation:** Execute strictly phase-by-phase with passing tests before each next phase.
- **Risk:** Optimistic locking edge cases under concurrent debit/credit.
  - **Mitigation:** Add concurrency-focused service/integration tests and ensure `@Version` behavior is asserted.
- **Risk:** PostgreSQL-specific DDL differences may break local/dev startup if scripts are not validated early.
  - **Mitigation:** Run PostgreSQL integration tests first in each DB-related phase and keep SQL limited to portable PostgreSQL DDL.
- **Risk:** CI/k8s still references petclinic labels/images.
  - **Mitigation:** Add grep-based check in CI for forbidden legacy identifiers in deploy artifacts.

## Success Criteria

- [ ] All pet-domain code, templates, and tests are removed.
- [ ] Banking domain and services implement all required operations and constraints.
- [ ] REST APIs replace MVC templates for account and admin operations.
- [ ] UUID, BigDecimal, JPA, `@Transactional`, and optimistic locking are implemented as specified.
- [ ] CI/deploy/local runtime configs are aligned with the new microservice.
- [ ] Full test suite passes with banking-domain coverage.

## Notes for Atlas

- Treat this as a high-impact migration; keep each phase commit-sized and verifiable.
- Implement `AuditLog` and `AccountEvent` as first-class persisted entities in this migration.
- Prefer deleting obsolete Petclinic artifacts rather than adapting them.
- Avoid speculative abstractions; only implement what is required by the listed features and technical constraints.
- After Phase 8, run a final workspace-wide scan for `pet|owner|visit|vet|specialty|petclinic` to confirm complete purge (excluding historical files like LICENSE if any false positives).