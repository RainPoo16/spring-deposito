# Plan: TICKET-001 Customer DDA Query API Contract

**Created:** 2026-03-05
**Status:** Ready for Atlas Execution

## Summary

Implement the customer-facing query contract for `GET /demand-deposit-accounts` with mandatory `x-customer-id` context, consistent JSON success payload, and typed client error handling for invalid header semantics. Since the repository is currently scaffold-level, this plan creates the minimal controller/service/DTO/error and integration test slices needed to satisfy the ticket without introducing out-of-scope admin/internal APIs. The implementation aligns with existing API design instructions for header documentation, OpenAPI responses, and deterministic request validation behavior. Delivery is split into small TDD phases so each requirement is independently verifiable.

## Context & Analysis

**Relevant Files:**
- `tickets/TICKET-001-customer-dda-query-api-contract.md`: Source ticket requirements and acceptance criteria.
- `docs/demand-deposit-account-functions-prd.md`: Product-level functional context for FR-1.1/FR-1.3.
- `.github/instructions/api-design-patterns.instructions.md`: Controller/OpenAPI/header documentation and typed error response guidance.
- `.github/instructions/java-spring-coding-standards.instructions.md`: Java/Spring implementation patterns.
- `.github/instructions/pii-protection.instructions.md`: Guardrails for header/log/event handling.
- `src/main/java/com/examples/deposit/DepositApplication.java`: Current source baseline (only active app class).
- `src/test/groovy/**`: Existing test framework location (Spock-based baseline).

**Key Functions/Classes:**
- `AccountController#getDemandDepositAccounts(...)` in `src/main/java/com/examples/deposit/controller/account/AccountController.java` (new): Owns HTTP contract and OpenAPI annotations.
- `AccountService#getAccountsByCustomerId(UUID)` in `src/main/java/com/examples/deposit/service/AccountService.java` (new): Encapsulates customer account query behavior.
- `AccountQueryResponse` in `src/main/java/com/examples/deposit/dto/response/AccountQueryResponse.java` (new): Typed success payload wrapper.
- `ApiErrorResponse` (or project-standard equivalent) in `src/main/java/com/examples/deposit/dto/error/` (new or reused): Typed client error payload for `400`.
- `GlobalExceptionHandler` in `src/main/java/com/examples/deposit/controller/advice/` (new or reused): Maps request/header validation failures to typed error response.

**Dependencies:**
- Spring Boot Web MVC for controller/validation flow.
- springdoc-openapi annotations for endpoint/header/response documentation.
- Spock + Spring test stack for integration tests.
- Security config (if present) to enforce scope/authorization checks.

**Patterns & Conventions:**
- Header requirement should be explicit and documented via OpenAPI annotation.
- Request validation failures return deterministic typed client errors (`400`).
- Success payload uses a contract wrapper (not raw list).
- Keep FR-2/internal/admin API out of scope.
- Avoid logging PII-rich header values beyond necessary diagnostics.

## Implementation Phases

### Phase 1: Define API Contract Types and Error Envelope

**Objective:** Establish DTOs/error models first so controller and tests target stable contract shapes.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/dto/response/AccountQueryResponse.java`: Add success wrapper fields required by ticket/PRD.
- `src/main/java/com/examples/deposit/dto/response/AccountSummaryDto.java` (if needed): Represent each account element.
- `src/main/java/com/examples/deposit/dto/error/ApiErrorResponse.java`: Typed error payload for client errors.

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/contract/AccountQueryContractSpec.groovy`: Validate JSON serialization shape for success and error wrappers.

**Steps:**
1. Write contract serialization test for expected `200` payload wrapper fields.
2. Write contract serialization test for expected typed `400` error payload fields.
3. Run targeted contract tests (expect fail).
4. Implement minimal DTO/error classes.
5. Re-run targeted tests (expect pass).

**Acceptance Criteria:**
- [ ] Success response wrapper fields are deterministic and documented in tests.
- [ ] Error response payload is typed and reusable.
- [ ] Contract tests pass.

---

### Phase 2: Implement Customer Query Endpoint with Mandatory Header

**Objective:** Deliver `GET /demand-deposit-accounts` with strict `x-customer-id` requirement and JSON `200` response.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/controller/account/AccountController.java`: Add endpoint method and request header binding.
- `src/main/java/com/examples/deposit/service/AccountService.java`: Add query method.
- `src/main/java/com/examples/deposit/service/impl/AccountServiceImpl.java` (if project structure uses impl classes): Return stub/fixture-backed data until persistence layer is wired.

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/controller/AccountControllerSpec.groovy`: Web-slice test for valid header -> `200` + JSON.

**Steps:**
1. Write controller test for valid `x-customer-id` request producing `200` and JSON content type.
2. Run controller test (expect fail).
3. Implement controller route, method signature, and service integration.
4. Implement minimal service behavior to return wrapper payload.
5. Re-run controller test (expect pass).

**Acceptance Criteria:**
- [ ] `GET /demand-deposit-accounts` exists.
- [ ] Missing route ambiguity is removed by deterministic mapping.
- [ ] Valid request returns `200` and `application/json`.
- [ ] Response body conforms to wrapper contract.

---

### Phase 3: Enforce Request-Level Invalid Header Semantics (`400`)

**Objective:** Ensure missing/malformed `x-customer-id` is rejected with typed `400` error.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/controller/advice/GlobalExceptionHandler.java` (or existing advice): Map binding/validation exceptions to typed error payload.
- `src/main/java/com/examples/deposit/controller/account/AccountController.java`: Add validation annotations if needed (`@Validated`, constraints).

**Tests to Write:**
- Extend `src/test/groovy/com/examples/deposit/controller/AccountControllerSpec.groovy`:
  - Missing header -> `400` typed error.
  - Invalid UUID header -> `400` typed error.

**Steps:**
1. Add failing tests for missing and malformed header semantics.
2. Run test subset (expect fail).
3. Implement/adjust exception mapping and validation behavior.
4. Re-run test subset (expect pass).
5. Ensure error payload field names and codes are deterministic.

**Acceptance Criteria:**
- [ ] Request without `x-customer-id` returns `400` typed client error.
- [ ] Invalid UUID in `x-customer-id` returns `400` typed client error.
- [ ] Error content type is JSON.

---

### Phase 4: Apply Authorization/Scope Checks

**Objective:** Enforce security constraints where configured for the customer query endpoint.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/controller/account/AccountController.java`: Add pre-authorization/scope annotation if project standard requires controller-level checks.
- `src/main/java/com/examples/deposit/config/security/SecurityConfig.java` (if present): Ensure endpoint matcher and required authority/scope are configured.

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/integration/AccountQuerySecurityIntegrationSpec.groovy`:
  - Unauthorized/insufficient scope behavior according to configured policy.
  - Authorized access happy path.

**Steps:**
1. Add failing security integration tests for authorized vs unauthorized/scope-missing scenarios.
2. Run security-focused tests (expect fail).
3. Implement security annotations/config route rules.
4. Re-run security tests (expect pass).
5. Verify no PII leakage in auth failure payload/logging.

**Acceptance Criteria:**
- [ ] Scope/authorization checks are applied per current security configuration.
- [ ] Authorized requests preserve `200` contract behavior.
- [ ] Unauthorized/scope failures follow existing global security error semantics.

---

### Phase 5: OpenAPI Documentation Coverage for Endpoint

**Objective:** Document header requirement and typed responses (`200`, `400`, plus applicable auth errors) for contract clarity.

**Files to Modify/Create:**
- `src/main/java/com/examples/deposit/controller/account/AccountController.java`: Add/complete `@Operation`, `@Parameter`, `@ApiResponses` annotations.
- `src/main/resources/application.properties` (if needed): Ensure OpenAPI endpoint generation is enabled consistently.

**Tests to Write:**
- `src/test/groovy/com/examples/deposit/integration/OpenApiContractIntegrationSpec.groovy`:
  - Assert generated OpenAPI includes `/demand-deposit-accounts`, required `x-customer-id` header, and `200`/typed error responses.

**Steps:**
1. Add failing test (or snapshot assertion) for required OpenAPI elements.
2. Run OpenAPI test (expect fail).
3. Implement OpenAPI annotations and schema references.
4. Re-run OpenAPI test (expect pass).
5. Confirm annotations do not diverge from runtime behavior.

**Acceptance Criteria:**
- [ ] OpenAPI lists `x-customer-id` as required request header.
- [ ] OpenAPI documents `200` success schema wrapper.
- [ ] OpenAPI documents typed `400` client error schema.

---

### Phase 6: End-to-End Integration Coverage for Ticket DoD

**Objective:** Provide integration tests that directly map to ticket acceptance criteria and lock regression behavior.

**Files to Modify/Create:**
- `src/test/groovy/com/examples/deposit/integration/AccountQueryIntegrationSpec.groovy`: End-to-end happy/negative request-level coverage.

**Tests to Write:**
- Happy path: authorized + valid header -> `200`, JSON content type, key payload fields.
- Negative path: missing header -> `400`, typed error body.
- Negative path: malformed header -> `400`, typed error body.

**Steps:**
1. Write integration tests for all acceptance criteria.
2. Run integration test class (expect some fail if gaps remain).
3. Fill remaining behavior/documentation gaps minimally.
4. Re-run targeted integration tests (expect pass).
5. Run broader test command for regression confidence.

**Acceptance Criteria:**
- [ ] Integration tests verify status code, content type, and key response fields.
- [ ] Acceptance criteria in TICKET-001 are covered by executable tests.
- [ ] Regression confidence established via broader suite run.

## Open Questions

1. What is the canonical typed error envelope for this service (if any existing standard is expected)?
   - **Option A:** Define `ApiErrorResponse` now for this service.
   - **Option B:** Reuse shared/global error model if discovered during implementation.
   - **Recommendation:** Prefer Option B if an existing model exists; otherwise Option A with minimal stable schema.

2. How should authorization be enforced in this stage (given scaffold baseline)?
   - **Option A:** Add controller-level `@PreAuthorize` for required scope.
   - **Option B:** Configure matcher-only security in `SecurityConfig`.
   - **Recommendation:** Use Option B for central policy + Option A only if repository convention requires explicit endpoint annotation.

3. What should initial account query data source be before persistence is implemented?
   - **Option A:** Deterministic stubbed in-memory response for contract-first delivery.
   - **Option B:** Block until repository/read model is implemented.
   - **Recommendation:** Option A to satisfy TICKET-001 contract scope quickly while keeping FR-2 out of scope.

## Risks & Mitigation

- **Risk:** No existing error/response conventions in scaffold project may cause rework.
  - **Mitigation:** Contract tests first; keep DTO/error models isolated and easily swappable.

- **Risk:** Security configuration may be absent, delaying scope-check acceptance.
  - **Mitigation:** Implement security test scaffolding with profile-based defaults and document fallback behavior.

- **Risk:** OpenAPI annotations diverge from runtime validation behavior.
  - **Mitigation:** Add OpenAPI integration assertions and request-level negative tests in same cycle.

## Success Criteria

- [ ] `GET /demand-deposit-accounts` implemented with required `x-customer-id` header.
- [ ] Valid authorized request returns `200` with contract-consistent JSON wrapper.
- [ ] Missing/malformed header returns typed `400` JSON error response.
- [ ] OpenAPI documents required header and `200`/typed error responses.
- [ ] Integration tests cover happy path and key request-level negative paths.
- [ ] No out-of-scope FR-2/admin endpoint work introduced.

## Notes for Atlas

- Repository is scaffold-level; prioritize minimal contract-first implementation over broad architecture.
- Follow `.github/instructions/api-design-patterns.instructions.md` and `.github/instructions/pii-protection.instructions.md` for header docs and sensitive-data handling.
- Keep each phase independently mergeable; do not batch all changes without phase verification.
- If security wiring is not present, implement endpoint contract and negative header semantics first, then add security checks in a clearly isolated commit/phase.
