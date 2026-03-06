## Phase 4 Complete: Transaction API Endpoints and ProblemDetail Mapping

Implemented customer-facing transaction posting endpoints for credit and debit flows and wired transaction-specific exception mapping into existing ProblemDetail conventions. Controller contracts now validate request payloads, propagate `x-customer-id` ownership context, and return explicit success/error content types.

**Files created/changed:**
- src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java
- src/main/java/com/examples/deposit/controller/exception/GlobalExceptionHandler.java
- src/main/java/com/examples/deposit/controller/exception/ApiProblemFactory.java
- src/main/java/com/examples/deposit/controller/dto/PostCreditTransactionReq.java
- src/main/java/com/examples/deposit/controller/dto/PostCreditTransactionResp.java
- src/main/java/com/examples/deposit/controller/dto/PostDebitTransactionReq.java
- src/main/java/com/examples/deposit/controller/dto/PostDebitTransactionResp.java
- src/test/groovy/com/examples/deposit/controller/DemandDepositAccountControllerSpec.groovy

**Functions created/changed:**
- DemandDepositAccountController.postCreditTransaction(UUID, PostCreditTransactionReq)
- DemandDepositAccountController.postDebitTransaction(UUID, PostDebitTransactionReq)
- GlobalExceptionHandler handlers for transaction blocked/status/insufficient-balance/idempotency-conflict exceptions
- ApiProblemFactory builders for transaction-specific problem details

**Tests created/changed:**
- DemandDepositAccountControllerSpec.credit transaction happy path (201 + JSON)
- DemandDepositAccountControllerSpec.debit transaction happy path (201 + JSON)
- DemandDepositAccountControllerSpec.transaction request validation failures (400 + problem+json)
- DemandDepositAccountControllerSpec.transaction blocked mapping (422)
- DemandDepositAccountControllerSpec.transaction status-not-allowed mapping (422)
- DemandDepositAccountControllerSpec.insufficient available balance mapping (422)
- DemandDepositAccountControllerSpec.transaction idempotency conflict mapping (409)

**Review Status:** APPROVED

**Git Commit Message:**
feat: add credit and debit transaction endpoints

- add /transactions/credit and /transactions/debit controller methods
- add transaction request/response DTOs and validation coverage
- map transaction exceptions to problem-detail responses
