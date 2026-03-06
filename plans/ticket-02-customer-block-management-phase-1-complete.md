## Phase 1 Complete: Domain Contract and Rule Matrix

Implemented the Ticket 02 block domain vocabulary and lifecycle eligibility contracts with test-first coverage. The block code catalog metadata and deterministic create/update/cancel eligibility matrix are now encoded and verified by targeted domain specs.

**Files created/changed:**
- src/main/java/com/examples/deposit/domain/BlockCode.java
- src/main/java/com/examples/deposit/domain/BlockDirection.java
- src/main/java/com/examples/deposit/domain/BlockRequestedBy.java
- src/main/java/com/examples/deposit/domain/AccountBlockStatus.java
- src/main/java/com/examples/deposit/domain/exception/DuplicateOrOverlappingBlockException.java
- src/main/java/com/examples/deposit/domain/exception/BlockNotEligibleForOperationException.java
- src/main/java/com/examples/deposit/domain/exception/AccountNotFoundException.java
- src/test/groovy/com/examples/deposit/domain/BlockCodeSpec.groovy
- src/test/groovy/com/examples/deposit/domain/AccountBlockEligibilitySpec.groovy

**Functions created/changed:**
- `BlockCode#fromCode(String)`
- `BlockCode#isEligibleForCreateBy(BlockRequestedBy)`
- `AccountBlockStatus#isEligibleForUpdate(BlockRequestedBy, BlockCode)`
- `AccountBlockStatus#isEligibleForCancel(BlockRequestedBy, BlockCode)`

**Tests created/changed:**
- `BlockCodeSpec` metadata and parsing coverage for all four block codes
- `AccountBlockEligibilitySpec` full matrix coverage for create/update/cancel eligibility
- `AccountBlockEligibilitySpec` parity assertion for update vs cancel eligibility behavior

**Review Status:** APPROVED

**Git Commit Message:**
feat: add block domain contract and rules

- add typed block code catalog with direction and requester metadata
- add lifecycle eligibility methods for create, update, and cancel
- add matrix-driven domain specs for block code and rule coverage
