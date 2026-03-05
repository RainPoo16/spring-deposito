## Phase 1 Complete: Establish Domain and Persistence Baseline

Implemented the initial DDA domain model and Flyway schema with lifecycle defaults and transition guard logic. This phase establishes a stable foundation for idempotent account creation and later API/service layers.

**Files created/changed:**
- src/main/java/com/examples/deposit/domain/DemandDepositAccountStatus.java
- src/main/java/com/examples/deposit/domain/DemandDepositAccount.java
- src/main/resources/db/migration/V1__create_demand_deposit_accounts.sql
- src/test/groovy/com/examples/deposit/domain/DemandDepositAccountSpec.groovy

**Functions created/changed:**
- DemandDepositAccount.createPending(UUID customerId, String idempotencyKey)
- DemandDepositAccount.activate()
- DemandDepositAccount.validateIdempotencyKey(String idempotencyKey)

**Tests created/changed:**
- DemandDepositAccountSpec.createPending initializes account baseline in pending verification
- DemandDepositAccountSpec.activate transitions account from pending verification to active
- DemandDepositAccountSpec.activate throws when account is not in pending verification

**Review Status:** APPROVED

**Git Commit Message:**
feat: add dda domain and migration baseline

- add demand deposit account entity with lifecycle transition guard
- add flyway migration for dda table, constraints, and indexes
- add domain spec for pending creation and activation behavior
