## Phase 5 Complete: End-to-End Integration, Replay Safety, and Regression Coverage

Implemented end-to-end Ticket 03 integration coverage across block, status, balance, happy-path posting, and replay idempotency behavior. Integration tests now assert invariants for rejected transactions (no balance mutation, no duplicate persistence) and regression safety with full-suite green.

**Files created/changed:**
- src/test/groovy/com/examples/deposit/integration/DemandDepositAccountTransactionIntegrationSpec.groovy

**Functions created/changed:**
- DemandDepositAccountTransactionIntegrationSpec credit/debit helper and scenario methods for block/status/balance/replay assertions

**Tests created/changed:**
- DemandDepositAccountTransactionIntegrationSpec.active debit block rejects debit with unchanged balances
- DemandDepositAccountTransactionIntegrationSpec.active credit block rejects credit with unchanged balances
- DemandDepositAccountTransactionIntegrationSpec.insufficient available balance rejects debit with unchanged balances
- DemandDepositAccountTransactionIntegrationSpec.eligible account credit/debit mutate balances correctly
- DemandDepositAccountTransactionIntegrationSpec.replay idempotency prevents duplicate posting and mutation
- DemandDepositAccountTransactionIntegrationSpec.pending-verification debit rejected with unchanged persistence counts
- DemandDepositAccountTransactionIntegrationSpec.pending-verification allowlisted credit succeeds and disallowed credit fails

**Review Status:** APPROVED

**Git Commit Message:**
test: add transaction integration regression scenarios

- add end-to-end block, status, balance, and replay safety tests
- assert rejected transactions keep balances and counts unchanged
- verify full suite remains green after transaction coverage expansion
