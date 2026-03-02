# Plan: Full Test Suite Migration to Spock/Groovy

**Created:** 2 March 2026
**Status:** Ready for Atlas Execution

## Summary

Migrate the current Java/JUnit test suite to Groovy/Spock across controller, service, repository, mapper, DTO, integration, and application context tests. The migration is phased to keep risk low: first enable Groovy/Spock in both Maven and Gradle while preserving existing green builds, then convert tests by layer from lowest-risk to highest-impact. Each phase includes explicit verification commands and acceptance criteria so Atlas can prove equivalence and prevent behavioral regressions.

## Context & Analysis

**Relevant Files:**
- `build.gradle`: Current Gradle test stack uses JUnit platform and Java-only test sources; needs Groovy/Spock support and test source-set alignment.
- `pom.xml`: Current Maven test dependencies include JUnit-oriented Spring test starters; needs Spock/Groovy dependencies and Surefire/Failsafe compatibility checks.
- `src/test/java/com/examples/deposit/DepositApplicationContextTests.java`: Context smoke test to convert to baseline Spock spec.
- `src/test/java/com/examples/deposit/mapper/AccountMapperTests.java`: Low-risk pure mapping tests for first pilot conversion.
- `src/test/java/com/examples/deposit/dto/ValidationTests.java`: Validation tests with assertion style conversions and exception expectations.
- `src/test/java/com/examples/deposit/service/AccountServiceTests.java`: Mockito-heavy unit tests requiring interaction/stubbing rewrite to Spock mocks.
- `src/test/java/com/examples/deposit/service/TransactionServiceTests.java`: Service orchestration tests with behavior/interaction assertions.
- `src/test/java/com/examples/deposit/controller/AccountControllerTests.java`: Web slice tests using MockMvc and bean replacement patterns.
- `src/test/java/com/examples/deposit/controller/AccountAdminControllerTests.java`: Additional web slice test migration with request/response assertions.
- `src/test/java/com/examples/deposit/repository/AccountRepositoryTests.java`: Data JPA tests with persistence and exception semantics.
- `src/test/java/com/examples/deposit/repository/TransactionRepositoryTests.java`: Data JPA query/constraint behaviors.
- `src/test/java/com/examples/deposit/integration/BankDepositIntegrationTests.java`: Full-stack integration behavior; migrate last due highest blast radius.
- `.github/instructions/testing-patterns.instructions.md`: Repository-standard target requiring Spock, deterministic tests, and typed response parsing expectations.

**Key Functions/Classes:**
- `AccountControllerTests`, `AccountAdminControllerTests`: `MockMvc` API contracts and HTTP error assertions.
- `AccountServiceTests`, `TransactionServiceTests`: Business orchestration and interaction contracts currently modeled with Mockito.
- `AccountRepositoryTests`, `TransactionRepositoryTests`: Data persistence, locking/validation behavior, and repository contract checks.
- `BankDepositIntegrationTests`: End-to-end workflow and event/state expectations.

**Dependencies:**
- Spring Boot 4.0.1 test starters: Keep for Spring test slices and integration bootstrap.
- JUnit 5: Keep temporarily during coexistence; remove/trim after complete migration.
- Groovy + Spock + spock-spring: Add as primary test framework dependencies.
- H2/Testcontainers dependencies: Preserve behavior during migration; only adjust when explicitly required by existing tests.

**Patterns & Conventions:**
- Project instruction target: Spock mandatory for Groovy tests, deterministic behavior, and no shared mutable test state.
- Existing tests are Java/JUnit/Mockito with AssertJ; migration must preserve behavior first, then align style.
- Keep naming and package mirroring to minimize churn and traceability risk.

## Implementation Phases

### Phase 1: Build Foundation for Spock/Groovy Coexistence

**Objective:** Enable Spock/Groovy test execution in both Gradle and Maven while retaining current JUnit pass state.

**Files to Modify/Create:**
- `build.gradle`: Add Groovy plugin/dependencies and ensure test task discovers Spock specs.
- `pom.xml`: Add Groovy/Spock dependencies and configure test plugin compatibility.
- `src/test/groovy/` (new directory tree): Prepare mirrored package paths for converted specs.

**Tests to Write:**
- `SpockSmokeSpec`: Minimal one-spec execution proof in `src/test/groovy`.

**Steps:**
1. Add Groovy/Spock dependencies and plugin wiring in Gradle and Maven.
2. Add one minimal Spock smoke spec.
3. Run Maven test command to verify mixed JUnit + Spock execution.
4. Run Gradle test command to verify mixed JUnit + Spock execution.
5. Keep all existing tests unchanged beyond framework enablement.

**Acceptance Criteria:**
- [ ] Maven runs both Java JUnit tests and Groovy Spock specs.
- [ ] Gradle runs both Java JUnit tests and Groovy Spock specs.
- [ ] No behavior regressions in existing tests at this stage.

---

### Phase 2: Pilot Conversion (Low-Risk Pure Tests)

**Objective:** Prove migration mechanics on tests with minimal Spring/mocking complexity.

**Files to Modify/Create:**
- Convert `src/test/java/com/examples/deposit/mapper/AccountMapperTests.java` to Groovy Spock equivalent.
- Convert `src/test/java/com/examples/deposit/dto/ValidationTests.java` to Groovy Spock equivalent.
- Remove or disable original Java versions only after Spock equivalents pass.

**Tests to Write:**
- `AccountMapperSpec.groovy`
- `ValidationSpec.groovy`

**Steps:**
1. Port test cases to Spock given/when/then style preserving assertions.
2. Convert exception assertions to Spock `thrown()` patterns.
3. Execute targeted test classes in Maven.
4. Execute targeted test classes in Gradle.
5. Remove replaced Java test classes.

**Acceptance Criteria:**
- [ ] Pilot specs pass with equivalent coverage and behavior.
- [ ] Java pilot tests are removed with no functional loss.
- [ ] Build remains green in both Maven and Gradle.

---

### Phase 3: Service Unit Test Migration

**Objective:** Replace Mockito-centric service unit tests with Spock interaction/stubbing semantics.

**Files to Modify/Create:**
- Convert `src/test/java/com/examples/deposit/service/AccountServiceTests.java`.
- Convert `src/test/java/com/examples/deposit/service/TransactionServiceTests.java`.

**Tests to Write:**
- `AccountServiceSpec.groovy`
- `TransactionServiceSpec.groovy`

**Steps:**
1. Rewrite Mockito stubs to Spock stubs (`>>`) and interactions (`1 *`).
2. Preserve existing business outcomes and exception scenarios.
3. Validate invocation counts and argument matching in Spock interaction blocks.
4. Run targeted service specs.
5. Remove replaced Java service tests.

**Acceptance Criteria:**
- [ ] Service behavior assertions are unchanged semantically.
- [ ] Mock interactions are validated via Spock interaction blocks.
- [ ] All service migration tests pass consistently.

---

### Phase 4: Controller Slice Test Migration

**Objective:** Convert web layer tests to Spock while preserving Spring MVC slice behavior and API contract assertions.

**Files to Modify/Create:**
- Convert `src/test/java/com/examples/deposit/controller/AccountControllerTests.java`.
- Convert `src/test/java/com/examples/deposit/controller/AccountAdminControllerTests.java`.

**Tests to Write:**
- `AccountControllerSpec.groovy`
- `AccountAdminControllerSpec.groovy`

**Steps:**
1. Port `@WebMvcTest` setup to Spock spec classes with Spring integration.
2. Replace mock bean declarations with Spock-compatible Spring bean replacement pattern.
3. Preserve MockMvc request assertions (status/body/content type) and error-path assertions.
4. Run targeted controller specs.
5. Remove replaced Java controller tests.

**Acceptance Criteria:**
- [ ] API contract assertions match prior behavior.
- [ ] Mocked collaborator behavior is stable and deterministic.
- [ ] All controller slice tests pass.

---

### Phase 5: Repository Test Migration

**Objective:** Convert persistence layer tests to Spock while preserving JPA/data behavior and constraints.

**Files to Modify/Create:**
- Convert `src/test/java/com/examples/deposit/repository/AccountRepositoryTests.java`.
- Convert `src/test/java/com/examples/deposit/repository/TransactionRepositoryTests.java`.

**Tests to Write:**
- `AccountRepositorySpec.groovy`
- `TransactionRepositorySpec.groovy`

**Steps:**
1. Port `@DataJpaTest` classes to Spock specs.
2. Keep transactional semantics and test data setup equivalent.
3. Convert exception expectations to Spock exception assertions.
4. Run targeted repository specs with H2 profile used previously.
5. Remove replaced Java repository tests.

**Acceptance Criteria:**
- [ ] Persistence and query behavior remains unchanged.
- [ ] Constraint/exception assertions remain equivalent.
- [ ] Repository specs pass in isolated runs and full suite.

---

### Phase 6: Integration and Context Test Migration

**Objective:** Migrate full integration tests and application context smoke test with behavior parity.

**Files to Modify/Create:**
- Convert `src/test/java/com/examples/deposit/integration/BankDepositIntegrationTests.java`.
- Convert `src/test/java/com/examples/deposit/DepositApplicationContextTests.java`.

**Tests to Write:**
- `BankDepositIntegrationSpec.groovy`
- `DepositApplicationContextSpec.groovy`

**Steps:**
1. Port integration test flow to Spock preserving endpoint sequence and assertions.
2. Keep infrastructure assumptions unchanged (MockMvc, DB wiring, profiles).
3. Migrate context-load test to minimal Spock spec.
4. Run integration-targeted suite.
5. Remove replaced Java integration/context tests.

**Acceptance Criteria:**
- [ ] End-to-end integration behavior remains equivalent.
- [ ] Context loads successfully via Spock-based smoke test.
- [ ] No loss of assertion coverage in integration paths.

---

### Phase 7: Cleanup, Standardization, and Final Verification

**Objective:** Remove legacy JUnit/Mockito artifacts that are no longer needed and verify fully migrated suite end-to-end.

**Files to Modify/Create:**
- `build.gradle`: Trim obsolete JUnit/Mockito dependencies if unused.
- `pom.xml`: Trim obsolete JUnit/Mockito dependencies if unused.
- `src/test/java/**`: Remove migrated Java test files.
- `src/test/groovy/**`: Ensure naming and structure consistency.

**Tests to Write:**
- None new unless gap is discovered during parity verification.

**Steps:**
1. Remove obsolete test dependencies only after confirming no references.
2. Run full Maven test suite.
3. Run full Gradle test suite.
4. Check for flaky behavior with one repeated run of targeted high-risk specs.
5. Ensure instruction alignment (Spock usage, deterministic patterns, no shared mutable static state).

**Acceptance Criteria:**
- [ ] All tests are Spock Groovy specs for migrated coverage.
- [ ] Maven full suite passes.
- [ ] Gradle full suite passes.
- [ ] No remaining Mockito/JUnit usage in test sources unless explicitly retained by decision.

## Open Questions

1. Should migration enforce immediate removal of all Mockito usage, or allow temporary coexistence inside Spock migration phases?
   - **Option A:** Immediate removal (pure Spock mocks/interactions from first converted class).
   - **Option B:** Temporary coexistence where required, cleanup at final phase.
   - **Recommendation:** Option A for consistency with testing instructions and reduced long-tail cleanup risk.

2. Should existing integration assertions using JSONPath be retained as-is during migration, or rewritten to typed response parsing now?
   - **Option A:** Retain JSONPath for behavioral parity, refactor later.
   - **Option B:** Rewrite to typed parsing during migration to align immediately with instruction standards.
   - **Recommendation:** Option A during migration, then separate follow-up refactor to reduce migration risk.

3. Must both Maven and Gradle remain fully supported for test execution in every phase?
   - **Option A:** Yes, verify both in each phase.
   - **Option B:** Primary on Maven, verify Gradle at key checkpoints only.
   - **Recommendation:** Option A, because repository supports both build tools and this avoids hidden regressions.

## Risks & Mitigation

- **Risk:** Build plugin incompatibility for Groovy/Spock across Maven and Gradle.
  - **Mitigation:** Phase 1 dual-run smoke verification before any class migration.

- **Risk:** Behavioral drift when translating Mockito semantics to Spock interactions.
  - **Mitigation:** Migrate service tests in small batches with targeted runs and explicit interaction assertions.

- **Risk:** Spring slice tests fail due to bean replacement differences between JUnit and Spock.
  - **Mitigation:** Pilot one controller spec first and standardize a reusable pattern before converting the rest.

- **Risk:** Migration creates flaky tests due to ordering/state leakage.
  - **Mitigation:** Enforce deterministic setup/cleanup and run repeated targeted high-risk specs.

## Success Criteria

- [ ] All existing test intent is preserved after migration.
- [ ] Target test classes are converted from Java/JUnit to Groovy/Spock.
- [ ] Maven and Gradle test workflows both pass end-to-end.
- [ ] Test suite follows repository testing instructions for Spock and deterministic behavior.
- [ ] No unresolved migration blockers remain for Atlas execution.

## Notes for Atlas

- Start with behavior parity, not stylistic perfection. Keep assertions equivalent first.
- Prefer one-class-at-a-time migration with immediate targeted execution.
- Do not over-refactor production code during test migration.
- Keep change scope surgical: migrate tests and minimal build wiring only.
- If instruction conflicts emerge, preserve passing behavior and document deviations in phase output for follow-up.