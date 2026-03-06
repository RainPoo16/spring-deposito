## Phase 2 Complete: Database Schema and Repository Support

Implemented block persistence infrastructure with Flyway schema, JPA mapping, and repository queries for overlap detection and ownership-safe account lookup. Repository slice tests now verify enum string persistence, overlap semantics for active/pending blocks, cancelled-block exclusion, and account+customer ownership filtering.

**Files created/changed:**
- src/main/resources/db/migration/V2__create_demand_deposit_account_block_tables.sql
- src/main/java/com/examples/deposit/domain/DemandDepositAccountBlock.java
- src/main/java/com/examples/deposit/repository/DemandDepositAccountBlockRepository.java
- src/main/java/com/examples/deposit/repository/DemandDepositAccountRepository.java
- src/test/groovy/com/examples/deposit/repository/DemandDepositAccountBlockRepositorySpec.groovy
- src/test/groovy/com/examples/deposit/repository/DemandDepositAccountRepositorySpec.groovy

**Functions created/changed:**
- `DemandDepositAccountBlock#create(...)`
- `DemandDepositAccountBlockRepository#existsOverlappingActiveOrPendingByAccountAndCode(...)`
- `DemandDepositAccountRepository#findByIdAndCustomerId(UUID, UUID)`

**Tests created/changed:**
- `DemandDepositAccountBlockRepositorySpec` for enum persistence and overlap checks
- `DemandDepositAccountRepositorySpec` ownership lookup coverage for `findByIdAndCustomerId`

**Review Status:** APPROVED

**Git Commit Message:**
feat: add block persistence and overlap queries

- add Flyway migration and JPA entity for account block records
- add repository overlap query for active and pending block conflicts
- add repository specs for overlap semantics and ownership lookup
