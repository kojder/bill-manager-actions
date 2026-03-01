#!/usr/bin/env bash
set -euo pipefail

# All variables injected as env vars from the workflow step:
# SOURCE, TASK_NUM, JIRA_KEY, PR_NUM, BRANCH
# GH_TOKEN, JIRA_BASE_URL, JIRA_USER_EMAIL, JIRA_API_TOKEN

CHANGED_FILES=$(gh pr diff "$PR_NUM" --name-only 2>/dev/null || echo "")

if [ "$SOURCE" = "tasks" ]; then
  TASKS_FILE="./ai/tasks.md"

  if [ ! -f "$TASKS_FILE" ]; then
    cat > /tmp/task_content.md <<EOF
> **Warning:** \`ai/tasks.md\` not found. Cannot auto-populate task reference.
EOF
    echo "has_content=true" >> "$GITHUB_OUTPUT"
    exit 0
  fi

  SECTION=$(awk '
    /^### Task '"$TASK_NUM"':/ { found=1; next }
    found && /^### Task [0-9]+:/ { found=0 }
    found && /^---$/ { found=0 }
    found && /^## / { found=0 }
    found { print }
  ' "$TASKS_FILE")

  if [ -z "$SECTION" ]; then
    cat > /tmp/task_content.md <<EOF
> **Warning:** Task $TASK_NUM not found in \`ai/tasks.md\`.
EOF
    echo "has_content=true" >> "$GITHUB_OUTPUT"
    exit 0
  fi

  TITLE=$(grep -oP "^### Task ${TASK_NUM}: \K.*" "$TASKS_FILE" | sed 's/ ✅ COMPLETED//')
  DESCRIPTION=$(echo "$SECTION" | awk '/^\*\*Description:\*\*/{flag=1; sub(/\*\*Description:\*\* */,""); print; next} flag && /^\*\*/{flag=0} flag{print}')
  REVIEW_RULES=$(echo "$SECTION" | grep -oP '^\*\*Claude review:\*\* \K.*' || true)
  EXPECTED=$(echo "$SECTION" | awk '/^\*\*Expected review points:\*\*/{flag=1; next} flag && /^- \[/{print} flag && !/^- \[/ && !/^$/{flag=0}')

  {
    echo "### Task ${TASK_NUM}: ${TITLE}"
    echo ""
    if [ -n "$DESCRIPTION" ]; then
      echo "**Description:** ${DESCRIPTION}"
      echo ""
    fi
    if [ -n "$CHANGED_FILES" ]; then
      echo "**Changed files (from PR diff):**"
      echo "$CHANGED_FILES" | sed 's/^/- /'
      echo ""
    fi
    if [ -n "$REVIEW_RULES" ]; then
      echo "**Claude review:** ${REVIEW_RULES}"
      echo ""
    fi
    if [ -n "$EXPECTED" ]; then
      echo "**Expected review points:**"
      echo "${EXPECTED}"
    fi
  } > /tmp/task_content.md
  echo "has_content=true" >> "$GITHUB_OUTPUT"

elif [ "$SOURCE" = "jira" ]; then
  if [ -z "$JIRA_BASE_URL" ] || [ -z "$JIRA_USER_EMAIL" ] || [ -z "$JIRA_API_TOKEN" ]; then
    echo "::warning::Jira secrets not configured. Skipping Jira enrichment."
    echo "has_content=false" >> "$GITHUB_OUTPUT"
    exit 0
  fi

  HTTP_CODE=$(curl -s -o /tmp/jira_response.json -w "%{http_code}" \
    -u "$JIRA_USER_EMAIL:$JIRA_API_TOKEN" \
    "$JIRA_BASE_URL/rest/api/3/issue/$JIRA_KEY?fields=summary,description")

  if [ "$HTTP_CODE" -ne 200 ]; then
    echo "::warning::Jira API returned HTTP $HTTP_CODE for $JIRA_KEY. Skipping."
    echo "has_content=false" >> "$GITHUB_OUTPUT"
    exit 0
  fi

  python3 .github/scripts/jira_parse.py

  if [ $? -ne 0 ]; then
    echo "::warning::Failed to parse Jira response."
    echo "has_content=false" >> "$GITHUB_OUTPUT"
    exit 0
  fi

  {
    cat /tmp/jira_header.md
    if [ -n "$CHANGED_FILES" ]; then
      echo "**Changed files (from PR diff):**"
      echo "$CHANGED_FILES" | sed 's/^/- /'
    fi
  } > /tmp/task_content.md
  echo "has_content=true" >> "$GITHUB_OUTPUT"

else
  cat > /tmp/task_content.md <<EOF
> **Warning:** No task number or Jira key detected in branch name \`${BRANCH}\`.
> Expected format: \`feat/task-{N}-description\` or \`feat/PROJ-1234-description\`.
> Fill in the Task / Issue Reference section manually if needed.
EOF
  echo "has_content=true" >> "$GITHUB_OUTPUT"
fi
