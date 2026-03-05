---
name: 'Testing Patterns'
description: 'Repository-wide Spock testing patterns for web slice, repository, and integration tests with isolation and deterministic assertions'
applyTo: '**/*Test.groovy, **/*Client.groovy, **/test/**/*.groovy'
---

# Testing Patterns and Standards

Single testing standard for all Groovy/Spock tests in this repository. All tests must be deterministic, isolated, and aligned with production behavior.

## Core Principles

- **Determinism first** — every test must produce the same result on every run, regardless of execution order or parallel context
- **Right test slice for the job** — use web slice, repository slice, and full integration tests intentionally
- **Observable assertions** — validate HTTP status, content type, and key payload fields
- **Parallel safety** — use per-test data and avoid shared mutable static state
- **Minimal mocking** — mock collaborators only in slice tests; prefer real wiring in integration tests

## 1) Non-Negotiable Rules

- Use **Spock** for tests.
- Use **`*Spec.groovy`** naming.
- Use **Spring test annotations that match test scope**:
  - `@WebMvcTest` for controller slice tests
  - `@DataJpaTest` for repository tests
  - `@SpringBootTest` + `@AutoConfigureMockMvc` for end-to-end integration tests
- Keep tests **parallel-safe** (no shared mutable static state).
- Validate **status + content type + critical response fields** for HTTP tests.

## 2) PII Protection in Tests

- Use obviously fake data only.
- Do not log full objects that may include sensitive fields.
- Log IDs, statuses, counters, and technical metadata only.
- ✅ `logger.info("Created test accountNumber={}", accountNumber)` / ❌ `logger.info("Created account={}", account)`

## 3) Test Base and Structure

- Use descriptive test names that describe behavior and expected outcome.
- Structure every test with `given:` → `when:` → `then:` blocks.

```groovy
// ✅ Minimal controller slice test
@WebMvcTest(AccountController)
@Import(GlobalExceptionHandler)
class AccountControllerSpec extends Specification {

  @Autowired
  private MockMvc mockMvc

  @SpringBean
  private AccountService accountService = Mock()

  def "openAccount returns 201"() {
    when:
    def result = mockMvc.perform(post("/api/accounts")
      .contentType(MediaType.APPLICATION_JSON)
      .content('{"ownerName":"Alice","accountNumber":"ACCT-CTRL-001"}'))

    then:
    1 * accountService.openAccount("Alice", "ACCT-CTRL-001") >> Account.create("Alice", "ACCT-CTRL-001")
    result.andExpect(status().isCreated())
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath('$.accountNumber').value('ACCT-CTRL-001'))
    }
}
// Reference: src/test/groovy/com/examples/deposit/controller/AccountControllerSpec.groovy
```

## 4) Response Validation (Mandatory)

Always validate: **HTTP status** + **content type** + **key response fields**.

- Success pattern: `status().isOk()` or `status().isCreated()` and `contentTypeCompatibleWith(MediaType.APPLICATION_JSON)`.
- Error pattern: assert 4xx/5xx and `contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)`.
- Assert relevant fields using `jsonPath`.

## 5) Parameterized Error Tests (`where:`)

Use data-driven tests when scenarios share the same structure and differ only by setup/inputs/expected errors.

- Name pattern: `"Operation failed - #caseDescription"`
- Use a `where:` table with clear scenario labels and expected outcomes.
- In `then:` block: assert status and payload fields per scenario.

### Common Pitfall (Spock Binding)

- Keep `where:` variable names consistent with method parameters.
- Avoid mutating shared objects in `where:` data rows.

## 6) Async and Eventual Consistency

Use `PollingConditions` for eventual consistency checks when async behavior is expected.

```groovy
def conditions = new PollingConditions(timeout: 5, delay: 0.2)
conditions.eventually {
  assert repository.findByAccountNumber(accountNumber).present
}
```

## 7) Integration Test Pattern

- Use `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` with `@AutoConfigureMockMvc`.
- Validate full request flow across controller, service, and persistence boundaries.
- Keep integration test payloads realistic but synthetic.

Reference: `src/test/groovy/com/examples/deposit/integration/BankDepositIntegrationSpec.groovy`

## 8) Repository Test Pattern

- Use `@DataJpaTest` for repository behavior and constraint validation.
- Cover uniqueness constraints, optimistic locking, and persistence behavior.

Reference: `src/test/groovy/com/examples/deposit/repository/AccountRepositorySpec.groovy`

## 9) Test Data and Isolation

- Use deterministic, fake test data (e.g., `ACCT-CTRL-001`, `ACCT-E2E-FLOW-001`).
- Keep data local to each test; do not reuse mutable global fixtures.
- Do not rely on execution order.

## 10) Mocking and Collaboration Boundaries

- In web slice tests, use `@SpringBean` and Spock `Mock()` for service collaborators.
- In integration tests, avoid replacing core beans unless the scenario requires it.
- Mock only external interactions not under test.

## 11) Parallel Testing Rules

### Do

- Keep per-test account numbers/IDs unique when tests can run concurrently.
- Keep all mutable fixtures in instance scope.
- Assert final consistency after concurrent operations.

### Don’t

- Use shared mutable static state.
- Depend on global JVM settings without strict reset.
- Write order-dependent tests.

## 12) Error Scenario Matrix (Quick Reference)

| Category | Common HTTP Code |
|---|---|
| Not found | 404 |
| Validation | 400 |
| Business rule | 409 or 422 |
| Unexpected server error | 500 |

## 13) Minimum Review Checklist

Before finalizing a test:

- [ ] Uses the correct test slice annotation for the target behavior
- [ ] Uses `given/when/then` with clear setup and assertions
- [ ] Validates status + content type + key response fields for HTTP tests
- [ ] Covers at least one happy path and one failure path
- [ ] Avoids shared mutable static state and order dependence

## 14) Anti-Patterns (Reject)

- Card-domain-specific fixtures and helpers copied from other repositories.
- Shared mutable static state.
- Test-order-dependent behavior.
- Assertions that ignore content type for error responses.
- Over-mocking in integration tests.

## 15) Running Tests (Quick Commands)

- All tests (Maven): `./mvnw test`
- Single spec (Maven): `./mvnw test -Dtest=AccountControllerSpec`
- All tests (Gradle): `./gradlew test`
- Single spec (Gradle): `./gradlew test --tests '*AccountControllerSpec'`

## 16) Test Scope and Organization

- Unit/spec-level logic tests: pure domain/value-object/utility behavior.
- Web slice tests: controller contract + validation + exception mapping.
- Repository tests: JPA mapping, constraints, and locking behavior.
- Integration tests: cross-layer flows using real Spring context.

## References

- `src/test/groovy/com/examples/deposit/controller/AccountControllerSpec.groovy` — `@WebMvcTest` + `@SpringBean` pattern
- `src/test/groovy/com/examples/deposit/repository/AccountRepositorySpec.groovy` — `@DataJpaTest` repository pattern
- `src/test/groovy/com/examples/deposit/integration/BankDepositIntegrationSpec.groovy` — full integration flow pattern
- `@.github/instructions/pii-protection.instructions.md` — PII protection rules for test data
