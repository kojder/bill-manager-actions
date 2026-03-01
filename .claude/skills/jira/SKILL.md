---
name: jira
description: Use when the user mentions Jira issues (e.g., "BM-123"), asks about tickets, wants to create/view/update issues, check board status, or manage their Jira workflow. Triggers on keywords like "jira", "issue", "ticket", "backlog", or issue key patterns.
---

# Jira

REST API backend for Jira Cloud (team-managed spaces). Credentials loaded from `.env`.

## Credentials Setup

**Always load credentials from `.env` before any operation:**

```bash
set -a && source .env && set +a
```

Variables used in every request:
- `JIRA_BASE_URL` — e.g. `https://your-domain.atlassian.net`
- `JIRA_USER_EMAIL` — Atlassian account email
- `JIRA_API_TOKEN` — API token from id.atlassian.com
- `JIRA_PROJECT_KEY` — default project key (e.g. `BM`)

**Note:** `.env` is gitignored. GitHub Actions uses the same variable names as repository secrets.

---

## Quick Reference (REST API)

> All commands require credentials loaded from `.env` first.

**Note:** Always save curl output to a temp file before parsing with python3 — piping curl → python3 is unreliable in this environment.

```bash
# Pattern to use for ALL API calls:
curl -s -u "$JIRA_USER_EMAIL:$JIRA_API_TOKEN" "URL" -o /tmp/jira_response.json && python3 -c "..." < /tmp/jira_response.json
```

### View issue
```bash
curl -s -u "$JIRA_USER_EMAIL:$JIRA_API_TOKEN" \
  "$JIRA_BASE_URL/rest/api/3/issue/ISSUE-KEY?fields=summary,status,description" \
  -o /tmp/jira_response.json && python3 -m json.tool /tmp/jira_response.json
```

### List issues in project
```bash
# NOTE: GET /search is removed — use POST /search/jql
curl -s -X POST \
  -u "$JIRA_USER_EMAIL:$JIRA_API_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"jql\": \"project=$JIRA_PROJECT_KEY ORDER BY created DESC\", \"fields\": [\"summary\",\"status\",\"description\"]}" \
  "$JIRA_BASE_URL/rest/api/3/search/jql" \
  -o /tmp/jira_response.json && \
python3 -c "
import json
with open('/tmp/jira_response.json') as f:
    for i in json.load(f)['issues']:
        print(i['key'], i['fields']['summary'], '-', i['fields']['status']['name'])
"
```

### Create issue
```bash
curl -s -X POST \
  -u "$JIRA_USER_EMAIL:$JIRA_API_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"fields\": {\"project\": {\"key\": \"$JIRA_PROJECT_KEY\"}, \"summary\": \"SUMMARY\", \"issuetype\": {\"name\": \"Task\"}}}" \
  "$JIRA_BASE_URL/rest/api/3/issue" \
  -o /tmp/jira_response.json && python3 -c "import json; d=json.load(open('/tmp/jira_response.json')); print('Created:', d['key'])"
```

### Get issue description
```bash
curl -s -u "$JIRA_USER_EMAIL:$JIRA_API_TOKEN" \
  "$JIRA_BASE_URL/rest/api/3/issue/ISSUE-KEY?fields=summary,description,status" \
  -o /tmp/jira_response.json && python3 -m json.tool /tmp/jira_response.json
```

### Update description (ADF format)
```bash
curl -s -X PUT \
  -u "$JIRA_USER_EMAIL:$JIRA_API_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"fields": {"description": {"type": "doc", "version": 1, "content": [{"type": "paragraph", "content": [{"type": "text", "text": "NEW DESCRIPTION"}]}]}}}' \
  "$JIRA_BASE_URL/rest/api/3/issue/ISSUE-KEY"
```

### Transition issue (change status)
```bash
# 1. Get available transitions
curl -s -u "$JIRA_USER_EMAIL:$JIRA_API_TOKEN" \
  "$JIRA_BASE_URL/rest/api/3/issue/ISSUE-KEY/transitions" \
  -o /tmp/jira_response.json && \
python3 -c "import json; [print(t['id'], t['name']) for t in json.load(open('/tmp/jira_response.json'))['transitions']]"

# 2. Apply transition using ID from step 1
curl -s -X POST \
  -u "$JIRA_USER_EMAIL:$JIRA_API_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"transition": {"id": "TRANSITION-ID"}}' \
  "$JIRA_BASE_URL/rest/api/3/issue/ISSUE-KEY/transitions"
```

### Add comment
```bash
curl -s -X POST \
  -u "$JIRA_USER_EMAIL:$JIRA_API_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"body": {"type": "doc", "version": 1, "content": [{"type": "paragraph", "content": [{"type": "text", "text": "COMMENT TEXT"}]}]}}' \
  "$JIRA_BASE_URL/rest/api/3/issue/ISSUE-KEY/comment" \
  -o /tmp/jira_response.json && \
python3 -c "import json; d=json.load(open('/tmp/jira_response.json')); print('Comment created:', d['id'])"
```

### List issue types in project
```bash
curl -s -u "$JIRA_USER_EMAIL:$JIRA_API_TOKEN" \
  "$JIRA_BASE_URL/rest/api/3/project/$JIRA_PROJECT_KEY" \
  -o /tmp/jira_response.json && \
python3 -c "import json; [print(t['id'], t['name']) for t in json.load(open('/tmp/jira_response.json'))['issueTypes']]"
```

---

## GitHub Actions Integration

In workflows, credentials come from repository secrets (same variable names):

```yaml
env:
  JIRA_BASE_URL: ${{ secrets.JIRA_BASE_URL }}
  JIRA_USER_EMAIL: ${{ secrets.JIRA_USER_EMAIL }}
  JIRA_API_TOKEN: ${{ secrets.JIRA_API_TOKEN }}
  JIRA_PROJECT_KEY: ${{ secrets.JIRA_PROJECT_KEY }}
```

Example — fetch issue by key extracted from branch name:
```bash
ISSUE_KEY=$(echo "$BRANCH_NAME" | grep -oP '[A-Z]+-[0-9]+')
curl -s -u "$JIRA_USER_EMAIL:$JIRA_API_TOKEN" \
  "$JIRA_BASE_URL/rest/api/3/issue/$ISSUE_KEY?fields=summary,description" \
  -o /tmp/jira_response.json && \
python3 -c "import json; d=json.load(open('/tmp/jira_response.json'))['fields']; print(d['summary'])"
```

---

## Description Format (ADF)

Jira Cloud uses **Atlassian Document Format (ADF)** for descriptions and comments — not plain text or Markdown.

Minimal ADF structure:
```json
{
  "type": "doc",
  "version": 1,
  "content": [
    {
      "type": "paragraph",
      "content": [{"type": "text", "text": "Your text here"}]
    }
  ]
}
```

To extract plain text from ADF response:
```python
import json, sys
d = json.load(sys.stdin)
desc = d['fields'].get('description')
if desc:
    for block in desc.get('content', []):
        for node in block.get('content', []):
            if node['type'] == 'text':
                print(node['text'])
```

---

## Workflow

**Creating tickets:**
1. Load `.env`
2. Draft content with user
3. Show curl command before executing
4. Confirm key from response (e.g. `BM-2`)

**Updating description:**
1. Fetch current description first — show to user
2. Propose new content
3. Get approval
4. PUT with ADF format
5. Verify with GET

**Changing status:**
1. Always fetch available transitions first
2. Never assume transition IDs — they vary per project
3. Show transition options to user before applying

---

## NEVER

- **NEVER hardcode credentials** — always use `.env` variables
- **NEVER edit description without showing original** — Jira has no undo
- **NEVER assume transition IDs** — always fetch `/transitions` first
- **NEVER bulk-modify without explicit approval**
- **NEVER expose `JIRA_API_TOKEN` in logs or output**
