## Phase 5 Complete: Customer API Endpoints and Error Mapping

Implemented customer-facing block create, update, and cancel endpoints with validated request DTOs and consistent `ProblemDetail` error mapping. Business rule violations are mapped to `422`, while account/block not-found and ownership-mismatch scenarios are mapped to `404`; cancel uses `204 No Content`.

**Files created/changed:**
- src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java
- src/main/java/com/examples/deposit/controller/dto/CreateDemandDepositAccountBlockReq.java
- src/main/java/com/examples/deposit/controller/dto/CreateDemandDepositAccountBlockResp.java
- src/main/java/com/examples/deposit/controller/dto/UpdateDemandDepositAccountBlockReq.java
- src/main/java/com/examples/deposit/controller/dto/UpdateDemandDepositAccountBlockResp.java
- src/main/java/com/examples/deposit/controller/exception/GlobalExceptionHandler.java
- src/main/java/com/examples/deposit/controller/exception/ApiProblemFactory.java
- src/main/java/com/examples/deposit/domain/exception/BlockNotFoundException.java
- src/main/java/com/examples/deposit/service/DemandDepositAccountBlockService.java
- src/test/groovy/com/examples/deposit/controller/DemandDepositAccountControllerSpec.groovy

**Functions created/changed:**
- `DemandDepositAccountController#createDemandDepositAccountBlock(...)`
- `DemandDepositAccountController#updateDemandDepositAccountBlock(...)`
- `DemandDepositAccountController#cancelDemandDepositAccountBlock(...)`
- `ApiProblemFactory#duplicateOrOverlappingBlock()`
- `ApiProblemFactory#blockNotEligibleForOperation()`
- `ApiProblemFactory#blockNotFound()`
- `GlobalExceptionHandler#handleDuplicateOrOverlappingBlockException(...)`
- `GlobalExceptionHandler#handleBlockNotEligibleForOperationException(...)`
- `GlobalExceptionHandler#handleBlockNotFoundException(...)`

**Tests created/changed:**
- `DemandDepositAccountControllerSpec` create/update/cancel happy paths
- `DemandDepositAccountControllerSpec` validation failure mapping to `400` problem details
- `DemandDepositAccountControllerSpec` business failure mapping to `422` problem details
- `DemandDepositAccountControllerSpec` not-found and ownership mismatch mapping to `404` problem details

**Review Status:** APPROVED with minor recommendations

**Git Commit Message:**
feat: expose customer block management endpoints

- add create update and cancel block APIs with validated DTO contracts
- map overlap and eligibility errors to 422 and not-found cases to 404
- extend web slice tests for success validation and problem detail responses
