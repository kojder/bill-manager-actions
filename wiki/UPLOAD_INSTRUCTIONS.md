# How to Upload Wiki Pages to GitHub

Step-by-step instructions for publishing wiki pages from the `wiki/pages/` folder to the GitHub Wiki.

---

## Prerequisites

- Git configured with SSH access to GitHub
- Repository: `kojder/bill-manager-actions`
- All wiki pages created in `wiki/pages/` directory

---

## Step 1: Clone the Wiki Repository

GitHub Wiki is a separate git repository. Clone it:

```bash
cd /tmp
git clone git@github.com:kojder/bill-manager-actions.wiki.git
cd bill-manager-actions.wiki
```

> **Note:** If the wiki doesn't exist yet, create at least one page via the GitHub web UI first (Settings > Wiki > Create the first page), then clone.

---

## Step 2: Copy Wiki Pages

Copy all pages from the project's `wiki/pages/` directory:

```bash
cp /path/to/bill-manager-actions/wiki/pages/*.md .
```

---

## Step 3: Verify Page Names

GitHub Wiki uses filenames as page titles. The numbered prefix (`01-`, `02-`, etc.) ensures alphabetical sidebar ordering. GitHub displays the filename (minus `.md`) as the page title, converting hyphens to spaces.

Expected files:
```
01-Home.md
02-Pipeline-Overview.md
03-CI-Pipeline-Deep-Dive.md
04-CLAUDE-MD-as-Review-Brain.md
05-Claude-Code-Review-Job.md
06-PR-Enrichment-and-Task-Workflow.md
07-Interactive-Claude-Assistant.md
08-Pattern-Police.md
09-Security-and-Permissions.md
10-Checkstyle-Configuration.md
11-Application-Architecture.md
12-Contributing-Guide.md
```

---

## Step 4: Set Home Page

GitHub Wiki uses `Home.md` as the landing page. Rename or copy:

```bash
cp 01-Home.md Home.md
```

---

## Step 5: Push to GitHub Wiki

```bash
git add .
git commit -m "docs: add wiki pages for CI/CD automation documentation"
git push origin master
```

---

## Step 6: Verify on GitHub

1. Go to: https://github.com/kojder/bill-manager-actions/wiki
2. Verify the sidebar shows all pages in order
3. Click through each page and check:
   - Internal links work (format: `[Text](Page-Name)`)
   - Mermaid diagrams render correctly
   - Table of contents links work

---

## Internal Link Format

GitHub Wiki links use the page filename (without `.md` extension):

```markdown
[CI Pipeline Deep Dive](03-CI-Pipeline-Deep-Dive)
[CLAUDE.MD as Review Brain](04-CLAUDE-MD-as-Review-Brain)
```

---

## Updating Pages

After making changes to `wiki/pages/` in the main repo:

```bash
cd /tmp/bill-manager-actions.wiki
cp /path/to/bill-manager-actions/wiki/pages/*.md .
cp 01-Home.md Home.md
git add . && git commit -m "docs: update wiki pages" && git push
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Wiki not found" when cloning | Create first page via GitHub web UI |
| Sidebar not showing pages | Pages must be committed and pushed to wiki repo |
| Links not working | Use `(Page-Name)` format, not `(Page-Name.md)` |
| Mermaid not rendering | GitHub Wiki supports Mermaid natively since 2022 |
