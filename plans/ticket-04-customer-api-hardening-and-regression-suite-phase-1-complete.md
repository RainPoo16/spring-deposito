## Phase 1 Complete: Contract Baseline and Gap Reproduction

Phase 1 established deterministic contract tests for Ticket 04 customer API hardening scenarios and strengthened RFC7807 assertions for error responses. The phase now has an approved baseline with targeted tests passing and no production code changes.

**Files created/changed:**
- src/test/groovy/com/examples/deposit/integration/DemandDepositAccountTransactionIntegrationSpec.groovy
- src/test/groovy/com/examples/deposit/integration/DemandDepositAccountBlockIntegrationSpec.groovy
- src/test/groovy/com/examples/deposit/controller/DemandDepositAccountControllerSpec.groovy
- plans/ticket-04-customer-api-hardening-and-regression-suite-phase-1-complete.md

**Functions created/changed:**
- foreign customer cannot post credit or debit to another customer's account
- credit and debit to non-existent account return 404 account-not-found
- same idempotency identity with changed payload returns 409 transaction-idempotency-conflict
- foreign customer cannot create block on another customer's account
- maps transaction failures to problem details with expected 4xx statuses

**Tests created/changed:**
- DemandDepositAccountTransactionIntegrationSpec.foreign customer cannot post credit or debit to another customer's account
- DemandDepositAccountTransactionIntegrationSpec.credit and debit to non-existent account return 404 account-not-found
- DemandDepositAccountTransactionIntegrationSpec.same idempotency identity with changed payload returns 409 transaction-idempotency-conflict
- DemandDepositAccountBlockIntegrationSpec.foreign customer cannot create block on another customer's account
- DemandDepositAccountControllerSpec.maps transaction failures to problem details with expected 4xx statuses

**Review Status:** APPROVED

**Git Commit Message:**
test: add ticket-04 contract baseline specs

- add ownership and account-not-found transaction integration tests
- add idempotency-conflict integration coverage for payload mismatch
- strengthen problem+json detail assertions for transaction failures
