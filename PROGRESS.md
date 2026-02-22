# Active Work Tracker

Persistent working board for tracking current task and ad-hoc ideas.
Read at the start of every conversation (referenced from CLAUDE.md).

---

## Current Task

### [W-1] PDF Support — PDF-to-Image Conversion Pipeline
**Source:** tasks.md Task 15
**Branch:** `feat/task-15-pdf-support`
**Status:** In Progress (verification pending)

#### Steps
- [x] PDFBox dependency + config (pom.xml, UploadProperties, application.properties)
- [x] ImageWriteUtils extraction (DRY refactor)
- [x] PdfConversionService (exception, interface, implementation)
- [x] BillAnalysisService interface change to List<byte[]>
- [x] Controller + ExceptionHandler + Cleanup
- [x] Tests (new + updated existing) — 131 pass, 0 fail
- [x] Documentation (tasks.md, api-plan.md, tech-stack.md, wiki)
- [ ] Final verification: spotless + checkstyle + tests

#### Notes
- Single-model strategy: Llama 4 Scout for both images and converted PDF pages
- PDFBox 3.x `InvalidPasswordException` caught explicitly for encrypted PDFs
- `ImageWriteUtils` extracted for DRY between preprocessing and PDF conversion

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
