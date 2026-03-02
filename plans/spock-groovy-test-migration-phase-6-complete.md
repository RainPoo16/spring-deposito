## Phase 6 Complete: Integration and Context Test Migration

Migrated integration and application context smoke tests from Java/JUnit to Groovy/Spock with behavior parity. Verified the new integration/context specs pass in both Maven and Gradle and removed replaced Java tests.

**Files created/changed:**
- src/test/groovy/com/examples/deposit/integration/BankDepositIntegrationSpec.groovy
- src/test/groovy/com/examples/deposit/DepositApplicationContextSpec.groovy
- src/test/java/com/examples/deposit/integration/BankDepositIntegrationTests.java (removed)
- src/test/java/com/examples/deposit/DepositApplicationContextTests.java (removed)

**Functions created/changed:**
- BankDepositIntegrationSpec lifecycle and failure-path scenarios
- DepositApplicationContextSpec."context loads"()

**Tests created/changed:**
- BankDepositIntegrationSpec
- DepositApplicationContextSpec

**Review Status:** APPROVED

**Git Commit Message:**
test: migrate integration and context tests

- convert bank deposit integration flow to Spock specification
- convert application context smoke test to Spock
- remove migrated Java tests and verify Maven/Gradle targets
