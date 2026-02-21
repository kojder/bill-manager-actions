# Active Work Tracker

Persistent working board for tracking current task and ad-hoc ideas.
Read at the start of every conversation (referenced from CLAUDE.md).

---

## Current Task

### [W-3] Task 10: End-to-End Integration + Simple UI
**Source:** tasks.md Task 10
**Branch:** `feat/task-10-integration`
**Status:** Review

#### Steps
- [x] Add `detectMimeType` to `FileValidationService` interface + impl
- [x] Refactor `BillResultStore` → interface + `InMemoryResultStore`
- [x] Wire full pipeline in `BillUploadController` (4 services)
- [x] Create `HealthController` + `HealthResponse`
- [x] Create `index.html` upload form
- [x] Update `BillUploadControllerTest` (unit tests)
- [x] Create `InMemoryResultStoreTest`
- [x] Create `BillUploadIntegrationTest`
- [x] Run formatters, checkstyle, tests (117 tests, 0 failures)
- [x] Update documentation (tasks.md)

#### Notes
- Synchronous pipeline: upload → validate → detect MIME → preprocess → AI analyze → store → return
- `BillResultStore` class → interface + `InMemoryResultStore` (SOLID DIP)
- Custom `/api/health` endpoint (separate from Actuator `/actuator/health`)
- Mock only `BillAnalysisService` in integration tests (real validation + preprocessing)

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
