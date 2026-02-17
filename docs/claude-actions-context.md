# Claude Code Actions — Context Management

How Claude Code Actions manages context, configuration, and extensibility when running as a GitHub Action.

## Context Sources

When Claude runs via GitHub Actions (code review or `@claude` mentions), it has access to the following context:

### 1. CLAUDE.md Files

Claude **automatically reads and follows** `CLAUDE.md` files from the repository. This is built into the action's system prompt:

> "Always check for and follow the repository's CLAUDE.md file(s) as they contain repo-specific instructions and guidelines."

- Root `CLAUDE.md` is always read
- Nested `CLAUDE.md` files in subdirectories are also recognized
- This is the primary way to control Claude's behavior during review

### 2. Full Repository Access

Claude receives a full checkout of the PR branch and can read any file in the repository using standard file tools (`Read`, `Glob`, `Grep`).

### 3. GitHub Context

Claude receives structured data about the triggering event:

| Data | Available |
|------|-----------|
| PR diff and changed files | Yes |
| PR description and title | Yes |
| Comments and review comments | Yes |
| Commit history | Yes |
| Author information | Yes |
| Workflow run statuses | Yes (with `actions: read` permission) |
| Job logs and test results | Yes (with `actions: read` permission) |

### 4. `.claude/` Directory

Claude can access the `.claude/` directory structure:

- `.claude/settings.json` — project-specific settings, hooks
- `.claude/commands/` — custom slash commands (see [Extensibility](#extensibility))

## Configuration Options

Configuration is done in the workflow YAML file (`ci.yml`, `claude.yml`):

```yaml
- uses: anthropics/claude-code-action@v1
  with:
    claude_code_oauth_token: ${{ secrets.CLAUDE_CODE_OAUTH_TOKEN }}
    allowed_bots: "claude[bot],github-actions[bot]"
    prompt: |
      Custom instructions for Claude...
    claude_args: |
      --allowedTools "..."
    settings: |
      {"model": "claude-sonnet-4-5-20250929"}
    plugins: "plugin-name"
```

| Parameter | Purpose |
|-----------|---------|
| `prompt` | Main instructions — what Claude should do and how |
| `claude_args` | CLI flags: `--model`, `--max-turns`, `--allowedTools`, `--mcp-config` |
| `settings` | JSON configuration: model, env variables, permissions, hooks |
| `allowed_bots` | Allow bot-initiated PR events (prevents blocking on bot commits) |
| `plugins` | Install Claude Code plugins for extended capabilities |
| `additional_permissions` | Grant extra GitHub API access (e.g., `actions:read`) |

## Extensibility

### Commands (`.claude/commands/`)

Custom slash commands are markdown files stored in the repository. They work as prompt templates that can be invoked from GitHub PR comments.

**Creating a command:**

```
.claude/commands/review-security.md
```

```markdown
Review this PR with a security focus:
- Check for injection vulnerabilities (SQL, XSS, command injection)
- Verify input validation at all system boundaries
- Ensure secrets are not hardcoded or logged
- Check authentication and authorization logic
```

**Invoking from a PR comment:**

```
@claude /review-security
```

This is the simplest way to create specialized review profiles (e.g., separate commands for backend, frontend, security review).

### Skills / Plugins

Skills are more advanced extensions installed as **plugins**. They provide specialized tools, domain knowledge, and workflows beyond simple prompt templates.

- Skills available in local Claude Code CLI (e.g., `pdf`, `docx`, `frontend-design`) are **not automatically available** in GitHub Actions
- To use skills in GitHub Actions, explicitly configure them via the `plugins` parameter in the workflow YAML
- Skills are a newer feature and the plugin ecosystem for GitHub Actions is still evolving

**Key difference:**

| Feature | Commands | Skills/Plugins |
|---------|----------|----------------|
| Definition | Markdown files in `.claude/commands/` | Installed packages |
| Complexity | Simple prompt templates | Tools + domain knowledge + workflows |
| GitHub Actions | Works out of the box | Requires `plugins` parameter in workflow |
| Invocation | `@claude /command-name` | Automatic or via workflow config |

### MCP Servers

Claude Code Actions can use custom MCP (Model Context Protocol) servers for additional tool integrations:

```yaml
claude_args: |
  --mcp-config /path/to/mcp-config.json
```

This enables Claude to interact with external services (databases, APIs, custom tools) during review.

## Workflow Types

This project uses two workflow configurations:

### `ci.yml` — Automated Code Review

Triggers on every PR (`pull_request` event). Runs after Checkstyle and tests pass. Claude reviews the PR diff and posts comments automatically.

### `claude.yml` — Interactive Assistant

Triggers on `@claude` mentions in PR comments, review comments, and issues. Claude responds to specific requests, can make code changes, and push commits.

## Security Considerations

- `CLAUDE_CODE_OAUTH_TOKEN` is stored as a GitHub repository secret
- The token is tied to the repo, not to individual PR authors
- PRs from **forks** do not have access to secrets (GitHub security policy)
- Collaborators pushing branches directly to the repo can trigger workflows with full secret access
- The `allowed_bots` parameter controls which bot actors can trigger Claude review

## Limitations

Claude running in GitHub Actions **cannot**:

- Submit formal GitHub PR reviews (approve/request changes)
- Execute arbitrary shell commands (disabled by default, controlled via `--allowedTools`)
- Modify `.github/workflows/` files (GitHub App permission restriction)
- Work across multiple repositories in a single run

## References

- [claude-code-action repository](https://github.com/anthropics/claude-code-action)
- [Setup guide](https://github.com/anthropics/claude-code-action/blob/main/docs/setup.md)
- [Configuration docs](https://github.com/anthropics/claude-code-action/blob/main/docs/configuration.md)
- [Capabilities and limitations](https://github.com/anthropics/claude-code-action/blob/main/docs/capabilities-and-limitations.md)
