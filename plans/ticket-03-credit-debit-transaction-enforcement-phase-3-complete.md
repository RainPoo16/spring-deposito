## Phase 3 Complete: Credit/Debit Service Orchestration and Idempotent Posting

Implemented transaction posting orchestration with lock-first account mutation, status/block/balance validation, and replay-safe idempotency behavior. Service logic now rejects idempotency identity reuse across mismatched account/payload contexts and returns consistent replay results tied to the originally posted transaction.

**Files created/changed:**
- src/main/java/com/examples/deposit/service/DemandDepositAccountTransactionService.java
- src/main/java/com/examples/deposit/service/dto/PostCreditTransactionCommand.java
- src/main/java/com/examples/deposit/service/dto/PostDebitTransactionCommand.java
- src/main/java/com/examples/deposit/service/dto/PostedTransactionResult.java
- src/main/java/com/examples/deposit/domain/exception/TransactionIdempotencyConflictException.java
- src/test/groovy/com/examples/deposit/service/DemandDepositAccountTransactionServiceSpec.groovy

**Functions created/changed:**
- DemandDepositAccountTransactionService.postCredit(PostCreditTransactionCommand)
- DemandDepositAccountTransactionService.postDebit(PostDebitTransactionCommand)
- DemandDepositAccountTransactionService.resolveReplayResult(...)
- DemandDepositAccountTransactionService.toResult(DemandDepositAccountTransaction, DemandDepositAccount)

**Tests created/changed:**
- DemandDepositAccountTransactionServiceSpec.successful credit posting
- DemandDepositAccountTransactionServiceSpec.successful debit posting
- DemandDepositAccountTransactionServiceSpec.insufficient balance rejection with no mutation
- DemandDepositAccountTransactionServiceSpec.status and block restriction rejections
- DemandDepositAccountTransactionServiceSpec.replay returns existing posting
- DemandDepositAccountTransactionServiceSpec.concurrent replay yields single posting
- DemandDepositAccountTransactionServiceSpec.conflict on same idempotency identity with different account
- DemandDepositAccountTransactionServiceSpec.conflict on same idempotency identity with mismatched amount/code
- DemandDepositAccountTransactionServiceSpec.conflict on same idempotency identity with mismatched transaction type

**Review Status:** APPROVED

**Git Commit Message:**
feat: implement idempotent credit debit posting service

- add transaction posting service with lock-safe balance mutation
- enforce status, block, and balance validation matrix
- add replay conflict checks and concurrency-focused service specs
