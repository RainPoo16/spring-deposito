## Phase 3 Complete: Service Unit Test Migration

Migrated service unit tests from Java/JUnit+Mockito to Groovy/Spock with interaction-based assertions and preserved business behavior. Verified targeted service specs pass on both Maven and Gradle.

**Files created/changed:**
- src/test/groovy/com/examples/deposit/service/AccountServiceSpec.groovy
- src/test/groovy/com/examples/deposit/service/TransactionServiceSpec.groovy
- src/test/java/com/examples/deposit/service/AccountServiceTests.java (removed)
- src/test/java/com/examples/deposit/service/TransactionServiceTests.java (removed)

**Functions created/changed:**
- AccountServiceSpec service behavior test scenarios (6 specs)
- TransactionServiceSpec service behavior test scenarios (6 specs)

**Tests created/changed:**
- AccountServiceSpec
- TransactionServiceSpec

**Review Status:** APPROVED

**Git Commit Message:**
test: migrate service tests to spock specs

- convert account and transaction service tests to Spock
- replace Mockito semantics with Spock interactions and stubs
- remove migrated Java tests and verify Maven/Gradle targets
