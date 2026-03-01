## Description

<!-- Summary of changes and motivation -->

## Task / Issue Reference

<!-- Auto-populated by PR Enrich workflow based on branch name:
     - feat/task-{N}-description → reads from ./ai/tasks.md
     - feat/PROJ-1234-description → fetches from Jira REST API
     Changed files are always taken from the actual PR diff.
     You can also fill this manually if the branch name doesn't match. -->

<!-- TASK_PLACEHOLDER -->

## Type of Change

- [ ] New feature (`feat`)
- [ ] Bug fix (`fix`)
- [ ] Refactoring (`refactor`)
- [ ] Documentation (`docs`)
- [ ] CI/CD configuration (`chore`)

## Review Rule Sets

<!-- Check which CLAUDE.md path-specific review rules apply to this PR -->

- [ ] **Global** — architecture, SOLID, REST conventions, Java Records
- [ ] **Config Module** (`**/config/**`) — secrets, env separation, configurable URLs/timeouts
- [ ] **Upload Module** (`**/upload/**`) — MIME validation (magic bytes), size limits, path traversal, temp file cleanup
- [ ] **AI Module** (`**/ai/**`) — timeout, retry (Exponential Backoff), token limits, structured output, fallback

## Expected Review Findings

<!-- What should Claude Code Actions catch during review? Copy relevant points from ./ai/tasks.md -->

- [ ] <!-- e.g., MIME validation by file content, not by extension -->
- [ ] <!-- e.g., Size check BEFORE loading entire file into memory -->

## Testing

- [ ] `./mvnw checkstyle:check` — zero violations
- [ ] `./mvnw test` — all tests pass
- [ ] New unit tests added for changed code

## Review Evaluation

<!-- Fill in AFTER Claude Code Actions review completes -->

### Findings Quality

| Metric | Value |
|--------|-------|
| Expected findings detected | `/` |
| False positives | |
| Missed findings | |
| Unexpected valuable findings | |

### Notes

<!-- Observations about Claude's review quality, e.g.:
- Did Claude apply the correct path-specific rules?
- Were inline comments placed on the right lines?
- Was the summary accurate?
- Any suggestions for improving CLAUDE.md review rules?
-->
