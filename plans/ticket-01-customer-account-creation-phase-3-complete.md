## Phase 3 Complete: Implement Application Service for Create + Activation Lifecycle

Implemented transactional service orchestration for idempotent account creation and one-time activation, including persisted lifecycle-event hooks for FR-AC-4. Review feedback was addressed with constraint-specific integrity handling, idempotency-key length validation, and transactional rollback integration coverage.

**Files created/changed:**
- src/main/java/com/examples/deposit/service/DemandDepositAccountService.java
- src/main/java/com/examples/deposit/service/dto/CreateDemandDepositAccountCommand.java
- src/main/java/com/examples/deposit/service/dto/DemandDepositAccountResult.java
- src/main/java/com/examples/deposit/exception/AccountLifecycleException.java
- src/main/java/com/examples/deposit/exception/AccountNotFoundException.java
- src/main/java/com/examples/deposit/exception/AccountCreationConflictException.java
- src/main/java/com/examples/deposit/domain/DemandDepositAccountLifecycleEvent.java
- src/main/java/com/examples/deposit/domain/DemandDepositAccountLifecycleEventType.java
- src/main/java/com/examples/deposit/repository/DemandDepositAccountLifecycleEventRepository.java
- src/main/resources/db/migration/V2__create_demand_deposit_account_lifecycle_events.sql
- src/test/groovy/com/examples/deposit/service/DemandDepositAccountServiceSpec.groovy
- src/test/groovy/com/examples/deposit/integration/DemandDepositAccountTransactionalIntegrationSpec.groovy
- src/test/groovy/com/examples/deposit/domain/DemandDepositAccountSpec.groovy
- src/main/java/com/examples/deposit/domain/DemandDepositAccount.java

**Functions created/changed:**
- DemandDepositAccountService.createMainAccount(CreateDemandDepositAccountCommand command)
- DemandDepositAccountService.activateAccount(UUID accountId)
- DemandDepositAccountService.isDuplicateIdempotencyViolation(DataIntegrityViolationException ex)
- DemandDepositAccountLifecycleEvent.accountCreated(DemandDepositAccount account)
- DemandDepositAccountLifecycleEvent.accountActivated(DemandDepositAccount account)
- CreateDemandDepositAccountCommand.CreateDemandDepositAccountCommand(UUID customerId, String idempotencyKey)
- DemandDepositAccount.validateIdempotencyKey(String idempotencyKey)

**Tests created/changed:**
- DemandDepositAccountServiceSpec.createMainAccount returns existing account for matching idempotency key
- DemandDepositAccountServiceSpec.createMainAccount persists pending account and lifecycle event for first create
- DemandDepositAccountServiceSpec.createMainAccount resolves duplicate idempotency race as replay when existing can be loaded
- DemandDepositAccountServiceSpec.createMainAccount rethrows non-idempotency data integrity violations
- DemandDepositAccountServiceSpec.activateAccount transitions pending account to active and persists lifecycle event
- DemandDepositAccountServiceSpec.activateAccount throws AccountNotFoundException when account does not exist
- DemandDepositAccountServiceSpec.activateAccount throws AccountLifecycleException when account cannot transition
- DemandDepositAccountServiceSpec.createMainAccount command rejects idempotency key longer than 128
- DemandDepositAccountSpec.createPending throws when idempotency key is longer than 128 characters
- DemandDepositAccountTransactionalIntegrationSpec.createMainAccount rolls back account when lifecycle event persistence fails

**Review Status:** APPROVED

**Git Commit Message:**
feat: add dda service lifecycle orchestration

- add idempotent create and one-time activation service flows
- persist lifecycle events in same transaction for fr-ac-4 hook
- add service and integration tests for conflict handling and rollback
