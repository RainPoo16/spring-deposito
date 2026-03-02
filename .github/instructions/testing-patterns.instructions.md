---
name: 'Testing Patterns'
description: 'Spock Framework testing patterns, TestContainers setup, fake implementations, async testing, and test data management'
applyTo: '**/*Test.groovy, **/*Client.groovy, **/test/**/*.groovy'
---

# Testing Patterns and Standards

Single testing standard for all Groovy/Spock tests in the Card Service. All tests must be deterministic, parallel-safe, and aligned with production behavior.

> **Precedence**: For network-specific transaction tests, defer to `@.github/instructions/transactions-testing-patterns.instructions.md`. For test review standards, see `@.github/instructions/unit-test-reviewer.instructions.md`.

## Core Principles

- **Determinism first** — every test must produce the same result on every run, regardless of execution order or parallel context
- **Real infrastructure over mocks** — use TestContainers (PostgreSQL, Kafka) and fake implementations; never use Mockito or `@MockBean`
- **Three-layer validation** — validate API response, database state, and emitted events for every integration test
- **Parallel safety** — use per-test unique identifiers (`GUID.v7().toUUID()`) and per-test fake instances; never share mutable static state
- **Production-aligned fakes** — fake implementations must mirror real service behavior with configurable success/failure responses

## 1) Non-Negotiable Rules

- Use **Spock** for tests.
- Use **`AbstractIntegrationTest`** for integration tests.
- Use **`objectMapper.readValue()`** for response parsing. Do **not** use `JsonSlurper`.
- Use **fake implementations** for external services (prefer interfaces + fakes).
- Keep tests **parallel-safe** (no shared mutable static state).
- Validate both:
  - business/database state
  - emitted events (when flow is event-driven)

## 2) PII Protection in Tests

- Use obviously fake data only.
- Do not log full objects that may include sensitive fields.
- Log IDs, statuses, counters, and technical metadata only.
- ✅ `logger.info("Created test entity id={}", entity.id)` / ❌ `logger.info("Created card={}", card)`

## 3) Test Base and Structure

- Integration tests extend `AbstractIntegrationTest`.
- Use descriptive test names: `"should ... when ..."` or `"... failed - #caseDescription"`.
- Structure every test with `given:` → `when:` → `then:` blocks.

```groovy
// ✅ Minimal integration test with TestContainers (via AbstractIntegrationTest)
class InternalCardControllerPaginationTest extends AbstractIntegrationTest {

    def "should return paginated card accounts with metadata"() {
        given: "A customer with a virtual card"
        def user = generateNewUserWithAVirtualCard()

        when: "Admin requests card accounts"
        def response = internalCardServiceClient.getCardAccounts(user.customerProjection.id, null, null, 0, 2)

        then: "Response contains paginated results"
        response.status == 200
        response.contentType == MediaType.APPLICATION_JSON_VALUE
        def result = objectMapper.readValue(response.body.toString(), GetCardAccountsResp.class)
        result.totalElements() >= 1
    }
}
// Reference: src/test/groovy/com/ytl/card/controller/card/InternalCardControllerPaginationTest.groovy
```

## 4) Response Validation (Mandatory)

Always validate: **HTTP status** + **content type** + **typed response body**.

- Success: `response.status == 200`, `response.contentType == MediaType.APPLICATION_JSON_VALUE`, parse with `objectMapper.readValue(response.body.toString(), TypedResponse.class)`.
- Error: `response.status == 4xx`, `response.contentType == MediaType.APPLICATION_PROBLEM_JSON_VALUE`, parse into `ProblemDetail.class`, assert `type` and `title`.
- ❌ Never use `JsonSlurper` for response parsing.

## 5) Parameterized Error Tests (`where:`)

Use data-driven tests when scenarios share the same structure and differ only by setup/inputs/expected errors.

- Name pattern: `"Operation failed - #caseDescription"`
- Declare data variables as method parameters.
- Use `where:` table with columns: description, user, setupAction, IDs, expected status/type/title.
- In `then:` block: assert status, content type, and parse `ProblemDetail` with `objectMapper.readValue()`.

### Critical Pitfall (Spock Binding)

- Method parameter must be `user` (not `setupUser`).
- In `where:`, use direct call: `generateNewUserWithAVirtualCard()`.
- Do **not** do `def user = setupUser()` in `given:`.

## 6) Async and Eventual Consistency

Use `await(timeout, { assertion })` helper or `PollingConditions(timeout: N, delay: D).eventually { assert ... }` for eventual consistency checks.

## 7) Event Testing (Kafka)

- Use `KafkaTestService` to send and assert Kafka events.
- Verify both the emitted event payload and resulting side effects (database state, downstream calls).
- Pattern: send event → poll until side effect appears → assert topic content with deserialized Avro payload.
- Use `kafkaTestService.assertTopicContent(topicName, assertion)` to validate published events.
- Use `kafkaTestService.deserializeAvroValue(topicName, bytes)` for Avro deserialization.

For transaction-specific event testing patterns (VISA, MyDebit, SAN), see `@.github/instructions/transactions-testing-patterns.instructions.md`.

## 8) Test Data and Generators

Use generator helpers from `AbstractIntegrationTest`:

- `generateNewUserWithAVirtualCard()`
- `generateNewUserWithAPhysicalCard()`
- `generateNewUserWithAConvertedActivePhysicalCard()`
- `generateNewUserWithSavingsAccount()`

Use `GUID.v7().toUUID()` for runtime IDs in tests.

## 9) Cleanup and Isolation

- Reset fake service state in `cleanup()` (e.g., `fakeService.reset()`).
- Do not use `repository.deleteAll()`.
- Do not rely on test order.

## 10) External Services: Interface + Fake Pattern

1. Define interface for external dependency.
2. Production class implements interface; inject interface in services.
3. Use fake implementation in tests — ensures reliability, determinism, and parallel safety.

Fake requirements: configurable success/failure responses, `reset()` support, no static mutable state.

## 11) Idempotency Testing

For retryable commands/events, test duplicate execution:

- first call succeeds
- duplicate call is safe (no duplicate side effects)
- state remains consistent

Check using stable keys such as `id` or `correlationId`.

- **Retryable API calls**: repeat the same request with the same business key and assert no duplicate records, events, or transfers.
- **Retryable Kafka events**: replay the same event and assert state remains stable (no duplicate entities or side effects).

## 12) Parallel Testing Rules

### Do

- Use per-test unique identifiers (`GUID.v7().toUUID()`) to avoid cross-test data collisions.
- Use per-test instances for mutable objects/fakes.
- Use `CompletableFuture`/parallel execution where concurrency is part of behavior.
- Assert final consistency after all futures complete.

### Don’t

- `ClockUtils.setFakeClock(...)` shared static mutation
- `System.setProperty(...)` without strict isolation
- static mutable fixtures used by multiple tests

## 13) Transaction Test Delegation

Transaction tests (VISA, MyDebit, SAN) must additionally follow `@.github/instructions/transactions-testing-patterns.instructions.md` for network-specific fixture composition, assertion patterns, and GL movement validation. When generic and transaction-specific instructions conflict, the transaction-specific file takes precedence for transaction tests.

## 14) Test Client Pattern

Encapsulate MockMvc calls in dedicated client classes. Each client wraps `MockMvc.perform(...)` calls and returns parsed results via `MvcUtil.parseMvcResult(result)`. This keeps test methods focused on assertions, not HTTP plumbing.

## 15) Error Scenario Matrix (Quick Reference)

| Category | Common HTTP Code |
|---|---|
| Not found | 404 |
| Validation | 400 |
| Business rule | 422 |
| External dependency | 424 |
| Authorization | 403 |

## 16) Minimum Review Checklist

Before finalizing a test:

- [ ] Uses correct base class (`AbstractIntegrationTest` for integration)
- [ ] Uses typed response parsing with `objectMapper.readValue()`
- [ ] Validates status + content type + response fields
- [ ] Covers happy path and key failure paths
- [ ] Resets fake state in `cleanup()`
- [ ] Avoids static mutable shared state
- [ ] Verifies side effects/events when applicable

## 17) Anti-Patterns (Reject)

- `JsonSlurper` response parsing
- shared mutable static state
- test-order-dependent behavior
- missing cleanup for stateful fakes
- deleting all records globally as cleanup
- assertions that ignore content type for error responses

## 18) Running Tests (Quick Commands)

- All tests: `./mvnw test`
- Single class: `./mvnw test -Dtest=ClassName`
- Single method: `./mvnw test -Dtest='ClassName#method name'`
- Build without tests: `./mvnw clean verify -DskipTests`

## 19) Test Scope and Organization

- Unit tests: pure domain/value-object/utility logic.
- Integration tests: default choice; full Spring + TestContainers via `AbstractIntegrationTest`.
- Keep tests isolated with generated data (`GUID.v7().toUUID()`).
- For event-driven flows, validate database effects and published events.

## References

- `@ARCHITECTURE.md` — domain model, event-driven patterns, and card service architecture
- `@.github/instructions/transactions-testing-patterns.instructions.md` — VISA, MyDebit, SAN transaction test patterns
- `@.github/instructions/unit-test-reviewer.instructions.md` — test quality review guidelines
- `@.github/instructions/pii-protection.instructions.md` — PII protection rules for test data
