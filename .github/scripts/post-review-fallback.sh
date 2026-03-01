#!/usr/bin/env bash
set -euo pipefail

# All variables injected as env vars from the workflow step:
# GH_TOKEN, EXEC_FILE, PR_NUM
# GITHUB_REPOSITORY is set automatically by GitHub Actions

if [ ! -f "$EXEC_FILE" ]; then
  echo "Execution file not found, skipping"
  exit 0
fi

jq -r '.[-1] | select(.type == "result") | .result // empty' \
  "$EXEC_FILE" > /tmp/review.md

if [ ! -s /tmp/review.md ]; then
  echo "No review result found in execution file"
  exit 0
fi

# Signature present in every full structured report from /spring-java-reviewer
FULL_REPORT_MARKER="| Critical |"

# Check for existing Claude comment
CLAUDE_COMMENT=$(gh api "repos/$GITHUB_REPOSITORY/issues/${PR_NUM}/comments" \
  --jq '[.[] | select(.user.login == "claude[bot]")] | .[0] // empty')

if [ -n "$CLAUDE_COMMENT" ]; then
  EXISTING_BODY=$(echo "$CLAUDE_COMMENT" | jq -r '.body')
  if echo "$EXISTING_BODY" | grep -qF "$FULL_REPORT_MARKER"; then
    echo "Full report already present in Claude comment, skipping"
    exit 0
  fi
  # Comment exists but is missing the full report — patch it
  COMMENT_ID=$(echo "$CLAUDE_COMMENT" | jq -r '.id')
  {
    echo "$EXISTING_BODY"
    echo ""
    echo "---"
    echo ""
    echo "## 📋 Full Spring Java Reviewer Report"
    echo ""
    cat /tmp/review.md
  } > /tmp/updated_body.md
  python3 -c 'import json; body=open("/tmp/updated_body.md").read(); json.dump({"body": body}, open("/tmp/patch_payload.json", "w"))'
  gh api "repos/$GITHUB_REPOSITORY/issues/comments/$COMMENT_ID" \
    -X PATCH \
    --input /tmp/patch_payload.json \
    --silent \
    && echo "Fallback: appended full report to existing comment $COMMENT_ID" \
    || echo "Warning: failed to update existing comment"
  exit 0
fi

# No existing comment — get changed file paths and post via Reviews API
gh pr diff "$PR_NUM" --name-only > /tmp/changed_files.txt

# Get HEAD commit SHA for inline comments
COMMIT_SHA=$(gh pr view "$PR_NUM" --json headRefOid --jq '.headRefOid')
export COMMIT_SHA

python3 .github/scripts/parse_review_findings.py

# Post the review (summary + inline comments in one API call)
gh api "repos/$GITHUB_REPOSITORY/pulls/${PR_NUM}/reviews" \
  --input /tmp/review_payload.json \
  --silent \
  && echo "Fallback: review posted with $(jq '.comments | length' /tmp/review_payload.json) inline comment(s)" \
  || {
    echo "Review API failed, falling back to simple comment"
    gh pr comment "$PR_NUM" --body-file /tmp/review.md
  }
