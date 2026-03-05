## Phase 4 Complete: Expose Customer API Contract and Error Mapping

Implemented the customer-facing `POST /demand-deposit-accounts` endpoint with explicit `ResponseEntity` semantics and deterministic idempotency behavior (`201` first create, `200` replay). Added centralized RFC7807 error mapping with sanitized, client-safe details and OpenAPI response schemas using `ProblemDetail` for error responses.

**Files created/changed:**
- pom.xml
- src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java
- src/main/java/com/examples/deposit/controller/dto/CreateDemandDepositAccountReq.java
- src/main/java/com/examples/deposit/controller/dto/CreateDemandDepositAccountResp.java
- src/main/java/com/examples/deposit/controller/exception/GlobalExceptionHandler.java
- src/test/groovy/com/examples/deposit/controller/DemandDepositAccountControllerSpec.groovy

**Functions created/changed:**
- DemandDepositAccountController.createDemandDepositAccount(UUID customerId, CreateDemandDepositAccountReq request)
- GlobalExceptionHandler.handleAccountCreationConflict(AccountCreationConflictException ex, HttpServletRequest request)
- GlobalExceptionHandler.handleAccountLifecycle(AccountLifecycleException ex, HttpServletRequest request)
- GlobalExceptionHandler.handleAccountNotFound(AccountNotFoundException ex, HttpServletRequest request)
- GlobalExceptionHandler.handleBadRequest(Exception ex, HttpServletRequest request)
- GlobalExceptionHandler.handleUnexpected(Exception ex, HttpServletRequest request)
- GlobalExceptionHandler.problem(HttpStatus status, String type, String detail, String instance)
- GlobalExceptionHandler.defaultDetail(String detail, HttpStatus status)

**Tests created/changed:**
- DemandDepositAccountControllerSpec.create demand deposit account returns 201 with application json for first create
- DemandDepositAccountControllerSpec.create demand deposit account returns 200 with application json for idempotent replay
- DemandDepositAccountControllerSpec.create demand deposit account replay sequence returns 201 then 200 with stable accountId
- DemandDepositAccountControllerSpec.create demand deposit account returns 400 problem detail for invalid request body
- DemandDepositAccountControllerSpec.create demand deposit account maps #exception.class.simpleName to #expectedStatus
- DemandDepositAccountControllerSpec.create demand deposit account returns 400 problem detail for invalid customer id header
- DemandDepositAccountControllerSpec.create demand deposit account returns safe generic detail for unexpected server error

**Review Status:** APPROVED

**Git Commit Message:**
feat: add dda customer creation api

- add post demand-deposit-accounts endpoint with idempotent 201/200 semantics
- add global problemdetail exception mapping with sanitized error details
- add webmvc tests for success replay and error contract coverage
