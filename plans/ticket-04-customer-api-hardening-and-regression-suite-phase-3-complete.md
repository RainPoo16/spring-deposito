## Phase 3 Complete: Transaction Code Validation Contract Fix

Phase 3 resolved the API/domain transaction-code mismatch by aligning request validation with policy-driven uppercase snake-case codes while preserving malformed-input 400 behavior. Coverage was expanded with HTTP integration tests for allowlisted, non-allowlisted, bypass, and malformed code scenarios.

**Files created/changed:**
- src/main/java/com/examples/deposit/controller/dto/PostCreditTransactionReq.java
- src/main/java/com/examples/deposit/controller/dto/PostDebitTransactionReq.java
- src/test/groovy/com/examples/deposit/integration/DemandDepositAccountTransactionIntegrationSpec.groovy
- plans/ticket-04-customer-api-hardening-and-regression-suite-phase-3-complete.md

**Functions created/changed:**
- PostCreditTransactionReq.transactionCode validation annotations
- PostDebitTransactionReq.transactionCode validation annotations
- DemandDepositAccountTransactionIntegrationSpec.pending verification account accepts allowlisted credit code via HTTP
- DemandDepositAccountTransactionIntegrationSpec.pending verification account rejects non-allowlisted code via HTTP
- DemandDepositAccountTransactionIntegrationSpec.bypass-validation transaction codes accepted through API when intended
- DemandDepositAccountTransactionIntegrationSpec.malformed transaction codes return 400 for credit and debit

**Tests created/changed:**
- DemandDepositAccountTransactionIntegrationSpec.pending verification account accepts allowlisted credit code via HTTP
- DemandDepositAccountTransactionIntegrationSpec.pending verification account rejects non-allowlisted code via HTTP
- DemandDepositAccountTransactionIntegrationSpec.bypass-validation transaction codes accepted through API when intended
- DemandDepositAccountTransactionIntegrationSpec.malformed transaction codes return 400 for credit and debit

**Review Status:** APPROVED

**Git Commit Message:**
fix: align transaction code request validation

- support policy-valid uppercase snake-case transaction codes
- enforce 64-char max transactionCode to match persistence
- add HTTP regression coverage for malformed code 400 cases
