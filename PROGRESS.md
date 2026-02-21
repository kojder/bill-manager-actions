# Active Work Tracker

Persistent working board for tracking current task and ad-hoc ideas.
Read at the start of every conversation (referenced from CLAUDE.md).

---

## Current Task

### [W-2] Create spring-boot-implementer Claude Code Skill
**Source:** ad-hoc
**Status:** In Progress

#### Steps
- [x] Scaffold skill with init_skill.py
- [x] Remove scaffolded example files
- [x] Write references/dto-exception.md
- [x] Write references/service.md
- [x] Write references/controller.md
- [x] Write references/config.md
- [x] Write references/testing.md
- [x] Write references/spring-ai.md
- [x] Write SKILL.md (frontmatter + body)
- [x] Verify skill structure
- [x] Commit

#### Notes
- Implementation guide skill (complements spring-java-reviewer for code review)
- Copy-paste-ready templates with `// TODO:` markers, not checklists
- Three-layer architecture: CLAUDE.md (conventions) → reviewer (verification) → implementer (writing)
- 6 reference files by component type for granular context loading
- All content in English

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
