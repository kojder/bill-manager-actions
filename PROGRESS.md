# Active Work Tracker

Persistent working board for tracking current task and ad-hoc ideas.
Read at the start of every conversation (referenced from CLAUDE.md).

---

## Current Task

_No active task._

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

## Pending Documentation

### CI Review Pipeline Optimization (PR #22-#28)

Lessons learned and changes that need to be documented in wiki/docs in a future session.

#### Problem
Claude Code Action in agent mode has NO system prompt injection — `prompt:` input is the ONLY instruction.
After 13+ analysis turns (skill + file reads), Claude ignores posting tool instructions ~67% of the time.
Result: review text generated but invisible (not posted to PR).

#### Solution — Two-layer review posting
1. **Prompt instructs Claude to post** (works ~33% of runs) via `mcp__github_inline_comment__create_inline_comment` + `mcp__github_comment__update_claude_comment`
2. **Fallback step** (always runs) with duplicate check → Python parser → GitHub PR Reviews API:
   - Checks for existing review comments (skip if already posted)
   - Extracts `result` from execution file via `jq`
   - Python parser finds `[W-N]`/`[C-N]` findings with `path:line` patterns
   - Resolves short paths to full repo paths using `gh pr diff --name-only`
   - Posts via GitHub PR Reviews API (inline comments + summary body in one API call)
   - Falls back to simple `gh pr comment` if API fails

#### Key findings
- **`use_sticky_comment: true`** — vestigial in claude-code-action v1, not implemented
- **`track_progress: true`** — forces tag mode → adds Write + git push → causes infinite CI loop. NEVER use with review-only workflows
- **Prompt structure**: Safety constraints at TOP, MANDATORY posting at BOTTOM (recency bias)
- **Parser** handles 3 finding formats: backtick `path:line`, backtick `path:line-line`, plain `path line N`
- **ci.yml changes must go to master first**: close PR → push to master → rebase branch → new PR (workflow validation)

#### Master commits (ci.yml)
- `c15d92c` — added MCP comment tool to allowedTools
- `0dd5b92` — added Output Rules prompt section
- `aec5cfe` — added post-processing fallback step
- `b060395` — added duplicate check to fallback
- `faa5027` — restructured prompt (MANDATORY section at end, Safety at beginning)
- `d4cc01a` — added Python finding parser + GitHub Reviews API for inline comments

#### Wiki pages to update
- `05-Claude-Code-Review-Job` — fallback step, two-layer posting architecture
- `02-Pipeline-Overview` — updated pipeline flow diagram
- `09-Security-and-Permissions` — `--allowedTools` changes, MCP tools added

---

## Ideas Backlog

| ID | Idea | Source | Priority |
|----|------|--------|----------|
| | _Empty — I-1 verified (skill works on PR #25-#28)._ | | |

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
