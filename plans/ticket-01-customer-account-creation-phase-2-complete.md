## Phase 2 Complete: Account Creation Service with Idempotency

Implemented the service-layer account creation flow with transactional idempotency and one-time account-created event publishing. A concurrency-safe revision was applied to prevent duplicate account rows under same-key concurrent requests, and conflict fallback behavior is explicitly covered by tests. Review is approved with all Phase 2 criteria satisfied.

**Files created/changed:**
- src/main/java/com/examples/deposit/service/DemandDepositAccountService.java
- src/main/java/com/examples/deposit/service/AccountCreationEligibilityService.java
- src/main/java/com/examples/deposit/service/AccountLifecycleEventPublisher.java
- src/main/java/com/examples/deposit/service/SpringAccountLifecycleEventPublisher.java
- src/main/java/com/examples/deposit/domain/exception/CustomerNotEligibleForAccountCreationException.java
- src/main/java/com/examples/deposit/domain/exception/IdempotencyConflictException.java
- src/main/java/com/examples/deposit/domain/DemandDepositAccount.java
- src/test/groovy/com/examples/deposit/service/DemandDepositAccountServiceSpec.groovy

**Functions created/changed:**
- `DemandDepositAccountService.createMainAccount(UUID customerId, String idempotencyKey)`
- `DemandDepositAccountService.findExistingAccount(UUID customerId, String idempotencyKey)`
- `AccountCreationEligibilityService.isEligibleForMainAccountCreation(UUID customerId)`
- `AccountLifecycleEventPublisher.publishAccountCreated(UUID accountId, UUID customerId)`
- `SpringAccountLifecycleEventPublisher.publishAccountCreated(UUID accountId, UUID customerId)`
- `DemandDepositAccount.createWithId(UUID accountId, UUID customerId, DemandDepositAccountStatus status)`

**Tests created/changed:**
- `DemandDepositAccountServiceSpec.creates pending verification account for eligible customer`
- `DemandDepositAccountServiceSpec.returns existing account for replayed idempotency key without creating duplicates`
- `DemandDepositAccountServiceSpec.throws customer not eligible exception when eligibility fails`
- `DemandDepositAccountServiceSpec.concurrent requests with same idempotency key keep one account row and publish once`
- `DemandDepositAccountServiceSpec.throws idempotency conflict when fallback cannot load consistent account after integrity violation`

**Review Status:** APPROVED

**Git Commit Message:**
feat: implement idempotent account creation service

- add transactional main account creation with replay handling
- publish account-created event only on first successful create
- add concurrency and conflict fallback service test coverage
