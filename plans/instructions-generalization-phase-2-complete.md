## Phase 2 Complete: Repair Broken References and Localize to Existing Repository

Removed or replaced all references to non-existent files (ARCHITECTURE.md, testing-patterns.instructions.md, cron-task-patterns.instructions.md, pagination-endpoint-design-patterns.instructions.md) across all instruction files. Updated illustrative examples in the instruction-authoring guide to use existing files. Fixed source code paths to match actual repository structure.

**Files created/changed:**
- `.github/instructions/api-design-patterns.instructions.md`
- `.github/instructions/code-reviewer.instructions.md`
- `.github/instructions/configuration-patterns.instructions.md`
- `.github/instructions/database-jpa-patterns.instructions.md`
- `.github/instructions/java-spring-coding-standards.instructions.md`
- `.github/instructions/instructions.instructions.md`
- `.github/instructions/pii-protection.instructions.md`

**Functions created/changed:**
- N/A (documentation-only changes)

**Tests created/changed:**
- Reference integrity grep: `ARCHITECTURE.md|testing-patterns|cron-task-patterns|pagination-endpoint|com/ytl/card|related-file` → zero matches across all instruction files

**Review Status:** APPROVED (after revision to fix template example reference and source code paths in instructions.instructions.md)

**Git Commit Message:**
```
chore: repair broken references in instruction files

- Remove references to non-existent ARCHITECTURE.md
- Remove references to non-existent testing/cron/pagination instruction files
- Update illustrative examples to use existing instruction file names
- Fix source code paths to match actual repository structure
```
