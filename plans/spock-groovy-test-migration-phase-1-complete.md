## Phase 1 Complete: Build Foundation for Spock/Groovy Coexistence

Enabled Spock/Groovy test execution in both Maven and Gradle while preserving existing Java/JUnit test execution. Added a minimal Spock smoke spec and verified mixed-framework execution successfully.

**Files created/changed:**
- build.gradle
- pom.xml
- src/test/groovy/com/examples/deposit/SpockSmokeSpec.groovy

**Functions created/changed:**
- build.gradle: test dependency/version wiring for Spock and Groovy
- pom.xml: gmavenplus + surefire test discovery configuration for Groovy specs
- SpockSmokeSpec."smoke should pass"()

**Tests created/changed:**
- SpockSmokeSpec

**Review Status:** APPROVED

**Git Commit Message:**
chore: add spock and groovy test foundation

- configure Maven and Gradle for mixed JUnit and Spock tests
- add gmavenplus and surefire include patterns for specs
- add deterministic Spock smoke spec and verify both toolchains
