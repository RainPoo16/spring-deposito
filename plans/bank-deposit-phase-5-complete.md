## Phase 5 Complete: REST Controllers (Customer + Admin)

Exposed all required REST endpoints for account lifecycle and balance operations. AccountController handles customer operations (open/get/credit/debit), AccountAdminController handles admin operations (freeze/unfreeze/close). GlobalExceptionHandler provides deterministic HTTP error responses using ProblemDetail.

**Files created/changed:**
- src/main/java/com/examples/deposit/controller/account/AccountController.java
- src/main/java/com/examples/deposit/controller/admin/AccountAdminController.java
- src/main/java/com/examples/deposit/exception/GlobalExceptionHandler.java

**Functions created/changed:**
- AccountController: openAccount(), getAccount(), credit(), debit()
- AccountAdminController: freeze(), unfreeze(), close()
- GlobalExceptionHandler: handleNotFound(), handleInvalidState(), handleInsufficientBalance(), handleValidation()

**Tests created/changed:**
- AccountControllerTests (6 tests: open 201, open 400, credit 200, debit 200, get 200, get 404)
- AccountAdminControllerTests (4 tests: freeze 200, unfreeze 200, close 200, freeze 409)

**Review Status:** APPROVED

**Git Commit Message:**
```
feat: add REST controllers for account and admin operations

- Add AccountController with open/get/credit/debit endpoints
- Add AccountAdminController with freeze/unfreeze/close endpoints
- Add GlobalExceptionHandler with ProblemDetail error responses
- Map exceptions to HTTP 404, 409, 422, 400 status codes
- Add 10 WebMvcTest controller tests
```
