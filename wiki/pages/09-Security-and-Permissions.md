# Security and Permissions

> Every Claude workflow uses scoped tool whitelists and minimal GitHub permissions to enforce least-privilege access — the review job can only read and report, not modify code.

---

## Table of Contents

- [Security Philosophy](#security-philosophy)
- [allowedTools Whitelist Comparison](#allowedtools-whitelist-comparison)
- [GitHub Permissions per Workflow](#github-permissions-per-workflow)
- [Secrets Management](#secrets-management)
- [Fork Safety](#fork-safety)
- [Bot Actor Control](#bot-actor-control)
- [What Claude Cannot Do](#what-claude-cannot-do)
- [Related Pages](#related-pages)

---

## Security Philosophy

All three workflows follow the principle of **least-privilege access**: each Claude invocation gets only the tools and permissions it needs, nothing more.

| Workflow | Access Level | Rationale |
|----------|-------------|-----------|
| CI Review | Read-only + write reports | Automated review should observe, not modify |
| Interactive Claude | Read/Write/Edit + build tools | Developer-initiated, may need to make changes |
| Pattern Police | Read-only + write reports | Audit should only report, never modify |

---

## allowedTools Whitelist Comparison

Each workflow explicitly declares which tools Claude can use via `--allowedTools`:

| Tool | CI Review | Interactive | Pattern Police |
|------|:---------:|:-----------:|:--------------:|
| `Read` | — | Yes | — |
| `Write` | Yes | Yes | Yes |
| `Edit` | — | Yes | — |
| `Bash(gh pr view:*)` | Yes | Yes | Yes |
| `Bash(gh pr diff:*)` | Yes | Yes | Yes |
| `Bash(gh pr comment:*)` | Yes | Yes | — |
| `Bash(gh issue view:*)` | — | Yes | — |
| `Bash(gh issue comment:*)` | — | Yes | — |
| `Bash(git diff:*)` | — | Yes | — |
| `Bash(git log:*)` | — | Yes | — |
| `Bash(git status)` | — | Yes | — |
| `Bash(./mvnw checkstyle:check)` | — | Yes | — |
| `Bash(./mvnw test)` | — | Yes | — |
| `Bash(./mvnw test -Dtest:*)` | — | Yes | — |
| `Bash(mkdir -p reports)` | Yes | — | Yes |
| `mcp__github_inline_comment__*` | Yes | Yes | — |

**Key observations:**

- **CI Review** has no `Read` or `Edit` — it cannot read arbitrary files or modify source code. It can only write to the `reports/` directory and post comments.
- **Interactive Claude** has the broadest tool set — it can read, write, edit files, run tests, and interact with Git history. This is appropriate because it's developer-initiated.
- **Pattern Police** is the most restricted — it can only read the PR diff, view PR metadata, and write a report. No commenting, no file reading, no builds.

---

## GitHub Permissions per Workflow

Each workflow declares minimum required GitHub token permissions:

### CI Pipeline (`ci.yml`)

| Job | `contents` | `pull-requests` | `issues` | `id-token` |
|-----|:---------:|:---------------:|:--------:|:----------:|
| enrich-description | read | write | — | — |
| checkstyle | — | — | — | — |
| test | — | — | — | — |
| claude-review | read | write | — | write |
| cleanup-label | — | write | — | — |

### Interactive Claude (`claude.yml`)

| Permission | Value | Why |
|-----------|-------|-----|
| `contents` | write | Can create/modify files |
| `pull-requests` | write | Can post PR comments |
| `issues` | write | Can post issue comments |
| `id-token` | write | OAuth token exchange with Anthropic |

### Pattern Police (`pattern-police.yml`)

| Permission | Value | Why |
|-----------|-------|-----|
| `contents` | read | Can read repository files |
| `pull-requests` | read | Can read PR diff (not write comments) |
| `id-token` | write | OAuth token exchange with Anthropic |

**Pattern Police has `pull-requests: read`** (not `write`) — it cannot post comments on the PR. Its only output is the downloadable report artifact.

---

## Secrets Management

### CLAUDE_CODE_OAUTH_TOKEN

The primary secret used across all three workflows:

```yaml
claude_code_oauth_token: ${{ secrets.CLAUDE_CODE_OAUTH_TOKEN }}
```

| Aspect | Details |
|--------|---------|
| **Storage** | GitHub repository secret |
| **Scope** | Repository-level (not org-level) |
| **Purpose** | Authenticates Claude Code Action with Anthropic API |
| **Who can access** | Repository collaborators with push access |
| **Forks** | NOT available to fork PRs (GitHub security policy) |

### GITHUB_TOKEN vs github.token

The workflows use two token patterns:

| Pattern | Used By | Scope |
|---------|---------|-------|
| `${{ secrets.GITHUB_TOKEN }}` | enrich-description, cleanup-label | Scoped to the workflow's declared permissions |
| `${{ github.token }}` | claude-review, pattern-police | Same token, different syntax (shorthand) |

Both refer to the same automatically-generated GitHub token, scoped to the permissions declared in the workflow.

---

## Fork Safety

GitHub's security model prevents secrets from leaking to fork PRs:

| Scenario | Secrets Available? | Workflows Run? |
|---------|:-----------------:|:--------------:|
| Branch push (collaborator) | Yes | Yes (full pipeline) |
| Fork PR (external contributor) | No | Partial (no Claude review) |

When a fork PR triggers the CI pipeline:
- **checkstyle** and **test** jobs run normally (they don't need secrets)
- **claude-review** fails silently because `CLAUDE_CODE_OAUTH_TOKEN` is unavailable
- **enrich-description** works if it only needs `GITHUB_TOKEN` (available for fork PRs with limited scope)

This is by design — you should never expose API tokens to untrusted fork PRs.

---

## Bot Actor Control

### Workflow-Level Filter (claude.yml)

The Interactive Claude workflow rejects all bot-generated events at the job level:

```yaml
if: |
  !endsWith(github.actor, '[bot]') && ...
```

This prevents the CI review's inline comments (posted by `claude[bot]`) from triggering redundant workflow runs. Combined with removing `pull_request_review` and `pull_request_review_comment` triggers, this eliminates ~14 noise runs per review cycle.

### Action-Level Filter (allowed_bots)

All workflows specify which bot actors are allowed to interact with Claude when the workflow does run:

```yaml
allowed_bots: "claude[bot],github-actions[bot]"
```

| Bot | Why Allowed |
|-----|------------|
| `claude[bot]` | Claude Code Action's own bot account (for self-referencing comments) |
| `github-actions[bot]` | GitHub Actions automation (for bot-initiated events) |

This prevents arbitrary bots from triggering expensive Claude invocations.

---

## What Claude Cannot Do

Regardless of which workflow is running, Claude **cannot**:

| Limitation | Reason |
|-----------|--------|
| Submit formal GitHub PR reviews (approve/request changes) | GitHub App permission restriction |
| Execute arbitrary shell commands | Blocked by `--allowedTools` whitelist |
| Modify `.github/workflows/` files | GitHub App permission restriction |
| Work across multiple repositories | Single-repo checkout only |
| Access external APIs or services | No network tools in whitelist |
| Push commits (in CI review and Pattern Police) | No `git push` in allowed tools |
| Review PRs that modify workflow files | Workflow file validation requires matching `master` |

### Workflow File Validation

Claude Code Action enforces that the workflow file invoking it (`ci.yml`) must be identical on the PR branch and the default branch. This prevents malicious PRs from modifying the review workflow to bypass security controls.

**Impact:** PRs that legitimately modify `ci.yml` (e.g., adding CI steps) will fail the `claude-review` job. The workaround is merging with `gh pr merge <N> --squash --admin`.

---

## Related Pages

- [Pipeline Overview](02-Pipeline-Overview) — How all three workflows compare
- [CI Pipeline Deep Dive](03-CI-Pipeline-Deep-Dive) — Permissions for each CI job
- [Claude Code Review Job](05-Claude-Code-Review-Job) — Tool restrictions for the review job
- [Interactive Claude Assistant](07-Interactive-Claude-Assistant) — Broader tool access explained
- [Pattern Police](08-Pattern-Police) — Minimal read-only permissions

---

*Last updated: 2026-02-20*

*Sources: `.github/workflows/ci.yml` (permissions, allowedTools), `.github/workflows/claude.yml` (permissions, allowedTools), `.github/workflows/pattern-police.yml` (permissions, allowedTools), `docs/claude-actions-context.md` (Security Considerations, Limitations)*
