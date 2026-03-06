## Phase 3 Complete: Customer Create Account API Contract

Implemented `POST /demand-deposit-accounts` with request validation, explicit success/problem content types, and centralized RFC7807-style error mapping. Added web-slice tests for success, validation, business errors, idempotency conflict, and malformed request variants to lock response contract consistency. Phase 3 review is approved.

**Files created/changed:**
- src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java
- src/main/java/com/examples/deposit/controller/dto/CreateDemandDepositAccountReq.java
- src/main/java/com/examples/deposit/controller/dto/CreateDemandDepositAccountResp.java
- src/main/java/com/examples/deposit/controller/exception/GlobalExceptionHandler.java
- src/main/java/com/examples/deposit/controller/exception/ApiProblemFactory.java
- src/test/groovy/com/examples/deposit/controller/DemandDepositAccountControllerSpec.groovy

**Functions created/changed:**
- `DemandDepositAccountController.createDemandDepositAccount(UUID customerId, CreateDemandDepositAccountReq req)`
- `GlobalExceptionHandler.handleCustomerNotEligible(CustomerNotEligibleForAccountCreationException ex)`
- `GlobalExceptionHandler.handleIdempotencyConflict(IdempotencyConflictException ex)`
- `GlobalExceptionHandler.handleValidationFailure(MethodArgumentNotValidException ex)`
- `GlobalExceptionHandler.handleMalformedRequest(Exception ex)`
- `GlobalExceptionHandler.handleUnexpectedException(Exception ex)`
- `ApiProblemFactory.customerNotEligible()`
- `ApiProblemFactory.idempotencyConflict()`
- `ApiProblemFactory.validationFailed(String detail)`
- `ApiProblemFactory.malformedRequest()`
- `ApiProblemFactory.internalServerError()`

**Tests created/changed:**
- `DemandDepositAccountControllerSpec.returns 201 and response body for successful account creation`
- `DemandDepositAccountControllerSpec.returns 400 problem detail for missing idempotency key`
- `DemandDepositAccountControllerSpec.returns 400 problem detail for blank idempotency key`
- `DemandDepositAccountControllerSpec.maps ineligible customer to 422 problem detail`
- `DemandDepositAccountControllerSpec.maps idempotency conflict to 409 problem detail`
- `DemandDepositAccountControllerSpec.returns 400 malformed request problem detail for malformed json body`
- `DemandDepositAccountControllerSpec.returns 400 malformed request problem detail for missing request body`
- `DemandDepositAccountControllerSpec.returns 400 malformed request problem detail for invalid x-customer-id header`
- `DemandDepositAccountControllerSpec.returns 400 malformed request problem detail for missing x-customer-id header`
- `DemandDepositAccountControllerSpec.replay call returns success with stable response semantics`

**Review Status:** APPROVED

**Git Commit Message:**
feat: add account creation controller contract

- add post demand-deposit-accounts endpoint with dto validation
- centralize problem detail mapping in global exception handler
- add web slice tests for success, conflict, and malformed requests
