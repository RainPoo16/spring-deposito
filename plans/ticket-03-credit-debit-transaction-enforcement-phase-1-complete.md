## Phase 1 Complete: Transaction Domain Contract and Validation Matrix

Implemented Ticket 03 domain primitives and balance mutation invariants with status-aware credit/debit enforcement. Transaction code policy behavior is centralized and now covered by focused domain tests, including normalization and pending-verification allowlist rules.

**Files created/changed:**
- src/main/java/com/examples/deposit/domain/DemandDepositAccountStatus.java
- src/main/java/com/examples/deposit/domain/DemandDepositAccount.java
- src/main/java/com/examples/deposit/domain/TransactionType.java
- src/main/java/com/examples/deposit/domain/TransactionValidationMode.java
- src/main/java/com/examples/deposit/domain/TransactionCodePolicy.java
- src/main/java/com/examples/deposit/domain/exception/TransactionBlockedException.java
- src/main/java/com/examples/deposit/domain/exception/InsufficientAvailableBalanceException.java
- src/main/java/com/examples/deposit/domain/exception/TransactionNotAllowedForAccountStatusException.java
- src/test/groovy/com/examples/deposit/domain/DemandDepositAccountTransactionSpec.groovy
- src/test/groovy/com/examples/deposit/domain/TransactionCodePolicySpec.groovy

**Functions created/changed:**
- DemandDepositAccount.applyCredit(BigDecimal, String)
- DemandDepositAccount.applyDebit(BigDecimal, String)
- DemandDepositAccount.requireEligibleStatus(TransactionType, String)
- TransactionCodePolicy.resolveValidationMode(String)
- TransactionCodePolicy.isAllowedForPendingVerification(TransactionType, String)

**Tests created/changed:**
- DemandDepositAccountTransactionSpec.credit increases current and available balances
- DemandDepositAccountTransactionSpec.debit decreases current and available balances
- DemandDepositAccountTransactionSpec.debit throws on insufficient available balance
- DemandDepositAccountTransactionSpec.credit status eligibility matrix
- DemandDepositAccountTransactionSpec.debit status eligibility matrix
- TransactionCodePolicySpec.validation mode required/bypass matrix
- TransactionCodePolicySpec.pending-verification allowlist behavior
- TransactionCodePolicySpec.code normalization behavior

**Review Status:** APPROVED

**Git Commit Message:**
feat: add transaction domain validation rules

- add balance mutation methods for credit and debit
- centralize transaction code policy and status checks
- add domain specs for matrix and normalization coverage
