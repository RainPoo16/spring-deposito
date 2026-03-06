## Phase 6 Complete: Comprehensive Regression Suite and CI Gate

Phase 6 added a dedicated end-to-end regression integration spec that maps Ticket 04 acceptance criteria to executable journey and negative-path scenarios. Targeted integration runs and the full `./mvnw test` suite passed, providing CI-equivalent closure evidence.

**Files created/changed:**
- src/test/groovy/com/examples/deposit/integration/DemandDepositAccountRegressionIntegrationSpec.groovy
- plans/ticket-04-customer-api-hardening-and-regression-suite-phase-6-complete.md

**Functions created/changed:**
- DemandDepositAccountRegressionIntegrationSpec.regression journey create activate block blocked transaction cancel block successful transaction replay without duplicates
- DemandDepositAccountRegressionIntegrationSpec.negative path matrix returns problem detail with problem content-type - #scenario

**Tests created/changed:**
- DemandDepositAccountRegressionIntegrationSpec.regression journey create activate block blocked transaction cancel block successful transaction replay without duplicates
- DemandDepositAccountRegressionIntegrationSpec.negative path matrix returns problem detail with problem content-type - malformed request
- DemandDepositAccountRegressionIntegrationSpec.negative path matrix returns problem detail with problem content-type - ownership mismatch
- DemandDepositAccountRegressionIntegrationSpec.negative path matrix returns problem detail with problem content-type - account not found
- DemandDepositAccountRegressionIntegrationSpec.negative path matrix returns problem detail with problem content-type - restriction blocked
- DemandDepositAccountRegressionIntegrationSpec.negative path matrix returns problem detail with problem content-type - insufficient balance
- DemandDepositAccountRegressionIntegrationSpec.negative path matrix returns problem detail with problem content-type - idempotency conflict

**Review Status:** APPROVED

**Git Commit Message:**
test: add ticket-04 end-to-end regression matrix

- add chained customer mutation journey integration coverage
- add negative-path matrix with problem detail content-type checks
- verify targeted and full test-suite gates for closure evidence
