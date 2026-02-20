# Contributing Guide

> Practical guide for setting up the project, creating task branches, writing PRs that work with the CI pipeline, and interpreting Claude's automated review.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Local Setup](#local-setup)
- [Running the Application](#running-the-application)
- [Development Workflow](#development-workflow)
- [Running Spotless Locally](#running-spotless-locally)
- [Running Checkstyle Locally](#running-checkstyle-locally)
- [Running Tests Locally](#running-tests-locally)
- [How to Download Review Artifacts](#how-to-download-review-artifacts)
- [How to Trigger Pattern Police](#how-to-trigger-pattern-police)
- [Code Conventions Quick Reference](#code-conventions-quick-reference)
- [Related Pages](#related-pages)

---

## Prerequisites

| Requirement | Version | Check Command |
|-------------|---------|---------------|
| Java (JDK) | 17+ | `java -version` |
| Maven | 3.9+ (or use `./mvnw`) | `./mvnw -version` |
| Git | 2.x+ | `git --version` |
| GitHub CLI | 2.x+ | `gh --version` |

No database required — the application uses in-memory storage (ConcurrentHashMap).

---

## Local Setup

```bash
# Clone the repository
git clone git@github.com:kojder/bill-manager-actions.git
cd bill-manager-actions

# Copy environment file and add your Groq API key
cp .env.example .env
# Edit .env: GROQ_API_KEY=gsk_...

# Verify the build
./mvnw clean compile

# Auto-fix formatting
./mvnw spotless:apply

# Run checkstyle
./mvnw checkstyle:check

# Run tests
./mvnw test
```

---

## Running the Application

```bash
# Start with default profile
./mvnw spring-boot:run

# Start with dev profile (verbose logging, lenient limits)
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`. Available endpoints:

- `POST http://localhost:8080/api/bills/upload` — Upload a bill
- `GET http://localhost:8080/api/bills/{id}` — Retrieve analysis result

---

## Development Workflow

### Step 1: Pick a Task

Check `ai/tasks.md` for available tasks. Each task has a description, scope, and expected review points.

### Step 2: Create a Branch

**Branch naming is critical** — it determines how the CI pipeline enriches your PR:

```bash
# Format: feat/task-{N}-{short-description}
git checkout -b feat/task-8-image-preprocessing
```

The regex `task-([0-9]+)` extracts the task number from the branch name. See [PR Enrichment](06-PR-Enrichment-and-Task-Workflow) for details.

### Step 3: Implement and Test Locally

```bash
# Write code...

# Auto-fix formatting
./mvnw spotless:apply

# Run checkstyle (MUST pass before commit)
./mvnw checkstyle:check

# Run tests
./mvnw test

# Commit (Conventional Commits format)
git add .
git commit -m "feat(upload): add image preprocessing service"
```

### Step 4: Open a Pull Request

```bash
git push -u origin feat/task-8-image-preprocessing
gh pr create --fill
```

The PR template (`.github/pull_request_template.md`) will be applied automatically. Fill in:

1. **Description** — Summary of changes
2. **Type of Change** — Check the appropriate box
3. **Review Rule Sets** — Check which CLAUDE.md rules apply
4. **Expected Review Findings** — Copy from `ai/tasks.md`
5. **Testing** — Confirm checkstyle and tests pass

### Step 5: Wait for CI Pipeline

The pipeline runs automatically:

```
enrich-description → checkstyle → test → claude-review
```

The `enrich-description` job will populate the **Task Reference** section with context from `ai/tasks.md`.

### Step 6: Read Claude's Review

Claude produces three types of output:

1. **Inline comments** — On specific lines in the PR diff
2. **PR summary comment** — Top-level overview of findings
3. **Structured report** — Downloadable artifact (see [How to Download](#how-to-download-review-artifacts))

### Step 7: Iterate

Push fixes and the pipeline re-runs (checkstyle → test → claude-review):

```bash
git add .
git commit -m "fix(upload): address review findings from PR #8"
git push
```

To trigger a **full re-run** (including enrichment): add the `rerun` label to the PR.

---

## Running Spotless Locally

Spotless enforces Google Java Format (GOOGLE style, 2-space indent) across all Java files.

```bash
# Check formatting (same as CI)
./mvnw spotless:check

# Auto-fix all formatting issues
./mvnw spotless:apply
```

Run `spotless:apply` before committing to avoid CI failures. The `.editorconfig` file provides IDE-agnostic defaults (2-space indent for Java), reducing formatting drift from IDE reformatting.

Use `// spotless:off` and `// spotless:on` comments to exclude code blocks where google-java-format produces lines exceeding 120 characters (e.g., Records with multiple validation annotations).

---

## Running Checkstyle Locally

```bash
# Run checkstyle
./mvnw checkstyle:check

# Expected output (success):
# BUILD SUCCESS - no violations

# On failure: fix violations and re-run
```

**Zero tolerance policy:** Every commit must pass checkstyle. The CI pipeline blocks on any violation.

Key rules to remember:
- All method/constructor parameters must be `final`
- No star imports (`import java.util.*`)
- Braces required on all control structures
- Max line length: 120 characters

See [Checkstyle Configuration](10-Checkstyle-Configuration) for the full rule set.

---

## Running Tests Locally

```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=FileValidationServiceImplTest

# Run a specific test method
./mvnw test -Dtest=FileValidationServiceImplTest#shouldRejectOversizedFile
```

---

## How to Download Review Artifacts

After the CI pipeline completes:

1. Go to the PR on GitHub
2. Click **Checks** tab
3. Click on the **CI Pipeline** workflow run
4. Scroll to the **Artifacts** section at the bottom
5. Download `claude-review-report-pr-{N}`

The artifact contains `reports/pr-{N}-review.md` — a structured review report with execution plan, findings, suggested patches, and next actions.

---

## How to Trigger Pattern Police

1. Go to the **Actions** tab on GitHub
2. Select **Pattern Police** from the workflow list
3. Click **Run workflow**
4. Enter the **PR number**
5. Click the green **Run workflow** button
6. After completion, download the `pattern-audit-pr-{N}` artifact

See [Pattern Police](08-Pattern-Police) for details on what it checks.

---

## Code Conventions Quick Reference

| Convention | Rule |
|-----------|------|
| **Formatting** | Google Java Format via Spotless (`./mvnw spotless:apply`) |
| **DTOs** | Java Records, not classes |
| **`final`** | On all parameters, fields, catch variables (except interface method params) |
| **Interfaces** | Services define interfaces, controllers depend on abstractions |
| **Comments** | Minimal — code should be self-documenting through clear naming |
| **Commits** | Conventional Commits format: `type(scope): description` |
| **API keys** | Environment variables only, never hardcoded |
| **MIME validation** | By magic bytes, not by extension |
| **Error responses** | Standardized `ErrorResponse` record |

---

## Related Pages

- [CI Pipeline Deep Dive](03-CI-Pipeline-Deep-Dive) — Full pipeline walkthrough
- [PR Enrichment and Task Workflow](06-PR-Enrichment-and-Task-Workflow) — How branch naming and task extraction work
- [Checkstyle Configuration](10-Checkstyle-Configuration) — Detailed Checkstyle rules
- [Application Architecture](11-Application-Architecture) — Project structure and tech stack

---

*Last updated: 2026-02-20*

*Sources: `CLAUDE.md` (Code Conventions, Build Commands, Git Commit Guidelines), `ai/tasks.md` (Rules section)*
