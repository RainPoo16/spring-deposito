# Plan: Generalize Instruction Files for Repository Migration

**Created:** 2026-03-02
**Status:** Ready for Atlas Execution

## Summary

Current instruction files in `.github/instructions/` are heavily coupled to a prior card-service codebase (`com.ytl.card`, Episode6, Visa/MyDebit/SAN, and non-existent references). This plan generalizes those files so they are reusable in this repository and future repositories, while preserving strong engineering standards (PII, security, Spring/JPA, API design). The implementation is documentation-only and focuses on replacing product-specific mandates with domain-agnostic rules, valid local references, and neutral examples.

## Context & Analysis

**Relevant Files:**
- `.github/instructions/api-design-patterns.instructions.md`: Contains card-specific package paths, endpoint examples, and references to missing files.
- `.github/instructions/code-reviewer.instructions.md`: Entire review framework is card-domain specific and references missing architecture/docs.
- `.github/instructions/configuration-patterns.instructions.md`: Card-specific profiles, prefixes, and integrations in examples.
- `.github/instructions/database-jpa-patterns.instructions.md`: Card entity/table examples and repository guidance tied to prior domain.
- `.github/instructions/event-driven-patterns.instructions.md`: Card event names, topics, and package conventions.
- `.github/instructions/java-spring-coding-standards.instructions.md`: Uses `com.ytl.card` naming and card-specific examples.
- `.github/instructions/instructions.instructions.md`: Includes card-specific sample paths/classes for authoring guidance.
- `.github/instructions/prompt.instructions.md`: Mostly generic; verify consistency only.
- `.github/instructions/pii-protection.instructions.md`: Mostly reusable; keep as authoritative source with minimal changes.

**Key Findings:**
- References to files that do not exist in this repo: `ARCHITECTURE.md`, `testing-patterns.instructions.md`, `cron-task-patterns.instructions.md`, `pagination-endpoint-design-patterns.instructions.md`.
- References to package paths that do not exist: `src/main/java/com/ytl/card/...`.
- Overly specific external providers and payment rails create misleading guidance for non-card repositories.
- Some guidance is still valid but should be reframed as “example patterns” rather than “mandatory card-service behavior”.

**Dependencies:**
- VS Code instruction frontmatter contract (`name`, `description`, `applyTo`) must remain valid.
- Existing repo package baseline appears to be `src/main/java/com/examples/deposit/`.
- PII policy must retain precedence over all other instruction files.

**Patterns & Conventions to Preserve:**
- Imperative style (`Always`, `Never`, `Must`) and high-signal bullet/checklist format.
- Spring layering, transaction boundaries, DTO validation, and observability principles.
- Zero-downtime migration patterns and idempotent event consumption concepts.

## Implementation Phases

### Phase 1: Normalize Scope and Remove Hard-Coded Product Identity

**Objective:** Replace card-repository identity language with generic microservice language in all instruction files.

**Files to Modify/Create:**
- `.github/instructions/api-design-patterns.instructions.md`
- `.github/instructions/code-reviewer.instructions.md`
- `.github/instructions/configuration-patterns.instructions.md`
- `.github/instructions/database-jpa-patterns.instructions.md`
- `.github/instructions/event-driven-patterns.instructions.md`
- `.github/instructions/java-spring-coding-standards.instructions.md`

**Tests to Write:**
- Content guard check: no `com.ytl.card`, `Card Service`, `Episode6`, `MyDebit`, `Visa`, `SAN` (unless explicitly marked as optional examples).

**Steps:**
1. Add/adjust intro sections so each file states “generic patterns derived from current repository conventions”.
2. Replace package/path examples with neutral placeholders or repository-valid examples.
3. Convert domain-specific mandatory rules to domain-agnostic rules with optional examples.
4. Keep critical non-domain controls unchanged (PII, security, resilience, transaction safety).
5. Run content guard grep checks and fix misses.

**Acceptance Criteria:**
- [ ] No hard-coded card-service identity remains as mandatory guidance.
- [ ] All six files remain coherent and actionable.
- [ ] All content guard checks pass.

---

### Phase 2: Repair Broken References and Localize to Existing Repository

**Objective:** Remove/replace references to non-existent files and dead links.

**Files to Modify/Create:**
- `.github/instructions/api-design-patterns.instructions.md`
- `.github/instructions/code-reviewer.instructions.md`
- `.github/instructions/configuration-patterns.instructions.md`
- `.github/instructions/database-jpa-patterns.instructions.md`
- `.github/instructions/java-spring-coding-standards.instructions.md`
- `.github/instructions/instructions.instructions.md`

**Tests to Write:**
- Reference integrity check: every `@...` reference in instruction files points to an existing file in this repo.

**Steps:**
1. Collect all `@...` references across `.github/instructions/*.instructions.md`.
2. Remove or replace missing references with existing files.
3. Ensure every retained reference resolves in the current repository.
4. Reword examples that point to old paths/classes.
5. Re-run integrity checks.

**Acceptance Criteria:**
- [ ] No references to missing docs/instructions remain.
- [ ] Reference style remains consistent with `instructions.instructions.md`.
- [ ] Instruction files are self-contained enough for reuse.

---

### Phase 3: Tighten and Generalize Over-Specialized Sections

**Objective:** Reduce card-only deep detail while keeping strong review/security standards.

**Files to Modify/Create:**
- `.github/instructions/code-reviewer.instructions.md` (primary heavy rewrite)
- `.github/instructions/event-driven-patterns.instructions.md`
- `.github/instructions/database-jpa-patterns.instructions.md`
- `.github/instructions/configuration-patterns.instructions.md`

**Tests to Write:**
- Section consistency check: each file retains clear sections (core principles, implementation patterns, validation/review checklist, references).

**Steps:**
1. Replace card-network/vendor-specific checklists with generalized financial/backend security patterns.
2. Convert concrete rail/provider requirements to optional examples block.
3. Keep compliance-critical controls (PII, encryption, least privilege, audit trail) as generic mandatory rules.
4. Trim excessively niche sections that do not apply cross-repo.
5. Verify readability and line-count guidance (target 300–800 lines where practical).

**Acceptance Criteria:**
- [ ] `code-reviewer.instructions.md` no longer assumes card-domain architecture.
- [ ] Event/database/config files use reusable patterns without false constraints.
- [ ] Security/compliance rigor remains intact.

---

### Phase 4: Final Consistency Pass and Validation Evidence

**Objective:** Ensure all instruction files are internally consistent, valid, and migration-ready.

**Files to Modify/Create:**
- `.github/instructions/*.instructions.md` (final touch-ups only)

**Tests to Write:**
- Frontmatter validation check (`name`, `description`, `applyTo` present and syntactically valid).
- Keyword regression scan for legacy identity terms.
- Duplicate/contradiction scan for high-risk rules.

**Steps:**
1. Validate YAML frontmatter in all instruction files.
2. Run keyword scans for legacy terms and dead references.
3. Resolve any contradictory rules discovered in cross-file review.
4. Produce a concise migration summary of what was generalized and why.
5. Confirm no non-instruction files were modified.

**Acceptance Criteria:**
- [ ] All instruction files pass frontmatter and keyword checks.
- [ ] No broken references or old-domain assumptions remain.
- [ ] Changes are limited to `.github/instructions/*.instructions.md` only.

## Open Questions

1. How aggressive should generalization be?
   - **Option A:** Fully domain-neutral (no card-specific examples at all).
   - **Option B:** Domain-neutral rules + optional “card-style example” callouts.
   - **Recommendation:** Option B for continuity and easier onboarding while remaining reusable.

2. Should references prefer neutral placeholders or current repository package paths?
   - **Option A:** Neutral placeholders (e.g., `com.example.<service>`).
   - **Option B:** Current repo paths (`com.examples.deposit`) for immediate local relevance.
   - **Recommendation:** Option B in examples, with wording that they are illustrative.

3. Should the very large reviewer file be split now?
   - **Option A:** Keep single file and generalize in place.
   - **Option B:** Split by concern (security, architecture, compliance) now.
   - **Recommendation:** Option A now (minimal change risk), split later in a dedicated cleanup task.

## Risks & Mitigation

- **Risk:** Over-generalization removes useful domain-specific safeguards.
  - **Mitigation:** Preserve mandatory security/compliance controls; only demote vendor/rail specifics.
- **Risk:** Broken references remain after edits.
  - **Mitigation:** Add explicit reference-integrity scan in Phase 2 and Phase 4.
- **Risk:** File tone becomes vague after simplification.
  - **Mitigation:** Keep imperative language and concrete examples in every major section.

## Success Criteria

- [ ] All `.github/instructions/*.instructions.md` files are repository-agnostic and reusable.
- [ ] No card-repository-specific package paths/vendors remain as hard requirements.
- [ ] All internal file references resolve in this repository.
- [ ] Security and PII protections remain explicit and enforceable.
- [ ] Validation checks and summary evidence are included in implementation handoff.

## Notes for Atlas

- Keep edits surgical: do not change `applyTo` scope unless required for correctness.
- Prefer rewriting examples over deleting guidance to preserve instructional value.
- Treat `pii-protection.instructions.md` as baseline authority and avoid weakening any rule.
- If any requirement is ambiguous during execution, choose the simpler generalized wording and record rationale in the summary.
