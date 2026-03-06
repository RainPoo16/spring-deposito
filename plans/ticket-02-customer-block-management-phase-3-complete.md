## Phase 3 Complete: Block Creation Service Rules (FR-BK-1, FR-BK-2)

Implemented the block-creation service flow with ownership validation, customer code eligibility checks, overlap prevention, and date-range validation. Service-level tests now cover required success and failure paths and pass with deterministic outcomes.

**Files created/changed:**
- src/main/java/com/examples/deposit/service/DemandDepositAccountBlockService.java
- src/main/java/com/examples/deposit/service/dto/CreateDemandDepositAccountBlockCommand.java
- src/test/groovy/com/examples/deposit/service/DemandDepositAccountBlockServiceSpec.groovy

**Functions created/changed:**
- `DemandDepositAccountBlockService#createBlock(CreateDemandDepositAccountBlockCommand)`

**Tests created/changed:**
- `DemandDepositAccountBlockServiceSpec` create happy path
- `DemandDepositAccountBlockServiceSpec` non-customer-allowed and unknown code rejection
- `DemandDepositAccountBlockServiceSpec` overlap rejection
- `DemandDepositAccountBlockServiceSpec` invalid date range rejection
- `DemandDepositAccountBlockServiceSpec` ownership mismatch rejection

**Review Status:** APPROVED

**Git Commit Message:**
feat: enforce block creation service rules

- add block creation service with ownership and eligibility checks
- reject overlapping blocks and invalid effective and expiry ranges
- add service specs for create success and failure rule matrix
