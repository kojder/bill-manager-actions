# Active Work Tracker

Persistent working board for tracking current task and ad-hoc ideas.
Read at the start of every conversation (referenced from CLAUDE.md).

---

## Current Task

### [W-1] Task 18: Category Classification — PurchaseCategory Enum + Enhanced Prompt
**Source:** tasks.md Task 18
**Branch:** `feat/task-18-category-enum`
**Status:** In Progress

#### Steps
- [x] Create `PurchaseCategory` enum with `@JsonValue`/`@JsonCreator`
- [x] Change `BillAnalysisResult.categoryTags` from `List<String>` to `List<PurchaseCategory>`
- [x] Enhance `SYSTEM_PROMPT` with category taxonomy and multi-tag instruction
- [x] Create `PurchaseCategoryTest` (10 tests)
- [x] Update 6 existing test files
- [x] Verify: spotless:apply + checkstyle:check + test (161 tests, 0 failures)
- [x] Update documentation (api-plan.md, tasks.md)
- [ ] Self-review with /spring-java-reviewer
- [ ] Commit and create PR

#### Notes
- Root cause: `BeanOutputConverter` generated `"type": "string"` — no enum constraint in JSON schema
- Fix: enum → `"enum": ["grocery", ...]` in schema + explicit prompt taxonomy
- `@JsonCreator` falls back to OTHER for unknown values (Groq doesn't enforce JSON Schema)
- User decision: multi-tag (humidifier = electronics + home_and_garden)

<!-- Template for adding a new task:

### [W-N] Task title
**Source:** ad-hoc | tasks.md Task N
**Branch:** `feat/task-N-description`
**Status:** Planning | In Progress | Review

#### Steps
- [ ] Step 1
- [ ] Step 2

#### Notes
- Context, decisions, blockers

-->

---

## Ideas Backlog

| ID | Idea | Source | Priority |
|----|------|--------|----------|
| | _No ideas yet._ | | |

<!-- Priority: low / medium / high -->
<!-- When starting work on an idea, move it to Current Task as [W-N] and remove from this table -->

---

## Completion Checklist

Before removing a completed task from this file, verify:

- [ ] `ai/tasks.md` updated (status, implementation notes) -- if task originated there
- [ ] `CLAUDE.md` updated if conventions/rules changed
- [ ] Wiki pages updated per CLAUDE.md "Wiki Synchronization Rule" table
- [ ] Documentation in `./ai/` consistent with changes
- [ ] `README.md` updated if user-facing changes

---

## Rules

- **Always one Current Task** during active work (even if from `ai/tasks.md`)
- **IDs**: `W-N` for work items, `I-N` for ideas (separate from tasks.md numbering)
- **Completed tasks are REMOVED** (not marked done) -- only after Completion Checklist passes
- **Ideas** move from Backlog to Current Task when work begins
