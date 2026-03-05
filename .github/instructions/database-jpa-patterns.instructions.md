---
name: 'Database Jpa Patterns'
description: 'JPA entity design, repository patterns, UUID v7 usage, keyset pagination, optimistic locking, and query optimization'
applyTo: '**/repository/**/*.java, **/domain/**/*.java, **/*Repository.java, **/*Entity.java, **/db/migration/**/*.sql'
---

# Database and JPA Patterns

> **Based on Actual Implementation**: These patterns reflect established conventions for Spring Boot microservices. Examples use this repository's structure for illustration.

## Core Principles

1. **UUID v7 for all primary keys**: Use `GUID.v7().toUUID()` for time-ordered, monotonic identifiers that improve index locality and keyset pagination.
2. **Enum fields always use `EnumType.STRING`**: Never use ordinal mapping — it breaks silently on reordering.
3. **Zero-downtime migrations**: All DDL changes must use safe patterns (`NOT VALID`, `CONCURRENTLY`, multi-step type changes).
4. **Keyset pagination for large datasets**: Never use high `OFFSET` for backfills or batch processing — always paginate by `id > :lastId`.
5. **Row locks scoped to single rows**: Use `FOR UPDATE` on primary key only; use `SKIP LOCKED` for concurrent batch processors.

## PII Protection

All PII protection rules from `@.github/instructions/pii-protection.instructions.md` apply. Never log customer names, account numbers, tokens, or credentials. Only log UUIDs, correlation IDs, status codes, and system-generated metadata.

## JPA Entity Design

### Entity Classes
- Annotate domain aggregates with `@Entity` and explicit `@Table(name = "account")` etc.
- Implement `equals()` / `hashCode()` using immutable business keys when available; fall back to primary key only after persistence
- Primary key strategy: use UUID v7 (monotonic, time-ordered) to improve index locality and enable efficient keyset pagination
- UUID v7 Generation: we generate identifiers explicitly (not via JPA generator) using `com.github.f4b6a3.uuid.alt.GUID.v7().toUUID()` before persisting. Never mix auto generation and manual assignment for the same entity.

```java
import static com.github.f4b6a3.uuid.alt.GUID.v7;

@Entity
@Table(name = "account")
public class Account {
    @Id
    private UUID id;

    public static Account createNew(UUID customerId, AccountStatus status) {
        Account account = new Account();
        account.id = v7().toUUID(); // Monotonic UUID improves keyset pagination performance
        account.customerId = customerId;
        account.status = status;
        return account;
    }
}
```

### Entity Relationships
- Default to `FetchType.LAZY` for associations; eagerly load only small, immutable value objects
- Always define the owning side with `@JoinColumn` for clarity (e.g. `@ManyToOne @JoinColumn(name = "customer_id")`)
- Use `@ElementCollection` for small sets of value objects; otherwise explicit entity
- Use `@Embedded` for composable value objects (e.g. address, limits)

### Enum Mapping (CRITICAL)

**Always use `@Enumerated(EnumType.STRING)` for enum fields in JPA entities.**

**Why STRING over ORDINAL**:
- **Database readability**: `'ACTIVE'` is self-documenting; ordinal `0` is cryptic
- **Refactoring safety**: Reordering enum constants breaks ordinal mapping silently
- **Migration safety**: Adding enum values mid-list requires data migration with ordinals
- **Debugging**: Query results show meaningful values without reverse lookup

**✅ Correct Pattern**:
```java
@Entity
@Table(name = "account")
public class Account {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING) // REQUIRED
    @Column(name = "status", nullable = false)
    private AccountStatus status;
}
```

**❌ Never Use ORDINAL** (implicit default if `@Enumerated` omitted):
```java
@Entity
public class Account {
    private AccountStatus status; // WRONG: defaults to ORDINAL
    
    @Enumerated(EnumType.ORDINAL) // WRONG: explicit ordinal is fragile
    private AccountStatus status;
}
```

**Enum Definition Example**:
```java
public enum AccountStatus {
    PENDING,    // Stored as 'PENDING' in database
    ACTIVE,     // Stored as 'ACTIVE' in database
    SUSPENDED,  // Stored as 'SUSPENDED' in database
    CLOSED      // Stored as 'CLOSED' in database
}
```

**Database Schema**:
```sql
CREATE TABLE account (
    id UUID PRIMARY KEY,
    status VARCHAR(50) NOT NULL,  -- Stores 'ACTIVE', 'PENDING', etc.
    customer_id UUID NOT NULL
);
```

**Migration Safety Example**:
```java
// Adding new enum value is safe with STRING
public enum AccountStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    FROZEN,     // New value added mid-list
    CLOSED
}
// With EnumType.STRING: No data migration needed
// With EnumType.ORDINAL: Would break existing data (CLOSED was 3, now 4)
```

### Example Entity Pattern
```java
@Entity
@Table(name = "account")
public class Account {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING) // Always required for enums
    @Column(name = "status", nullable = false)
    private AccountStatus status;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
}
```

## Repository Layer

### Repository Interfaces
- Extend `JpaRepository<Entity, UUID>` for CRUD operations
- Add `JpaSpecificationExecutor<Entity>` when dynamic filtering + pagination is required (e.g. fraud transaction searches)
- Method naming for simple predicates; use `@Query` (JPQL or native) for complex joins / locking / keyset pagination
- Return `Optional<T>` for single-row lookups that may be absent

### Custom Queries & Native Use
- Prefer JPQL for portability; use native queries for:
  - Row-level locking (`FOR UPDATE`, `FOR UPDATE SKIP LOCKED`)
  - Keyset pagination based on UUID v7 ordering
  - Vendor-specific performance optimizations (indexes, hints)
- Include explicit column lists in large native queries rather than `SELECT *` for clarity (except simple locking selects)

### Example Repository Pattern
```java
public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByCustomerId(UUID customerId);
    List<Account> findByCustomerIdAndStatus(UUID customerId, AccountStatus status);

    @Query(value = "SELECT * FROM account a WHERE a.id = :id FOR UPDATE", nativeQuery = true)
    Optional<Account> findByIdForUpdate(UUID id);
}
```

## Specification Pattern

### Dynamic Filtering with Pagination
- Build composite `Specification<T>` in services for endpoints supporting multiple optional filters + pagination
- Start with `Specification.where(null)` then chain `.and(condition)` only when parameter non-null
- Keep each specification method tiny & single-responsibility; put them in `*Specification` class inside repository package (e.g. `TransactionRiskSpecification`)
- For pageable queries: call repository method like `findAll(spec, pageable)` (provided by `JpaSpecificationExecutor`) or custom signature returning `Page<T>`
- Favor `Page<T>` when total count is needed; use `Slice<T>` for performance when only next existence required
- Ensure criteria builder uses indexed columns for predicates; add indexes in migrations accordingly

### Example Specification Methods
```java
public static Specification<Transaction> hasCustomerId(UUID customerId) {
    return (root, query, cb) -> customerId == null ? cb.conjunction() : cb.equal(root.get("customerId"), customerId);
}
```

### Performance Tips
- Never use unnecessary `fetch` joins in specifications when simple predicates suffice; count query becomes expensive
- Always use `query.distinct(true)` when joining collections to prevent duplicate root results; verify count query
- Separate projection queries (DTO) for heavy list endpoints rather than serializing full aggregate

## Pagination Strategies

### Offset Pagination (Default)
- Use `Pageable` with page/size for small to moderate result sets (< ~10k rows scanned)
- Drawbacks at scale: high `OFFSET` values cause sequential scan beyond index usefulness

### Keyset Pagination (Preferred for Backfilling / Large Data)
- Use monotonic UUID v7 `id` ordering: query with `WHERE id > :lastId ORDER BY id ASC LIMIT :batchSize`
- Avoid OFFSET entirely; constant cost per page
- Store `lastId` from previous batch; skip gaps naturally
- Recommended batch size: 1000 (tune based on row size & network throughput)

#### Example Keyset Query (Account Backfill)
```java
@Query(value = """
                SELECT a.external_reference, a.id
                FROM account a
                WHERE a.category IS NULL
                    AND a.external_reference IS NOT NULL
                    AND a.id > :lastId
                ORDER BY a.id ASC
        LIMIT :limit
        """, nativeQuery = true)
List<Object[]> findRecordsWithNullCategoryAfter(UUID lastId, int limit);
```
Guidelines:
- First batch: pass minimal UUID (or use `00000000-0000-0000-0000-000000000000` sentinel) or a query variant without `id > :lastId`
- Convert `Object[]` to typed projection (create record: `record AccountBackfill(String externalReference, UUID id)` for readability)
- Ensure index exists on `account(id)` (primary key implicitly covers)

### Choosing Strategy
| Use Case | Strategy | Notes |
|----------|----------|-------|
| User-facing filtered list (page count needed) | Offset (`Page<T>`) | Provide total count for UI |
| Infinite scroll / large export | Keyset | Lower memory footprint |
| Backfill / migration tasks | Keyset | Deterministic progress tracking |
| Admin small lists (< 500 rows) | Offset or keyset | Either acceptable |

## Row-Level Locking & Concurrency

### Patterns
- Use native `SELECT ... FOR UPDATE` for immediate exclusive locks on a single row you intend to modify in same transaction
- Use `FOR UPDATE SKIP LOCKED` for concurrent workers processing queues (settlement reconciliation, task tables) to avoid blocking
- Encapsulate locking inside a `@Transactional` service method; repository method only fetches

### Examples
```java
@Query(value = "SELECT * FROM account a WHERE a.id = :id FOR UPDATE", nativeQuery = true)
Optional<Account> findByIdForUpdate(UUID id);

@Query(value = "SELECT * FROM settlement_reconciliation WHERE id = :id FOR UPDATE SKIP LOCKED", nativeQuery = true)
Optional<SettlementReconciliation> findByIdForUpdateSkipLocked(UUID id);
```

### Guidelines
- Keep locked section minimal (perform necessary validation + update + emit events)
- Avoid locking broad ranges; always lock by primary key or narrowly indexed predicate
- Prefer `SKIP LOCKED` for batch processors to reduce contention & deadlock risk
- Do not mix optimistic versioning (`@Version`) and manual `FOR UPDATE` on same entity without rationale

### Alternative (JPA API)
- `entityManager.find(Entity.class, id, LockModeType.PESSIMISTIC_WRITE)` when native query not needed; may emit vendor-specific SQL internally
- Use native query for multi-table row processing or projection at lock time

## Transaction Management

### Boundaries
- Annotate service layer (not repositories) with `@Transactional`
- Use `readOnly = true` for pure fetch + transform operations (no flush, slightly improved performance)
- Use `@Transactional(rollbackFor = Exception.class)` where lazy loaded associations may be traversed to ensure any unexpected runtime exception triggers rollback (and for broader safety in complex workflows)
- For backfill: wrap each batch in its own transaction to reduce long-held locks & memory
    - Pseudocode (see your backfill task implementation):
  ```java
  UUID lastId = UUID.fromString("00000000-0000-0000-0000-000000000000");
  while (true) {
      List<AccountBackfill> batch = repo.findRecordsWithNullCategoryAfter(lastId, 1000)
          .stream().map(r -> new AccountBackfill((String) r[0], (UUID) r[1])).toList();
      if (batch.isEmpty()) break;
      transactionTemplate.execute(status -> {
          batch.forEach(row -> process(row));
          return null;
      });
      lastId = batch.get(batch.size()-1).id();
      if (batch.size() < 1000) break;
  }
  ```
- Avoid nested transactions unless outbox/event patterns require `REQUIRES_NEW`

## Flyway Migration Scripts

### SQL Inline Comments (DDL Documentation)

**Always use simple inline comments (`--`) in Flyway migration scripts, NOT PostgreSQL's `COMMENT ON` statements.**

#### Comment Style

- Use `--` for inline comments (standard SQL comment syntax)
- Place comments on the line immediately before or after the relevant DDL element
- Keep comments concise and technical - explain purpose, business context, or non-obvious constraints
- Do NOT use `COMMENT ON TABLE/COLUMN` statements - these add unnecessary database metadata overhead

#### When to Add Comments

**Always comment:**

- Non-obvious column purposes (e.g., `token_requestor_id`, `awb_number`)
- Foreign key relationships that aren't immediately clear from naming
- Indexes with specific performance considerations

**Do NOT comment:**

- Self-explanatory columns (`id`, `created_at`, `updated_at`, `customer_id`)
- Standard audit fields
- Simple status enums (unless status transitions have special meaning)

#### Example Patterns

**Table Creation with Comments:**

```sql
-- Stores financial transaction data processed by the service
CREATE TABLE IF NOT EXISTS transaction (
    id UUID NOT NULL PRIMARY KEY,
    account_id UUID NOT NULL,
    transaction_type TEXT NOT NULL,
    transaction_amount DECIMAL(19, 4) NOT NULL,
    transaction_currency TEXT NOT NULL,
    settlement_status TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
```

**Adding Columns:**

```sql
-- Add tracking fields for physical delivery workflows
ALTER TABLE account
    ADD COLUMN awb_number TEXT,  -- Courier tracking number
    ADD COLUMN awb_courier TEXT;  -- Courier service provider (e.g., DHL, FedEx)
```

**Creating Indexes:**

```sql
-- Index for keyset pagination on reconciliation queries
CREATE INDEX IF NOT EXISTS idx_settlement_recon_id 
    ON settlement_reconciliation(id);

-- Composite index for customer transaction history lookups
CREATE INDEX IF NOT EXISTS idx_transaction_customer_created 
    ON transaction(customer_id, created_at DESC);
```

#### Anti-Patterns (Avoid These)

❌ **Don't use `COMMENT ON` statements:**

```sql
-- BAD: Adds database metadata overhead
COMMENT ON COLUMN transaction.reference_id 
    IS 'Stores the external reference identifier from the upstream provider.';
```

✅ **Use inline comments instead:**

```sql
-- GOOD: Simple, readable, no database overhead
ALTER TABLE transaction
    -- External reference identifier from upstream provider
    ADD COLUMN reference_id TEXT;
```

#### Migration File Naming

- Follow Flyway convention: `V{version}__{description}.sql`
- Example: `V1.82__add_token_requestor_columns.sql`
- Version increments sequentially; description is snake_case and descriptive

### Guidelines Summary

1. ✅ Use `--` for inline comments in SQL
2. ❌ Never use `COMMENT ON TABLE/COLUMN` statements
3. ✅ Comment non-obvious column
4. ❌ Don't comment self-explanatory fields (`id`, `created_at`, etc.)
5. ✅ Place comments on the line before or after the relevant DDL element
6. ✅ Keep comments concise and technical

## Value Objects and Embeddables

- Use `@Embeddable` for small immutable components (limits, address) and `@AttributeOverride` for multiple instances
- Validate invariant inside constructor/factory, not via setters
- For JSON columns (e.g. metadata): annotate with `@JdbcTypeCode(SqlTypes.JSON)` and ensure consistent ObjectMapper
  configuration

## Database Migrations

### ⚠️ CRITICAL: Migration Safety (zero-downtime patterns)

**Never write migrations with blocking DDL that can cause production outages.**

Database migrations containing unsafe SQL statements pose significant risks to production systems, including:
- Prolonged table locks that block application writes and queries
- Full table rewrites that consume excessive resources and time on large tables
- Blocking DDL operations that cause application unavailability during deployment
- Production incidents from unreviewed migration scripts reaching production

#### Unsafe Patterns (NEVER USE)

❌ **Foreign Key Constraints without NOT VALID**
```sql
-- UNSAFE: Requires full table scan with exclusive lock
ALTER TABLE account ADD CONSTRAINT fk_customer 
    FOREIGN KEY (customer_id) REFERENCES customer(id);
```

✅ **Safe Pattern: Add constraint as NOT VALID, then validate separately**
```sql
-- Step 1: Add constraint without validation (fast, minimal lock)
ALTER TABLE account ADD CONSTRAINT fk_customer 
    FOREIGN KEY (customer_id) REFERENCES customer(id) NOT VALID;

-- Step 2: Validate in separate migration (allows concurrent reads/writes)
ALTER TABLE account VALIDATE CONSTRAINT fk_customer;
```

❌ **Index Creation without CONCURRENTLY**
```sql
-- UNSAFE: Blocks all writes to the table
CREATE INDEX idx_customer_id ON account(customer_id);
```

✅ **Safe Pattern: Use CONCURRENTLY**
```sql
-- Safe: Allows concurrent writes (takes longer but non-blocking)
CREATE INDEX CONCURRENTLY idx_customer_id ON account(customer_id);
```

❌ **Unique Constraints without NOT VALID**
```sql
-- UNSAFE: Validates all rows with table lock
ALTER TABLE account ADD CONSTRAINT uk_account_number UNIQUE (account_number);
```

✅ **Safe Pattern: Create unique index concurrently, then constraint**
```sql
-- Step 1: Create unique index concurrently
CREATE UNIQUE INDEX CONCURRENTLY uk_account_number ON account(account_number);

-- Step 2: Add constraint using existing index (fast)
ALTER TABLE account ADD CONSTRAINT uk_account_number UNIQUE USING INDEX uk_account_number;
```

❌ **Column Type Changes**
```sql
-- UNSAFE: Rewrites entire table with exclusive lock
ALTER TABLE account ALTER COLUMN status TYPE VARCHAR(50);
```

✅ **Safe Pattern: Multi-step migration for type changes**
```sql
-- Step 1: Add new column
ALTER TABLE account ADD COLUMN status_new VARCHAR(50);

-- Step 2: Backfill data in batches (application code or manual script)
-- UPDATE account SET status_new = status::VARCHAR WHERE id > :lastId LIMIT 1000;

-- Step 3: Switch application to use new column (deploy code change)

-- Step 4: Drop old column (in later migration after verification)
ALTER TABLE account DROP COLUMN status;
ALTER TABLE account RENAME COLUMN status_new TO status;
```

❌ **Adding NOT NULL Constraints Directly**
```sql
-- UNSAFE: Full table scan to validate, may lock table
ALTER TABLE account ALTER COLUMN customer_id SET NOT NULL;
```

✅ **Safe Pattern: Add CHECK constraint first, then NOT NULL**
```sql
-- Step 1: Add CHECK constraint as NOT VALID (fast)
ALTER TABLE account ADD CONSTRAINT chk_customer_id_not_null 
    CHECK (customer_id IS NOT NULL) NOT VALID;

-- Step 2: Validate constraint (allows reads/writes)
ALTER TABLE account VALIDATE CONSTRAINT chk_customer_id_not_null;

-- Step 3: Add NOT NULL (PostgreSQL recognizes existing CHECK, fast operation)
ALTER TABLE account ALTER COLUMN customer_id SET NOT NULL;

-- Step 4: Drop redundant CHECK constraint
ALTER TABLE account DROP CONSTRAINT chk_customer_id_not_null;
```

❌ **Other Dangerous Operations**
```sql
-- NEVER use these in production migrations:
VACUUM FULL table_name;                           -- Rewrites table with exclusive lock
LOCK TABLE table_name IN ACCESS EXCLUSIVE MODE;   -- Blocks all access
ALTER TABLE ... SET TABLESPACE ...;               -- Rewrites entire table
CLUSTER table_name USING index_name;              -- Rewrites table with exclusive lock
```

#### Migration Authoring Checklist

Before committing any Flyway migration script (`V*.sql`), verify:

1. ✅ All `CREATE INDEX` statements include `CONCURRENTLY` keyword
2. ✅ Foreign key constraints added with `NOT VALID`, validated in separate migration
3. ✅ Unique constraints created via concurrent index first
4. ✅ Column type changes use multi-step pattern (new column → backfill → switch → drop)
5. ✅ NOT NULL constraints added after CHECK constraint validation
6. ✅ No `VACUUM FULL`, `LOCK TABLE`, or table rewrites
7. ✅ Migration tested on realistic data volume (not just empty test DB)
8. ✅ Migration header comment documents:
   - Purpose and affected tables
   - Expected lock duration and impact
   - Rollback strategy if applicable
   - Related repository methods / specifications

#### Migration Review Requirements

All migration PRs must include:
- **Impact Analysis**: Which tables locked, estimated duration, peak traffic consideration
- **Testing Evidence**: Executed on staging environment with production-like data volume
- **Rollback Plan**: How to revert if migration fails mid-deployment
- **Deployment Window**: Can this run during peak hours or requires maintenance window?

#### Example Safe Migration Header
```sql
-- V1.28__add_customer_id_index_for_account_filtering.sql
-- Purpose: Support AccountSpecification.hasCustomerId() filtering performance
-- Impact: Non-blocking index creation on account (~5M rows, ~3-5 min)
-- Tables: account (SHARE UPDATE EXCLUSIVE lock during index build, reads/writes allowed)
-- Rollback: DROP INDEX CONCURRENTLY idx_account_customer_id;
-- Safe for production deployment during business hours
```

### Schema Management
- Use Flyway with semantic incremental naming (e.g. `V1.21__add_account_limit_index.sql`) — prefix grows over time; keep descriptive suffix
- Document rationale for new indexes (especially supporting keyset & filtering specs) in migration comment header
- Add composite indexes aligning with spec predicates (e.g. `(customer_id, status)` for `findByCustomerIdAndStatus`)
- **ALWAYS use safe migration patterns above** to prevent production outages

### Index Strategy with UUID v7
- UUID v7 improves locality — B-Tree inserts less likely to cause page splits vs random UUID v4
- Still monitor index bloat and vacuum statistics
- Add partial indexes if large portion of rows filtered by status (e.g. `WHERE status = 'ACTIVATED'`)

#### Repository & Specification Driven Index Guidance
When adding new repository methods or specification predicates, evaluate indexing needs early:

1. Single-Column Query Methods
    - Method: `findByAccountId(UUID accountId)` => Ensure index on `account(account_id)` (often already primary/unique) no composite needed.
    - Method: `findByCustomerId(UUID customerId)` => Create index on `account(customer_id)` if high cardinality and frequent.

2. Evolving to Multi-Column Methods
    - Adding `findByAccountIdAndCustomerId(UUID accountId, UUID customerId)` usually DOES NOT require a new composite index if `account_id` is already highly selective (near unique). The existing `account_id` index will filter quickly then customerId predicate applies to tiny result set.
   - Create composite index only when both columns significantly reduce result size AND queries frequently filter by both with low selectivity of the leading column OR range queries / ordering rely on second column.

3. Composite Index Heuristics
   - Never create composite index when leading single-column index cardinality/selectivity > ~95% uniqueness.
    - Use composite when pattern involves frequent queries like `findByStatusAndLifecycleAndAccountId` and each predicate independently still yields large intermediate sets.
   - Keep column order: most selective first OR if queries include range condition (`BETWEEN transmission_at`) place equality predicates first, then range.

4. Specification Predicates
    - Each `root.get("field")` used in equality for frequent filters must correspond to an indexed column (see migration `V1.27__create_indexes_for_account_service.sql`).
   - Never wrap indexed columns in functions (e.g. `LOWER(field)`) unless a functional index exists; always store normalized values.

5. Redundant Index Cleanup
    - Periodically scan for composite indexes whose first column already unique (e.g. `(account_id, status)` when `account_id` unique) and drop them.
   - Use `EXPLAIN (ANALYZE, BUFFERS)` to validate planner uses intended index; if planner ignores composite, reconsider necessity.

6. Naming Consistency
   - Follow pattern: `idx_<table>_<columns>` in lower case separated by underscores.
    - For multi-column descending element include direction: `(id DESC)` as seen in `idx_account_txn_error_filtering`.

7. Migration Authoring
   - Document decision in migration header comment: why new index, expected queries (method names / spec names), estimated row reduction.
   - Always use `CREATE INDEX CONCURRENTLY` for large tables (already used in `V1.27` examples) to avoid write locks.

8. Monitoring
   - Track index usage via `pg_stat_user_indexes`; drop indexes with very low scan counts versus size.

Example Decision Flow:
```
Add method findByAccountId -> account_id already indexed (no action)
Add method findByAccountIdAndCustomerId -> evaluate selectivity: account_id unique, skip composite
Add method findByStatusAndLifecycleAndAccountId -> existing (status, lifecycle, account_id) composite beneficial (see migration) to avoid multi-filter sequential scan
```

## Performance & Monitoring Checklist
1. Large filtered endpoints use specifications + indexed columns
2. Backfill tasks adopt keyset pagination (no high OFFSET)
3. Row locks confined to single-row operations; long-running logic executes post-commit if possible
4. Batch size tuned (1000 default) and configurable via property if needed (`deposit.backfill.batch-size`)
5. Avoid N+1 selects in specification queries (inspect generated SQL)
6. Add integration tests for keyset pagination correctness (ordering, termination when empty batch)
7. Explicit rollback semantics on complex transactional methods (`rollbackFor = Exception.class`) where appropriate
8. Index additions justified (no redundant composite if high-selectivity single index exists)

## Anti-Patterns to Avoid
- Using `Page<T>` + high OFFSET for > millions of rows backfill
- Locking entire table or unbounded range without index predicate
- Eager fetch of large collections in specifications
- Returning `List<Object[]>` without mapping to clear projection type
- Mixed random + monotonic UUID strategies across tables (inconsistent pagination behavior)
- Relying on implicit rollback (no `rollbackFor`) when wide lazy graph traversals occur

## References

- `@.github/instructions/java-spring-coding-standards.instructions.md` — Java and Spring coding standards for services and repositories
- `@.github/instructions/pii-protection.instructions.md` — PII-safe entity, query, and logging practices
