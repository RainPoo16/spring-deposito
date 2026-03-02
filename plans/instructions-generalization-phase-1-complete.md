## Phase 1 Complete: Normalize Scope and Remove Hard-Coded Product Identity

Replaced all card-service-specific identity language (`com.ytl.card`, Card Service, Episode6, Visa, MyDebit, SAN) with generic microservice patterns across 6 instruction files. All content guard checks pass with zero matches for legacy terms.

**Files created/changed:**
- `.github/instructions/api-design-patterns.instructions.md`
- `.github/instructions/code-reviewer.instructions.md`
- `.github/instructions/configuration-patterns.instructions.md`
- `.github/instructions/database-jpa-patterns.instructions.md`
- `.github/instructions/event-driven-patterns.instructions.md`
- `.github/instructions/java-spring-coding-standards.instructions.md`

**Functions created/changed:**
- N/A (documentation-only changes)

**Tests created/changed:**
- Content guard grep checks: `com.ytl.card`, `Card Service`, `Episode6`, `MyDebit`, `Visa`, `SAN`, `CardAccount`, `CardToken`, `card_account`, `card_transaction`, standalone `card` — all return zero matches

**Review Status:** APPROVED (after one revision to fix residual Visa SQL examples in database file)

**Git Commit Message:**
```
chore: generalize instruction files — remove card-service identity

- Replace com.ytl.card package paths with com.examples.deposit
- Replace Card Service terminology with generic microservice language
- Remove Episode6, Visa, MyDebit, SAN network-specific references
- Generalize entity examples (Card/CardAccount → Account/Transaction)
- Generalize SQL examples, config prefixes, and event names
```
