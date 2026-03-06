## Phase 1 Complete: Baseline Domain and Database Schema

Implemented the foundational persistence layer for Ticket 01 with Flyway schema, JPA entities, and repositories. Added repository slice tests first, then production code, and verified all targeted tests pass. Phase 1 acceptance criteria are satisfied and review is approved.

**Files created/changed:**
- src/main/resources/db/migration/V1__create_demand_deposit_account_tables.sql
- src/main/java/com/examples/deposit/domain/DemandDepositAccount.java
- src/main/java/com/examples/deposit/domain/DemandDepositAccountStatus.java
- src/main/java/com/examples/deposit/domain/AccountCreationIdempotency.java
- src/main/java/com/examples/deposit/repository/DemandDepositAccountRepository.java
- src/main/java/com/examples/deposit/repository/AccountCreationIdempotencyRepository.java
- src/test/groovy/com/examples/deposit/repository/DemandDepositAccountRepositorySpec.groovy
- src/test/groovy/com/examples/deposit/repository/AccountCreationIdempotencyRepositorySpec.groovy

**Functions created/changed:**
- `DemandDepositAccount.create(UUID customerId, DemandDepositAccountStatus status)`
- `DemandDepositAccount.activate()`
- `AccountCreationIdempotency.create(UUID customerId, String idempotencyKey, UUID accountId)`
- `DemandDepositAccountRepository.findByCustomerId(UUID customerId)`
- `AccountCreationIdempotencyRepository.findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey)`

**Tests created/changed:**
- `DemandDepositAccountRepositorySpec.persists and loads account with status stored as string`
- `DemandDepositAccountRepositorySpec.increments version when entity is updated`
- `AccountCreationIdempotencyRepositorySpec.finds record by customer id and idempotency key`
- `AccountCreationIdempotencyRepositorySpec.enforces uniqueness of customer id and idempotency key`

**Review Status:** APPROVED

**Git Commit Message:**
feat: add account schema and repository baseline

- add flyway migration for account and idempotency tables
- add demand deposit and idempotency jpa entities
- add repository specs for enum mapping and uniqueness
