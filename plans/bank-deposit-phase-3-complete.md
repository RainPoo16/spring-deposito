## Phase 3 Complete: Service Layer for Account Lifecycle & Balance Operations

Implemented AccountService and TransactionService with transactional boundaries, custom exception hierarchy, and type-based exception dispatch. All lifecycle rules (open/freeze/unfreeze/close) and balance operations (credit/debit) are enforced with proper error classification.

**Files created/changed:**
- src/main/java/com/examples/deposit/exception/AccountNotFoundException.java
- src/main/java/com/examples/deposit/exception/InvalidAccountStateException.java
- src/main/java/com/examples/deposit/exception/InsufficientBalanceException.java
- src/main/java/com/examples/deposit/service/account/AccountService.java
- src/main/java/com/examples/deposit/service/transaction/TransactionService.java
- src/main/java/com/examples/deposit/domain/aggregate/Account.java (debit throws InsufficientBalanceException)

**Functions created/changed:**
- AccountService.openAccount(), getAccount(), freezeAccount(), unfreezeAccount(), closeAccount()
- TransactionService.credit(), debit(), getTransactions()
- Account.debit() — now throws InsufficientBalanceException directly

**Tests created/changed:**
- AccountServiceTests (6 tests: open, freeze, unfreeze, close success/failure, not found)
- TransactionServiceTests (6 tests: credit/debit success, reject inactive, reject negative, reject overdraw)

**Review Status:** APPROVED (after revision to fix exception mapping)

**Git Commit Message:**
```
feat: add service layer for account lifecycle and balance operations

- Add AccountService with open/freeze/unfreeze/close operations
- Add TransactionService with credit/debit and transaction recording
- Add custom exceptions: AccountNotFound, InvalidAccountState, InsufficientBalance
- Use type-based exception dispatch for debit insufficient balance
- All service methods use @Transactional with proper rollback config
- Add 12 unit tests with Mockito for service layer
```
