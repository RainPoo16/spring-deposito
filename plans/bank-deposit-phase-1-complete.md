## Phase 1 Complete: Baseline Cleanup & Package Bootstrap

Removed all legacy Petclinic domain code (owner, vet, model, system controllers, templates) and established `com.examples.deposit` as the new application root with a clean Spring Boot bootstrap class. Context load test passes.

**Files created/changed:**
- src/main/java/com/examples/deposit/DepositApplication.java
- src/test/java/com/examples/deposit/DepositApplicationContextTests.java
- src/main/resources/application.properties (removed Thymeleaf settings)

**Functions created/changed:**
- `DepositApplication.main()` — new Spring Boot entry point
- `DepositApplicationContextTests.contextLoads()` — context validation test

**Tests created/changed:**
- DepositApplicationContextTests — context load test (PASSING)

**Review Status:** APPROVED

**Git Commit Message:**
```
feat: replace petclinic domain with bank deposit bootstrap

- Remove owner/vet/model packages and all Thymeleaf templates
- Create DepositApplication under com.examples.deposit package
- Add context load test for new application root
- Remove legacy test classes for pet domain
- Clean Thymeleaf config from application.properties
```
