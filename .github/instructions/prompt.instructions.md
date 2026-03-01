---
name: 'Prompt'
description: 'Guidelines for creating high-quality prompt files'
applyTo: '**/*.prompt.md'
---

# Prompt Files Guidelines

Instructions for creating effective and maintainable prompt files that guide the agent/runtime in delivering consistent, high-quality outcomes across any repository.

## Core Principles

- **Predictable behavior** — every prompt must produce consistent, repeatable outcomes across runs
- **Minimal permissions** — declare only the tools and capabilities the prompt requires; never request broad access
- **Self-contained clarity** — include all context, constraints, and success criteria within the prompt file itself
- **Imperative instructions** — write direct commands to the agent/tool (`Analyze`, `Generate`, `Validate`); never use advisory language
- **Portability** — design prompts to work across repositories without modification to core logic

## Frontmatter Requirements
- Use the latest prompt frontmatter fields and semantics:

- All prompt files in this repository must include `name`, `description`, `agent`, and `argument-hint`.

### Required Fields

| Field | Required | Guidance |
|-------|----------|----------|
| `name` | Yes | Use a lowercase kebab-case identifier derived from filename (for example, file `generate-pr-description.prompt.md` has name: `'generate-pr-description'`). Must be unique across all prompt files. |
| `description` | Yes | Add a short, action-oriented description of what the prompt does. |
| `agent` | Yes | Set execution agent to `ask`, `agent`, `plan`, or a custom agent name; define it explicitly and do not rely on implicit defaults. |
| `argument-hint` | Yes | Provide concise hint text for chat input so users know what to type. |

### Optional Fields

| Field | Required | Guidance |
|-------|----------|----------|
| `model` | No | Pin a model only when task quality depends on a specific model tier; otherwise inherit active model picker selection. |
| `tools` | No | Declare least-privilege tools only when needed; may include built-in tools, tool sets, MCP tools, extension-contributed tools, and `<server-name>/*` to include all tools from an MCP server. |

- Use consistent quoting (single quotes required) and keep one field per line for readability and clean diffs.

## File Naming and Placement
- Use kebab-case filenames ending with `.prompt.md` and store them under `.github/prompts/` unless your workspace standard specifies another directory.
- Provide a short filename that communicates the action (for example, `generate-readme.prompt.md` rather than `prompt1.prompt.md`).

## Body Structure
- Start with an `#` level heading that matches the prompt intent so it surfaces well in Quick Pick search.
- Organize content with predictable sections. Required baseline: `Mission` or `Primary Directive`, `Scope & Preconditions`, `Inputs`, `Workflow` (step-by-step), `Output Expectations`, and `Quality Assurance`.
- Adjust section names to fit the domain, but retain the logical flow: why → context → inputs → actions → outputs → validation.
- Reference related prompts or instruction files using relative links to aid discoverability.

## Input and Context Handling
- Use `${input:variableName[:placeholder]}` for required values and explain when the user must supply them. Always provide defaults or alternatives for every input.
- Call out contextual variables such as `${selection}`, `${file}`, `${workspaceFolder}` only when they are essential, and describe how the agent/tool must interpret them.
- Document how to proceed when mandatory context is missing (for example, "Request the file path and stop if it remains undefined").

## Tool and Permission Guidance
- Limit `tools` to the smallest set that enables the task. List them in the preferred execution order when the sequence matters.
- If the prompt inherits tools from agent context, document that relationship and state critical tool behaviors or side effects.
- Warn about destructive operations (file creation, edits, terminal commands) and include guard rails or confirmation steps in the workflow.

## Instruction Tone and Style
- Write in direct, imperative sentences targeted at the agent/tool (for example, “Analyze”, “Generate”, “Summarize”).
- Keep sentences short and unambiguous, following Google Developer Documentation translation best practices to support localization.
- Avoid idioms, humor, or culturally specific references; favor neutral, inclusive language.

## Output Definition
- Specify the format, structure, and location of expected results (for example, “Create `docs/adr/adr-XXXX.md` using the template below”).
- Include success criteria and failure triggers so the agent/tool knows when to halt or retry.
- Provide validation steps—manual checks, automated commands, or acceptance criteria lists—that reviewers can execute after running the prompt.

## Examples and Reusable Assets
- Embed Good/Bad examples or scaffolds (Markdown templates, JSON stubs) that the prompt must produce or follow.
- Maintain reference tables (capabilities, status codes, role descriptions) inline to keep the prompt self-contained. Update these tables when upstream resources change.
- Link to authoritative documentation instead of duplicating lengthy guidance.

## Quality Assurance Checklist
- [ ] Frontmatter fields are complete, accurate, and least-privilege.
- [ ] Frontmatter includes required keys (`name`, `description`, `agent`, `argument-hint`) with valid values.
- [ ] Frontmatter uses optional keys (`model`, `tools`, and additional metadata) only when necessary.
- [ ] Inputs include placeholders, default behaviours, and fallbacks.
- [ ] Workflow covers preparation, execution, and post-processing without gaps.
- [ ] Output expectations include formatting and storage details.
- [ ] Validation steps are actionable (commands, diff checks, review prompts).
- [ ] Security, compliance, and privacy policies referenced by the prompt are current.
- [ ] Prompt executes successfully in VS Code (`Chat: Run Prompt`) using representative scenarios.

## Maintenance Guidance
- Version-control prompts alongside the code they affect; update them when dependencies, tooling, or review processes change.
- Enforce required frontmatter fields immediately for new prompt files; migrate existing prompt files incrementally.
- Review prompts periodically to ensure tool lists, model requirements, and linked documents remain valid.
- Coordinate with other repositories: when a prompt proves broadly useful, extract common guidance into instruction files or shared prompt packs.

## Common Pitfalls

| Pitfall | Solution |
|---------|----------|
| Vague workflow steps that leave the agent/tool guessing | Define explicit step-by-step actions with clear input → output expectations |
| Overly broad tool permissions granting unnecessary access | Limit `tools` to the minimum set required; list in execution order |
| Missing validation steps so output quality is unchecked | Always include actionable success criteria and verification commands |
| Advisory language (`should`, `consider`, `try to`) | Use imperative commands (`Analyze`, `Generate`, `Validate`, `Must`) |

## References

- [Prompt Files Documentation](https://code.visualstudio.com/docs/copilot/customization/prompt-files#_prompt-file-format)
- [Awesome Copilot Prompt Files](https://github.com/github/awesome-copilot/tree/main/prompts)
- [Tool Configuration](https://code.visualstudio.com/docs/copilot/chat/chat-agent-mode#_agent-mode-tools)
- `@.github/instructions/instructions.instructions.md` — guidelines for instruction files
