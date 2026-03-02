## Plan Complete: Instruction File Generalization Migration

Generalized all `.github/instructions/*.instructions.md` files from a prior card-service codebase (`com.ytl.card`, Episode6, Visa/MyDebit) to be repository-agnostic and reusable. The 9 instruction files now use `com.examples.deposit` as illustrative examples, contain no hard-coded product/vendor identity, reference only files that exist in the repo, and preserve full security/PII/compliance rigor.

**Phases Completed:** 4 of 4
1. ✅ Phase 1: Normalize Scope / Remove Hard-Coded Product Identity
2. ✅ Phase 2: Repair Broken References
3. ✅ Phase 3: Tighten Over-Specialized Sections
4. ✅ Phase 4: Final Consistency Pass and Validation Evidence

**All Files Created/Modified:**
- `.github/instructions/api-design-patterns.instructions.md`
- `.github/instructions/code-reviewer.instructions.md`
- `.github/instructions/configuration-patterns.instructions.md`
- `.github/instructions/database-jpa-patterns.instructions.md`
- `.github/instructions/event-driven-patterns.instructions.md`
- `.github/instructions/instructions.instructions.md`
- `.github/instructions/java-spring-coding-standards.instructions.md`
- `.github/instructions/pii-protection.instructions.md`
- `.github/instructions/prompt.instructions.md` (not modified — already generic)

**Key Changes Made:**
- Replaced all `com.ytl.card` package references with `com.examples.deposit`
- Removed Episode6, Visa, MyDebit, SAN, ISO 8583 vendor/product references
- Generalized entity names: `CardAccount`→`Account`, `CardToken`→generic, `CardTransaction`→`Transaction`
- Removed broken references to non-existent files: `ARCHITECTURE.md`, `testing-patterns.instructions.md`, `cron-task-patterns.instructions.md`, `pagination-endpoint-design-patterns.instructions.md`
- Softened Avro event naming, tracing attribute keys, crypto references, and migration filename patterns
- Replaced hardcoded "7 years" retention with policy-based language
- Preserved all PII protection rules intact (card data references kept as legitimate PII categories)

**Test Coverage:**
- Frontmatter validation: All 9 files have required YAML fields ✅
- Legacy keyword regression: Zero matches across all files ✅
- Broken reference scan: Zero matches across all files ✅
- Line count verification: All files 108–609 lines (under 1000 limit) ✅
- Non-instruction file integrity: No files outside `.github/instructions/` modified ✅
- All tests passing: ✅

**Recommendations for Next Steps:**
- If the repo adds new domain entities or packages, update the illustrative examples in instruction files accordingly
- Consider creating `testing-patterns.instructions.md` if testing conventions grow beyond what's in `java-spring-coding-standards.instructions.md`
- The `prompt.instructions.md` file was already generic and untouched — review if it needs project-specific examples
