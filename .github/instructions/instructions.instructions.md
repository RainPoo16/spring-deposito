---
name: 'Instructions'
description: 'Guidelines for creating high-quality custom instruction files'
applyTo: '**/*.instructions.md'
---

# Custom Instructions File Guidelines

This is the sole authoritative guide for creating, updating, and maintaining `.instructions.md` files in this repository. All instruction-authoring standards are centralized here.

---

## P0 Critical Rules

These rules prevent instructions from being silently ignored by Copilot. Apply them before all other guidance.

### Source of Truth

- **Code is the single source of truth.** When instructions conflict with actual implementation, code wins.
- Instructions document what exists, not what we wish existed.
- Always verify patterns against real files before documenting them as standards.
- Flag and update outdated guidance immediately — code overrides docs.

### Size and Focus

- **Target**: 300–800 lines per instruction file.
- **Hard limit**: 1,000 lines maximum. Beyond this, Copilot may ignore the file entirely.
- Focus on the top 10–20 most critical rules per domain.
- Remove low-value guidance that developers already follow consistently.
- Split large files by domain concern — never mix unrelated patterns.

### Anti-Fabrication

- Never document patterns not verified in the actual codebase.
- Never use placeholder code or TODO comments in examples.
- Never invent file paths — verify they exist before referencing them.

### Conflict Prevention

- Review all instructions for contradictions; resolve explicitly.
- When conflicting patterns exist, prioritize patterns in newer files or files with higher test coverage.
- Never introduce patterns not found in the existing codebase.

### Context-Window Optimization

- Avoid duplication across instruction files and global guidance.
- Use imperative language: "Always", "Never", "Must" — not "Consider", "Try to", "Maybe".
- Write specific, actionable rules with examples — not vague guidance like "write clean code".
- Structure instructions hierarchically with clear headings for easy scanning.

---

## P1 Required: File Structure and Frontmatter

### Required YAML Frontmatter

Every instruction file must begin with YAML frontmatter containing these three required fields:

```yaml
---
name: 'Title Case Name'
description: 'Brief description of purpose and scope (1-500 characters)'
applyTo: 'glob pattern for target files'
---
```

### Frontmatter Rules

**Required fields** (every instruction file must include all three):

- **name**: Title Case identifier derived from filename (e.g., file `database-jpa-patterns.instructions.md` has name: `'Database Jpa Patterns'`). Must be unique across all instruction files.
- **description**: Single-quoted string, 1-500 characters, clearly stating purpose. Keep concise.
- **applyTo**: Glob pattern(s) specifying which files trigger this instruction.
  - Single pattern: `'**/*.java'`
  - Multiple patterns: `'**/*.java, **/*.groovy'`
  - Scoped pattern: `'**/controller/**/*.java'`
  - All files: `'**'`

**Optional fields:**

- **excludeAgent**: Array of agents to exclude from receiving this instruction. Values: `"coding-agent"`, `"code-review"`. Omit to apply to all agents.

### Glob Pattern Reference

| Pattern | Matches |
|---------|---------|
| `**/*.java` | All Java files |
| `**/*Test.groovy` | All Groovy test files |
| `**/controller/**/*.java` | Java files in controller packages |
| `**/test/**/*.groovy` | Groovy files in test directories |
| `**/db/migration/**/*.sql` | SQL migration files |
| `**` | All files (repository-wide) |

### File Naming and Location

- **Convention**: lowercase with hyphens (e.g., `database-jpa-patterns.instructions.md`)
- **Location**: `.github/instructions/` directory
- **Naming pattern**: `[domain]-[concern].instructions.md`

---

## P1 Required: Content and Writing Standards

### Domain Scope

One instruction file = one domain. Never mix unrelated concerns.

✅ **Good domain boundaries**:
- `database-jpa-patterns.instructions.md` — JPA, repositories, entities
- `api-design-patterns.instructions.md` — REST APIs, OpenAPI, controllers
- `event-driven-patterns.instructions.md` — Kafka, Debezium, Avro schemas

❌ **Bad — mixed concerns**:
- `backend-everything.instructions.md` — Database + API + Testing + Events

### File References

- Use `@` prefix for documentation/config file references (e.g., `@.github/instructions/*.instructions.md`, `@README.md`).
- Source code paths (`src/main/java/...`) do not need the `@` prefix.
- Always provide full file paths to real implementations:
  - ✅ "See `src/main/java/com/examples/deposit/service/account/AccountService.java` for implementation details"
  - ❌ "See AccountService for implementation"

### Examples

- Use minimal, clear examples: 5–15 lines showing only the core pattern.
- After examples, link to actual implementation files for full context.
- No PII in examples — use only UUIDs, correlation IDs, system-generated metadata.

```java
// Event publishing via outbox pattern
@Transactional
public void activateAccount(UUID accountId) {
    Account account = accountRepository.findById(accountId)
        .orElseThrow(() -> new AccountNotFoundException(accountId));
    account.activate();
    accountRepository.save(account);
    eventOutboxService.publish(new AccountActivatedEvent(account.getId()));
}
```

**Reference**: `src/main/java/com/examples/deposit/service/account/AccountService.java`

### Writing Style

- Write in imperative mood: "Use", "Always", "Never", "Must".
- Be specific and actionable — avoid "should", "might", "consider".
- Use bullet points and tables for scannability.
- Show anti-patterns alongside correct patterns.

### Recommended Sections

A well-structured instruction file follows this order:

1. **Title and Overview** — 1–2 sentences describing the domain
2. **Core Principles** — 3–5 high-level guiding bullets
3. **Implementation Patterns** — With examples (5–15 lines) and anti-patterns
4. **Testing Requirements** — Domain-specific testing guidance
5. **Common Pitfalls** — With concrete solutions
6. **References** — Links to related instruction files and architecture docs

---

## P2 Quality: Common Pitfalls and Prevention

### Issue 1: Instructions Are Ignored

**Symptoms**: Copilot doesn't follow the instructions.

**Root Causes**:
- File exceeds 1,000 lines
- Vague, ambiguous guidance (e.g., "improve code quality")
- Conflicting rules within same file or across files

**Prevention**:
- Keep files under 1,000 lines (target: 300–800)
- Write specific, actionable rules with concrete examples
- Review all instructions for conflicts; resolve explicitly
- Split large files by domain

### Issue 2: Language-Specific Rules Applied to Wrong Files

**Symptoms**: Java rules applied to Groovy tests, or vice versa.

**Root Causes**:
- Missing `applyTo` frontmatter in path-specific files
- Language-specific rules placed in repository-wide `@.github/copilot-instructions.md`

**Prevention**:
- Always use `applyTo` frontmatter in path-specific instruction files
- Place language-specific rules in dedicated files:
  - Java → `java-spring-coding-standards.instructions.md` with `applyTo: '**/*.java'`
  - Event handlers → `event-driven-patterns.instructions.md` with `applyTo: '**/handler/**/*.java'`
- Keep `@.github/copilot-instructions.md` for repository-wide, language-agnostic guidance

### Issue 3: Inconsistent Behavior Across Reviews

**Symptoms**: Copilot applies instructions differently each time.

**Root Causes**:
- Too many instructions (cognitive overload)
- Lack of specificity (leaves room for interpretation)
- Natural variability in AI responses

**Prevention**:
- Prioritize top 10–20 most critical rules per domain
- Add concrete examples to clarify intent:
  - ❌ Vague: "Use proper error handling"
  - ✅ Specific: "Catch specific exceptions and wrap in `AccountServiceException` with error codes from `ErrorCode` enum"
- Accept normal AI variability — focus on making critical rules unambiguous
- Use imperative language and hierarchically structured headings

### Best Practices Summary

| ✅ Do This | ❌ Not This |
|-----------|-------------|
| Keep instruction files under 1,000 lines | Write monolithic files with thousands of lines |
| Use `applyTo` frontmatter for path-specific rules | Put Java rules in repository-wide instructions |
| Write specific, actionable instructions with examples | Write vague guidance like "follow best practices" |
| Prioritize top 10–20 critical rules per domain | Include every possible rule and pattern |
| Use imperative language ("Always", "Never", "Must") | Use wishy-washy language ("Consider", "Try to", "Maybe") |
| Provide concrete code examples (5–15 lines) | Describe patterns in prose without examples |
| Review for conflicts and resolve them explicitly | Ignore contradictions and hope for the best |
| Split by domain/concern (one file per topic) | Mix unrelated patterns in the same file |

---

## Optimization Strategy (When File Exceeds 1,000 Lines)

### Step 1: Identify Redundancy
Scan for repeated patterns, duplicate examples, and verbose explanations that can be shortened.

### Step 2: Prioritize by Impact
Categorize rules:
- 🔴 **Critical**: Security, PII protection, financial accuracy → KEEP
- 🟡 **Important**: Performance, maintainability, consistency → CONDENSE
- 🟢 **Nice-to-have**: Style preferences, minor conventions → CONSIDER REMOVING

### Step 3: Extract to Separate Files
If the file covers multiple domains, split by distinct concern. Update `@.github/copilot-instructions.md` to reference new files.

### Step 4: Condense Examples
Replace multiple similar examples with one clear example (5–15 lines) plus a link to a real implementation.

### Step 5: Remove Low-Value Rules
Remove rules developers already follow consistently, overly obvious guidance, patterns not used in the codebase, and outdated rules conflicting with current code.

---

## Technology and Version Grounding

Before documenting patterns in any instruction file:

- Detect exact language and framework versions from project files (`pom.xml`, `package.json`, etc.).
- Never use features beyond the detected versions.
- Scan the codebase for actual patterns before prescribing new ones.
- Prioritize consistency with existing code over external "best practices".
- When conflicting patterns exist, prioritize patterns in newer files or files with higher test coverage.

---

## Definition of Done Checklist

### Content Quality
- [ ] Every pattern includes a concrete example (5–15 lines)
- [ ] Examples link to actual files with full paths
- [ ] No vague language ("improve", "clean", "best practices" without specifics)
- [ ] Imperative language used ("Always", "Never", "Must")
- [ ] No conflicting rules within file or with other instruction files
- [ ] No PII in examples

### Technical Accuracy
- [ ] Patterns verified against actual codebase implementation
- [ ] File paths exist and are correct
- [ ] Examples follow project conventions
- [ ] No placeholder code or TODO comments in examples
- [ ] Technology/framework versions grounded in project reality

### Structure and Scope
- [ ] File under 1,000 lines (target: 300–800)
- [ ] Clear domain focus (one file = one domain)
- [ ] Appropriate `applyTo` frontmatter for path-specific files
- [ ] Logical section organization with clear headings
- [ ] Cross-references to related instruction files

### Context Footprint
- [ ] No duplication with other instruction files or global guidance
- [ ] Top 10–20 most critical rules prioritized
- [ ] No guidance that developers already follow consistently
- [ ] Content scannable and hierarchically structured

### Self-Validation Questions
- [ ] Would a new developer know exactly what to do from this file?
- [ ] Can every pattern be verified in the codebase?
- [ ] Would this file actually help Copilot generate better code?

---

## Maintenance Workflow

### When to Update Instructions

1. **Code pattern changes** — implementation approach evolves
2. **New features** — patterns emerge requiring documentation
3. **Conflict discovered** — instructions contradict actual code
4. **File size growing** — approaching 1,000-line limit
5. **Reviewer feedback** — Copilot missed an important pattern

### Update Process

1. Read entire instruction file to understand current state
2. Verify patterns against actual codebase (code is source of truth)
3. Update conflicting sections to match code reality
4. Add new patterns with concrete examples and file references
5. Check file size — optimize if approaching 1,000 lines
6. Validate against Definition of Done checklist before committing

### Integration with Repository Standards

Before creating or updating instruction files, reference:

1. **`@.github/copilot-instructions.md`** — Repository-wide standards and file pattern mapping
2. **`@README.md`** — Project overview and setup instructions

---

## Example Structure (Minimal Template)

Use this template to bootstrap new instruction files:

```markdown
---
name: 'Domain Patterns'
description: 'Brief description of purpose'
applyTo: '**/*.ext'
---

# [Domain] Patterns

Brief introduction and context (1-2 sentences).

## Core Principles

- Guiding principle 1
- Guiding principle 2
- Guiding principle 3

## Implementation Patterns

### Pattern 1: [Name]

**When to Use**: [Clear trigger/condition]

```language
// Minimal example (5-15 lines)
code example
```

**Reference**: `src/main/java/com/examples/deposit/service/account/AccountService.java`

**Anti-Pattern**:
```language
// What NOT to do
```

## Testing Requirements

- Domain-specific testing guidance

## Common Pitfalls

- ❌ Pitfall: [Description]
  - ✅ Solution: [How to avoid]

## References

- Related: `@.github/instructions/java-spring-coding-standards.instructions.md`
- Project: `@README.md`
```

---

## Additional Resources

- [Custom Instructions Documentation](https://code.visualstudio.com/docs/copilot/customization/custom-instructions)
