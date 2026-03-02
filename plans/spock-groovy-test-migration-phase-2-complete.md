## Phase 2 Complete: Pilot Conversion (Low-Risk Pure Tests)

Converted low-risk mapper and DTO validation tests from Java/JUnit to Groovy/Spock while preserving behavior and assertion coverage. Verified both Maven and Gradle targeted runs for the new specs.

**Files created/changed:**
- src/test/groovy/com/examples/deposit/mapper/AccountMapperSpec.groovy
- src/test/groovy/com/examples/deposit/dto/ValidationSpec.groovy
- src/test/java/com/examples/deposit/mapper/AccountMapperTests.java (removed)
- src/test/java/com/examples/deposit/dto/ValidationTests.java (removed)

**Functions created/changed:**
- AccountMapperSpec."should map Account domain to AccountResponse DTO"()
- AccountMapperSpec."should map Transaction domain to TransactionResponse DTO"()
- ValidationSpec."should fail validation for blank account holder name"()
- ValidationSpec."should fail validation for null initial deposit"()
- ValidationSpec."should fail validation for zero initial deposit"()
- ValidationSpec."should fail validation for negative initial deposit"()
- ValidationSpec."should pass validation for valid request"()

**Tests created/changed:**
- AccountMapperSpec
- ValidationSpec

**Review Status:** APPROVED

**Git Commit Message:**
test: migrate mapper and dto tests to spock

- convert AccountMapperTests to AccountMapperSpec with parity checks
- convert ValidationTests to ValidationSpec preserving validation cases
- remove migrated Java test classes and verify Maven/Gradle targets
