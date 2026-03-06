## Phase 4 Complete: Update and Cancel Lifecycle Rules (FR-BK-3)

Implemented update and cancel operations with lifecycle and initiator eligibility checks enforced in domain methods and orchestrated in the service layer. Added explicit behavior for repeated cancel attempts to fail as ineligible operations, with service tests covering eligible and ineligible paths.

**Files created/changed:**
- src/main/java/com/examples/deposit/service/DemandDepositAccountBlockService.java
- src/main/java/com/examples/deposit/service/dto/UpdateDemandDepositAccountBlockCommand.java
- src/main/java/com/examples/deposit/domain/DemandDepositAccountBlock.java
- src/test/groovy/com/examples/deposit/service/DemandDepositAccountBlockServiceSpec.groovy

**Functions created/changed:**
- `DemandDepositAccountBlockService#updateBlock(UpdateDemandDepositAccountBlockCommand)`
- `DemandDepositAccountBlockService#cancelBlock(UpdateDemandDepositAccountBlockCommand)`
- `DemandDepositAccountBlock#updateDetails(...)`
- `DemandDepositAccountBlock#cancel(...)`

**Tests created/changed:**
- `DemandDepositAccountBlockServiceSpec` update eligibility tests
- `DemandDepositAccountBlockServiceSpec` cancel eligibility tests
- `DemandDepositAccountBlockServiceSpec` repeated cancel rejection behavior

**Review Status:** APPROVED

**Git Commit Message:**
feat: add block update and cancel rules

- add service operations for block update and cancel lifecycle changes
- enforce status and initiator eligibility in domain guard methods
- extend service specs with eligibility matrix and repeat-cancel rejection
