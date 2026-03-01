## Phase 2 Complete: Banking Domain Model & Repository Layer

Introduced Account aggregate and Transaction entity with UUID primary keys, BigDecimal monetary fields, enum status/type, optimistic locking via @Version, and unique account number constraint. Added AuditLog and AccountEvent demo entities. Repository tests verify persistence, constraints, and optimistic locking conflict detection.

**Files created/changed:**
- src/main/java/com/examples/deposit/domain/aggregate/Account.java
- src/main/java/com/examples/deposit/domain/entity/Transaction.java
- src/main/java/com/examples/deposit/domain/constant/AccountStatus.java
- src/main/java/com/examples/deposit/domain/constant/TransactionType.java
- src/main/java/com/examples/deposit/domain/audit/AuditLog.java
- src/main/java/com/examples/deposit/domain/eventoutbox/AccountEvent.java
- src/main/java/com/examples/deposit/repository/AccountRepository.java
- src/main/java/com/examples/deposit/repository/TransactionRepository.java
- pom.xml (added Lombok dependency)
- build.gradle (added Lombok dependencies)

**Functions created/changed:**
- Account.create() — static factory method
- Account.credit(), debit(), freeze(), unfreeze(), close() — business methods
- Transaction.create() — static factory method
- AccountRepository.findByAccountNumber()
- TransactionRepository.findByAccountIdOrderByCreatedAtDesc()

**Tests created/changed:**
- AccountRepositoryTests (4 tests: persist, unique constraint, version increment, stale update conflict)
- TransactionRepositoryTests (2 tests: persist linked to account, find by account ID)

**Review Status:** APPROVED (after revision to add optimistic locking conflict test)

**Git Commit Message:**
```
feat: add banking domain model with Account and Transaction entities

- Add Account aggregate with UUID PK, BigDecimal balance, optimistic locking
- Add Transaction entity with ManyToOne relation to Account
- Add AccountStatus and TransactionType enums
- Add AuditLog and AccountEvent demo entities
- Add AccountRepository and TransactionRepository interfaces
- Add repository tests for persistence, constraints, and locking
- Add Lombok dependency to Maven and Gradle builds
```
