## Phase 2 Complete: Persistence Model, Migration, and Locking Primitives

Implemented durable transaction persistence and replay/idempotency storage primitives with lock-safe repository access for account mutation paths. Migration V3 now introduces persistent balances plus transaction and idempotency tables with uniqueness rules and repository tests validating behavior.

**Files created/changed:**
- src/main/resources/db/migration/V3__add_transaction_posting_and_balance_columns.sql
- src/main/java/com/examples/deposit/domain/DemandDepositAccount.java
- src/main/java/com/examples/deposit/domain/DemandDepositAccountTransaction.java
- src/main/java/com/examples/deposit/domain/AccountTransactionIdempotency.java
- src/main/java/com/examples/deposit/repository/DemandDepositAccountTransactionRepository.java
- src/main/java/com/examples/deposit/repository/AccountTransactionIdempotencyRepository.java
- src/main/java/com/examples/deposit/repository/DemandDepositAccountRepository.java
- src/main/java/com/examples/deposit/repository/DemandDepositAccountBlockRepository.java
- src/test/groovy/com/examples/deposit/repository/DemandDepositAccountRepositorySpec.groovy
- src/test/groovy/com/examples/deposit/repository/DemandDepositAccountBlockRepositorySpec.groovy
- src/test/groovy/com/examples/deposit/repository/DemandDepositAccountTransactionRepositorySpec.groovy
- src/test/groovy/com/examples/deposit/repository/AccountTransactionIdempotencyRepositorySpec.groovy

**Functions created/changed:**
- DemandDepositAccountRepository.findByIdAndCustomerIdForUpdate(UUID, UUID)
- DemandDepositAccountBlockRepository.existsActiveCreditRestrictionOn(UUID, LocalDate)
- DemandDepositAccountBlockRepository.existsActiveDebitRestrictionOn(UUID, LocalDate)
- DemandDepositAccountTransactionRepository.findByAccountIdAndReferenceId(UUID, String)
- AccountTransactionIdempotencyRepository.findByCustomerIdAndIdempotencyKeyAndReferenceId(UUID, String, String)

**Tests created/changed:**
- DemandDepositAccountRepositorySpec.balance columns persist and load
- DemandDepositAccountRepositorySpec.findByIdAndCustomerIdForUpdate ownership and lock query behavior
- DemandDepositAccountBlockRepositorySpec.active incoming/outgoing restriction existence queries
- DemandDepositAccountTransactionRepositorySpec.transaction persistence and identity lookup
- DemandDepositAccountTransactionRepositorySpec.duplicate accountId/referenceId rejected
- AccountTransactionIdempotencyRepositorySpec.idempotency lookup and uniqueness behavior

**Review Status:** APPROVED

**Git Commit Message:**
feat: add transaction persistence and idempotency schema

- add V3 migration for balances, transaction, and idempotency tables
- add repositories and lock-safe account lookup for posting path
- add repository specs for uniqueness, locking, and restriction queries
