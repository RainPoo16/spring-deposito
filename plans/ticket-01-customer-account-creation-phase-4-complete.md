## Phase 4 Complete: Activation Transition and Exactly-Once Semantics

Implemented lifecycle activation orchestration from `PENDING_VERIFICATION` to `ACTIVE` with domain-guarded transitions and commit-safe side-effect publication. Added activation-focused tests, including concurrent activation attempts, to verify exactly-once event emission under contention. Phase 4 review is approved.

**Files created/changed:**
- src/main/java/com/examples/deposit/service/DemandDepositAccountService.java
- src/main/java/com/examples/deposit/domain/DemandDepositAccount.java
- src/main/java/com/examples/deposit/domain/exception/InvalidAccountLifecycleTransitionException.java
- src/main/java/com/examples/deposit/service/AccountLifecycleEventPublisher.java
- src/main/java/com/examples/deposit/service/SpringAccountLifecycleEventPublisher.java
- src/test/groovy/com/examples/deposit/service/DemandDepositAccountActivationSpec.groovy

**Functions created/changed:**
- `DemandDepositAccountService.activateAccountIfEligible(UUID accountId)`
- `DemandDepositAccountService.publishActivationAfterCommit(UUID accountId, UUID customerId)`
- `DemandDepositAccount.activate()`
- `AccountLifecycleEventPublisher.publishAccountActivated(UUID accountId, UUID customerId)`
- `SpringAccountLifecycleEventPublisher.publishAccountActivated(UUID accountId, UUID customerId)`

**Tests created/changed:**
- `DemandDepositAccountActivationSpec.activates pending account once when eligibility criteria pass`
- `DemandDepositAccountActivationSpec.duplicate activation attempts are idempotent and emit event once`
- `DemandDepositAccountActivationSpec.does not transition when eligibility criteria fail`
- `DemandDepositAccountActivationSpec.non-pending account activation path is safely handled`
- `DemandDepositAccountActivationSpec.concurrent activation attempts publish side effect once`

**Review Status:** APPROVED with minor recommendations

**Git Commit Message:**
feat: add account activation lifecycle transition

- add service activation flow with domain transition guards
- publish activation lifecycle event after transaction commit
- add sequential and concurrent activation specs for exactly-once
