## Phase 4 Complete: DTOs, Mapper, and API Contracts

Added request/response DTOs using Java records with Bean Validation annotations, and a utility mapper class for clean Account/Transaction domain-to-DTO conversion. Validation and mapping tests pass.

**Files created/changed:**
- src/main/java/com/examples/deposit/dto/request/OpenAccountRequest.java
- src/main/java/com/examples/deposit/dto/request/CreditRequest.java
- src/main/java/com/examples/deposit/dto/request/DebitRequest.java
- src/main/java/com/examples/deposit/dto/response/AccountResponse.java
- src/main/java/com/examples/deposit/dto/response/TransactionResponse.java
- src/main/java/com/examples/deposit/mapper/AccountMapper.java

**Functions created/changed:**
- AccountMapper.toResponse(Account) — maps Account entity to AccountResponse
- AccountMapper.toResponse(Transaction) — maps Transaction entity to TransactionResponse

**Tests created/changed:**
- AccountMapperTests (2 tests: account mapping, transaction mapping)
- ValidationTests (5 tests: blank owner/account number, null/negative/zero amounts)

**Review Status:** APPROVED

**Git Commit Message:**
```
feat: add DTOs, mapper, and API contracts for REST boundaries

- Add OpenAccountRequest, CreditRequest, DebitRequest with Bean Validation
- Add AccountResponse and TransactionResponse records
- Add AccountMapper utility for domain-to-DTO conversion
- Add mapper and validation tests (7 tests passing)
```
