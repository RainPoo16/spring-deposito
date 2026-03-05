## Phase 2 Complete: Add Repository and Idempotency Storage Guarantees

Implemented the JPA repository contract and validated idempotency storage guarantees using repository slice tests. Duplicate account creation for the same customer and idempotency key is now prevented at the database layer.

**Files created/changed:**
- src/main/java/com/examples/deposit/repository/DemandDepositAccountRepository.java
- src/test/groovy/com/examples/deposit/repository/DemandDepositAccountRepositorySpec.groovy

**Functions created/changed:**
- DemandDepositAccountRepository.findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey)

**Tests created/changed:**
- DemandDepositAccountRepositorySpec.findByCustomerIdAndIdempotencyKey returns persisted account
- DemandDepositAccountRepositorySpec.saving duplicate customer and idempotency key is rejected by database constraint

**Review Status:** APPROVED

**Git Commit Message:**
feat: add dda repository idempotency lookup

- add demand deposit account repository with idempotency finder
- add data jpa tests for lookup and duplicate-key rejection
- verify db unique constraint enforces idempotent create semantics
