## Phase 2 Complete: OpenAPI and Endpoint Documentation Alignment

Phase 2 aligned OpenAPI contracts for the four customer mutation endpoints and added guardrail tests to prevent documentation/runtime drift. Error response schemas are now consistently declared as `ProblemDetail` for 4xx/5xx, with canonical example fields verified in controller tests.

**Files created/changed:**
- src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java
- src/test/groovy/com/examples/deposit/controller/DemandDepositAccountControllerSpec.groovy
- plans/ticket-04-customer-api-hardening-and-regression-suite-phase-2-complete.md

**Functions created/changed:**
- DemandDepositAccountController#createDemandDepositAccount
- DemandDepositAccountController#createDemandDepositAccountBlock
- DemandDepositAccountController#postCreditTransaction
- DemandDepositAccountController#postDebitTransaction
- DemandDepositAccountControllerSpec.controller documents mutation endpoints with explicit operations and responses
- DemandDepositAccountControllerSpec.controller docs examples align with canonical problem detail fields

**Tests created/changed:**
- DemandDepositAccountControllerSpec.controller documents mutation endpoints with explicit operations and responses
- DemandDepositAccountControllerSpec.controller docs include problem detail schemas and representative examples
- DemandDepositAccountControllerSpec.controller docs examples align with canonical problem detail fields

**Review Status:** APPROVED

**Git Commit Message:**
chore: align mutation endpoint OpenAPI contracts

- document customer mutation endpoints with explicit responses
- align error examples with canonical problem detail payloads
- add reflection-based tests to prevent doc contract drift
