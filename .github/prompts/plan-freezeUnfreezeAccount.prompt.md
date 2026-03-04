## Plan: Harden account freeze/unfreeze with outbox events

Strengthen the existing freeze/unfreeze implementation (already present) by preserving current admin endpoints, tightening domain/service guarantees, and adding reliable outbox event publication for state transitions. This approach minimizes API risk, reuses current lifecycle patterns, and adds observability/contract confidence through focused tests.

**Steps**
1. Phase 1 — Baseline and invariants (blocking): confirm current lifecycle rules in domain and service, then codify explicit transition matrix for `ACTIVE ↔ FROZEN`, `FROZEN → CLOSED`, and invalid transitions returning domain exceptions mapped to HTTP 409. Verify by identifying all transition call paths and existing controller advice behavior.  
2. Phase 2 — Outbox event model (depends on 1): introduce/extend event types for `AccountFrozen` and `AccountUnfrozen` in the existing outbox model, with minimal non-PII payload (`accountId`, `accountNumber` if already non-PII-approved, `previousStatus`, `newStatus`, `occurredAt`, correlation/trace IDs if available). Ensure event persistence is in the same transaction as account status update.  
3. Phase 3 — Service orchestration hardening (depends on 2): update `freezeAccount` and `unfreezeAccount` workflows to (a) fetch + validate transition, (b) apply domain transition, (c) persist account, (d) append outbox row atomically, (e) keep existing error mapping and endpoint contract unchanged.  
4. Phase 4 — API and documentation alignment (parallel with 3 once contracts fixed): keep `POST /admin/accounts/{accountNumber}/freeze` and `/unfreeze`; ensure controller OpenAPI responses include success + 409 problem detail consistency with existing close/freeze tests and controller advice.  
5. Phase 5 — Observability/logging pass (depends on 3): add or align structured logs and (if project supports) service span attributes using non-PII identifiers only; capture action name, account UUID, status transition, and result. Avoid request-body or sensitive-field logging.  
6. Phase 6 — Test expansion (depends on 3-5): extend unit/controller/integration tests for valid transitions, invalid transitions, and outbox persistence side effects per action (freeze/unfreeze). Add regression tests confirming frozen accounts still reject transaction mutations and unfreeze restores allowed operations.  
7. Phase 7 — Verification and rollout (depends on 6): run targeted tests first, then broader suite; capture migration/compatibility notes (none expected for existing `status` column), and document deployment-safe behavior (no endpoint break, additive eventing).

**Relevant files**
- `/Users/weichien.poo/IdeaProjects/spring-deposito/src/main/java/com/examples/deposit/domain/aggregate/Account.java` — source of truth for transition guards (`freeze`, `unfreeze`, `close`, `requireActiveStatus`).
- `/Users/weichien.poo/IdeaProjects/spring-deposito/src/main/java/com/examples/deposit/domain/constant/AccountStatus.java` — status vocabulary; verify no enum/schema drift.
- `/Users/weichien.poo/IdeaProjects/spring-deposito/src/main/java/com/examples/deposit/service/account/AccountService.java` — orchestration and transactional boundary for freeze/unfreeze.
- `/Users/weichien.poo/IdeaProjects/spring-deposito/src/main/java/com/examples/deposit/controller/admin/AccountAdminController.java` — endpoint contract to keep stable.
- `/Users/weichien.poo/IdeaProjects/spring-deposito/src/main/java/com/examples/deposit/repository/AccountRepository.java` — read/write access for account lifecycle actions.
- `/Users/weichien.poo/IdeaProjects/spring-deposito/src/main/java/com/examples/deposit/domain/eventoutbox/AccountEvent.java` — outbox entity to extend for frozen/unfrozen event semantics.
- `/Users/weichien.poo/IdeaProjects/spring-deposito/src/main/resources/db/postgres/schema.sql` — validate outbox/status table supports added event rows (no breaking DDL expected).
- `/Users/weichien.poo/IdeaProjects/spring-deposito/src/test/java/com/examples/deposit/service/AccountServiceTests.java` — transition and service side-effect tests.
- `/Users/weichien.poo/IdeaProjects/spring-deposito/src/test/java/com/examples/deposit/controller/AccountAdminControllerTests.java` — endpoint + error contract tests.
- `/Users/weichien.poo/IdeaProjects/spring-deposito/src/test/java/com/examples/deposit/integration/BankDepositIntegrationTests.java` — end-to-end lifecycle verification.
- `/Users/weichien.poo/IdeaProjects/spring-deposito/src/test/java/com/examples/deposit/service/TransactionServiceTests.java` — frozen-state transaction-blocking regression.

**Verification**
1. Run focused service/controller tests for freeze/unfreeze and confirm transition + HTTP 409 behavior for illegal states.
2. Add assertions that freeze/unfreeze writes expected outbox event rows with correct event type and transition payload fields.
3. Run integration lifecycle test covering active → frozen → active/unfrozen transitions and transaction behavior before/after.
4. Run full test suite for regression confidence.
5. Validate logs/spans contain only approved non-PII identifiers and status metadata.

**Decisions**
- Keep existing admin endpoints unchanged.
- Scope is hardening current implementation, not full redesign.
- Add outbox publication for freeze/unfreeze as additive behavior.
- Excluded: new UI, new endpoint versions, and unrelated account lifecycle refactors.

**Further Considerations**
1. Event contract naming: standardize on `AccountFrozen`/`AccountUnfrozen` (recommended) to align with past-tense event naming conventions.
2. Idempotency semantics: decide whether repeated freeze/unfreeze calls should remain 409 (current strict transition) or become no-op success; recommended to keep strict 409 to preserve current behavior and avoid silent state assumptions.
3. Correlation metadata: if request correlation ID is available in context, include it in outbox metadata for traceability without introducing PII.
