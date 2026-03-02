## Phase 3 Complete: Tighten and Generalize Over-Specialized Sections

Removed remaining domain-specific mandatory constraints from 4 instruction files. Replaced card-network-specific terminology, migration filenames, class names, and feature flags with generic equivalents. Security and compliance rigor preserved throughout.

**Files created/changed:**
- `.github/instructions/code-reviewer.instructions.md`
- `.github/instructions/database-jpa-patterns.instructions.md`
- `.github/instructions/event-driven-patterns.instructions.md`
- `.github/instructions/configuration-patterns.instructions.md`

**Functions created/changed:**
- N/A (documentation-only changes)

**Tests created/changed:**
- Content guard grep: `card|visa|mydebit|episode6|com.ytl|8583` → zero matches across all 4 files

**Review Status:** APPROVED (after minor revision to soften hardcoded retention duration to policy-based wording)

**Git Commit Message:**
```
chore: tighten over-specialized sections in instruction files

- Generalize Avro event names, tracing attributes, and crypto references
- Replace card-specific migration filenames and index examples
- Generalize feature flag and workflow class name examples
- Soften hardcoded retention duration to regulatory policy reference
```
