## Phase 5 Complete: Repository Test Migration

Migrated repository tests to Spock with Data JPA behavior parity and removed legacy Java repository tests. Verified targeted repository specs pass in both Maven and Gradle after the removals.

**Files created/changed:**
- src/test/groovy/com/examples/deposit/repository/AccountRepositorySpec.groovy
- src/test/groovy/com/examples/deposit/repository/TransactionRepositorySpec.groovy
- src/test/java/com/examples/deposit/repository/AccountRepositoryTests.java (removed)
- src/test/java/com/examples/deposit/repository/TransactionRepositoryTests.java (removed)

**Functions created/changed:**
- AccountRepositorySpec repository persistence and constraint scenarios
- TransactionRepositorySpec repository query and relation scenarios

**Tests created/changed:**
- AccountRepositorySpec
- TransactionRepositorySpec

**Review Status:** APPROVED

**Git Commit Message:**
test: migrate repository tests to spock

- convert account and transaction repository tests to Spock
- preserve DataJpa behavior, constraint, and query assertions
- remove migrated Java repository tests and verify Maven/Gradle
