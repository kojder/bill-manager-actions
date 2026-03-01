# Jira Integration Setup

> Step-by-step guide for connecting the project to a Jira Cloud space — obtaining credentials, configuring the local environment, and setting up GitHub repository secrets for CI workflows.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Step 1: Create a Jira API Token](#step-1-create-a-jira-api-token)
- [Step 2: Configure the Local Environment](#step-2-configure-the-local-environment)
- [Step 3: Verify the Connection](#step-3-verify-the-connection)
- [Step 4: Configure GitHub Repository Secrets](#step-4-configure-github-repository-secrets)
- [Environment Variables Reference](#environment-variables-reference)
- [Notes on the Jira CLI](#notes-on-the-jira-cli)
- [Related Pages](#related-pages)

---

## Prerequisites

- A Jira Cloud account (free developer account at [atlassian.com](https://www.atlassian.com) is sufficient)
- A Jira project (Space) with a known project key (e.g. `BM`)
- GitHub CLI installed and authenticated (`gh auth status`)

---

## Step 1: Create a Jira API Token

API tokens are used instead of passwords for REST API authentication.

1. Go to [id.atlassian.com/manage-profile/security/api-tokens](https://id.atlassian.com/manage-profile/security/api-tokens)
2. Click **Create API token**
3. Enter a label (e.g. `bill-manager-local`)
4. Click **Create** and copy the token immediately — it is shown only once

> **Security note:** Treat the API token like a password. Never commit it to version control. Revoke and regenerate it if it is ever exposed in a chat, log, or public file.

---

## Step 2: Configure the Local Environment

Copy the example file and fill in your credentials:

```bash
cp .env.example .env
```

Edit `.env` with the following values:

```env
GROQ_API_KEY=gsk_your_actual_key_here
JIRA_BASE_URL=https://your-domain.atlassian.net
JIRA_USER_EMAIL=your-email@example.com
JIRA_API_TOKEN=your-api-token-here
JIRA_PROJECT_KEY=BM
```

| Variable | Description | Example |
|----------|-------------|---------|
| `GROQ_API_KEY` | Groq API key for LLM calls | `gsk_abc123...` |
| `JIRA_BASE_URL` | Your Atlassian tenant URL | `https://yourname.atlassian.net` |
| `JIRA_USER_EMAIL` | Email of the Atlassian account | `you@example.com` |
| `JIRA_API_TOKEN` | Token from Step 1 | `ATATT3xFfGF0...` |
| `JIRA_PROJECT_KEY` | Project key in Jira | `BM` |

> `.env` is listed in `.gitignore` (line 42) and will never be committed. `.env.example` with placeholder values is tracked in the repository as a reference template.

---

## Step 3: Verify the Connection

Load credentials and call the Jira REST API:

```bash
set -a && source .env && set +a

# Fetch your account info
curl -s -u "$JIRA_USER_EMAIL:$JIRA_API_TOKEN" \
  "$JIRA_BASE_URL/rest/api/3/myself?fields=displayName,emailAddress" \
  -o /tmp/jira_me.json && python3 -m json.tool /tmp/jira_me.json

# List issues in the project (note: GET /search removed, use POST /search/jql)
curl -s -X POST \
  -u "$JIRA_USER_EMAIL:$JIRA_API_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"jql\": \"project=$JIRA_PROJECT_KEY ORDER BY created DESC\", \"fields\": [\"summary\",\"status\"]}" \
  "$JIRA_BASE_URL/rest/api/3/search/jql" \
  -o /tmp/jira_issues.json && \
python3 -c "
import json
with open('/tmp/jira_issues.json') as f:
    for i in json.load(f)['issues']:
        print(i['key'], i['fields']['summary'], '-', i['fields']['status']['name'])
"
```

A successful response returns your account details and the list of issues. A `401 Unauthorized` response means the token or email is incorrect.

---

## Step 4: Configure GitHub Repository Secrets

For CI workflows to access Jira (e.g. fetching ticket descriptions to enrich PR bodies), the same four variables must be added as GitHub repository secrets.

### Using GitHub CLI

```bash
gh secret set JIRA_BASE_URL --body "https://your-domain.atlassian.net"
gh secret set JIRA_USER_EMAIL --body "your-email@example.com"
gh secret set JIRA_API_TOKEN  # prompts for value interactively (safer — not in shell history)
gh secret set JIRA_PROJECT_KEY --body "BM"
```

> **Recommended:** Use `gh secret set JIRA_API_TOKEN` without `--body` so the token is entered interactively and never appears in shell history.

### Using GitHub Web UI (alternative)

1. Go to **Settings → Secrets and variables → Actions** in the repository
2. Click **New repository secret** for each variable
3. Use the exact names from the table below

### Secret Names

| Secret Name | Maps to `.env` variable | Value |
|-------------|------------------------|-------|
| `JIRA_BASE_URL` | `JIRA_BASE_URL` | `https://your-domain.atlassian.net` |
| `JIRA_USER_EMAIL` | `JIRA_USER_EMAIL` | Your Atlassian account email |
| `JIRA_API_TOKEN` | `JIRA_API_TOKEN` | Token from Step 1 |
| `JIRA_PROJECT_KEY` | `JIRA_PROJECT_KEY` | e.g. `BM` |

The secret names intentionally match the `.env` variable names, so the same code works in both local development and CI.

### Using Secrets in Workflows

```yaml
env:
  JIRA_BASE_URL: ${{ secrets.JIRA_BASE_URL }}
  JIRA_USER_EMAIL: ${{ secrets.JIRA_USER_EMAIL }}
  JIRA_API_TOKEN: ${{ secrets.JIRA_API_TOKEN }}
  JIRA_PROJECT_KEY: ${{ secrets.JIRA_PROJECT_KEY }}
```

---

## Environment Variables Reference

Full list of variables used by the application:

| Variable | Required | Used by | Description |
|----------|----------|---------|-------------|
| `GROQ_API_KEY` | Yes | Spring AI / Groq | LLM API key for bill analysis |
| `JIRA_BASE_URL` | Jira only | Jira REST API | Atlassian tenant URL |
| `JIRA_USER_EMAIL` | Jira only | Jira REST API | Atlassian account email |
| `JIRA_API_TOKEN` | Jira only | Jira REST API | API authentication token |
| `JIRA_PROJECT_KEY` | Jira only | Jira REST API | Default project key for queries |

---

## Notes on the Jira CLI

[jira-cli](https://github.com/ankitpokhrel/jira-cli) v1.7.0 is installed at `/usr/local/bin/jira` but has a known bug with Jira **team-managed spaces** (panic on issue creation). All Jira operations in this project use the **Jira REST API v3** directly via `curl`.

The `/jira` Claude Code skill (`.claude/skills/jira/SKILL.md`) documents the full REST API workflow for interactive Jira operations within Claude Code sessions.

---

## Related Pages

- [Contributing Guide](12-Contributing-Guide) — Local setup, build commands, `.env` usage
- [PR Enrichment and Task Workflow](06-PR-Enrichment-and-Task-Workflow) — How Jira tickets will integrate with PR descriptions
- [Security and Permissions](09-Security-and-Permissions) — How secrets are scoped in CI workflows

---

*Last updated: 2026-03-01*
