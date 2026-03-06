## Phase 6 Complete: API Documentation and Observability Finalization

Completed Ticket 03 documentation and observability hardening by adding explicit OpenAPI annotations for transaction endpoints and OpenTelemetry span instrumentation for transaction service posting paths. The implementation preserves existing endpoint behavior while documenting success/error contracts and capturing non-PII telemetry attributes.

**Files created/changed:**
- src/main/java/com/examples/deposit/controller/DemandDepositAccountController.java
- src/main/java/com/examples/deposit/service/DemandDepositAccountTransactionService.java
- pom.xml

**Functions created/changed:**
- DemandDepositAccountController.postCreditTransaction(UUID, PostCreditTransactionReq) OpenAPI annotations
- DemandDepositAccountController.postDebitTransaction(UUID, PostDebitTransactionReq) OpenAPI annotations
- DemandDepositAccountTransactionService.postCredit(PostCreditTransactionCommand) span instrumentation
- DemandDepositAccountTransactionService.postDebit(PostDebitTransactionCommand) span instrumentation
- DemandDepositAccountTransactionService.startPostingSpan(...) helper

**Tests created/changed:**
- No new tests added in this phase (annotation/instrumentation only)
- Re-verified existing transaction controller and integration specs

**Review Status:** APPROVED

**Git Commit Message:**
chore: document transaction APIs and add spans

- add OpenAPI responses for credit and debit transaction endpoints
- instrument posting service methods with non-PII OpenTelemetry spans
- keep transaction contracts stable with full suite passing
