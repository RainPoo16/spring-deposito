## Phase 7 Complete: FR-BK-4 Propagation Hook and Ticket 03 Readiness

Implemented a minimal block lifecycle propagation hook for successful create, update, and cancel operations, with post-commit publishing semantics and non-PII payloads. Added service tests to verify one-time hook invocation on success and no invocation on failed operations, while explicitly avoiding any Ticket 03 transaction enforcement logic.

**Files created/changed:**
- src/main/java/com/examples/deposit/service/BlockLifecycleEventPublisher.java
- src/main/java/com/examples/deposit/service/SpringBlockLifecycleEventPublisher.java
- src/main/java/com/examples/deposit/service/DemandDepositAccountBlockService.java
- src/test/groovy/com/examples/deposit/service/DemandDepositAccountBlockServiceSpec.groovy

**Functions created/changed:**
- `BlockLifecycleEventPublisher#publishBlockCreated(UUID, UUID)`
- `BlockLifecycleEventPublisher#publishBlockUpdated(UUID, UUID)`
- `BlockLifecycleEventPublisher#publishBlockCancelled(UUID, UUID)`
- `SpringBlockLifecycleEventPublisher#publishBlockCreated(UUID, UUID)`
- `SpringBlockLifecycleEventPublisher#publishBlockUpdated(UUID, UUID)`
- `SpringBlockLifecycleEventPublisher#publishBlockCancelled(UUID, UUID)`
- `DemandDepositAccountBlockService#publishAfterCommit(Runnable)`

**Tests created/changed:**
- `DemandDepositAccountBlockServiceSpec` lifecycle hook invoked once after successful create/update/cancel
- `DemandDepositAccountBlockServiceSpec` lifecycle hook not invoked on failed validation/business checks

**Review Status:** APPROVED

**Git Commit Message:**
feat: add block lifecycle propagation hook

- add block lifecycle event publisher abstraction and spring implementation
- publish create update and cancel lifecycle events after transaction commit
- add service specs for successful and failed hook invocation behavior
