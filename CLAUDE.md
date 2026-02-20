# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## CRITICAL Rules

- **NEVER run `git push` in local CLI** — the user will always push manually. This applies to all branches, including feature branches and master. No exceptions. This rule does NOT apply when running as GitHub Action — in that context, pushing to PR branches is allowed and expected.

## Language Rules

- **Local development (Claude Code CLI)**: Communicate with the user in **Polish**
- **GitHub Actions context (code review, PR comments, issue responses)**: Always write in **English**
- **Code**: All code identifiers (classes, methods, variables, constants) in **English**
- **Documentation**: All documentation files (README, ./ai/ docs, code comments) in **English**
- **Commits**: Conventional Commits format, messages in **English**

## Build Commands

```bash
# Build project
./mvnw clean compile

# Run tests
./mvnw test

# Run single test class
./mvnw test -Dtest=BillManagerApplicationTests

# Auto-fix formatting (run before commit)
./mvnw spotless:apply

# Check formatting (CI gate)
./mvnw spotless:check

# Run checkstyle
./mvnw checkstyle:check

# Run application
./mvnw spring-boot:run

# Package application
./mvnw clean package
```

## Project Documentation

### GitHub Wiki

The project has a comprehensive **[GitHub Wiki](https://github.com/kojder/bill-manager-actions/wiki)** documenting CI automation, Claude Code Review workflows, security model, and application architecture. Wiki source files are maintained in `./wiki/pages/` (12 pages with Mermaid diagrams).

### AI Documentation (`./ai/`)

- `./ai/prd.md` - Product requirements, user stories, MVP scope
- `./ai/tech-stack.md` - Technology stack, architecture, module structure
- `./ai/api-plan.md` - REST API plan, data models, error codes
- `./ai/tasks.md` - Implementation task list with Claude Code Actions review mapping

## Active Work (Local CLI Only)

_This section applies to local Claude Code CLI sessions only. Skip when running as GitHub Action (code review context)._

**Read `PROGRESS.md` at the start of every conversation** to understand:
- The current task being worked on (always populated during active work)
- Ideas backlog for future work
- Detailed step breakdown for the current task

When completing a task, follow the Completion Checklist in `PROGRESS.md` before removing it.

## Architecture Overview

**Bill-Manager** is a Spring Boot 3.5.x application for automated bill analysis using LLMs.
The primary purpose of this repository is testing Claude Code Actions (`anthropics/claude-code-action`) automated Code Review in CI pipelines.

For detailed technology stack and module structure, see `./ai/tech-stack.md`.

## Development Workflow & Best Practices

### SOLID Principles

Always adhere to SOLID principles in software design:

- **S**ingle Responsibility Principle: Each class should have one reason to change
  - Example: `FileValidationService` only validates files, doesn't process them
- **O**pen/Closed Principle: Open for extension, closed for modification
  - Example: Use interfaces (`BillAnalysisService`) so implementations can be swapped
- **L**iskov Substitution Principle: Subtypes must be substitutable for their base types
  - Example: Any `BillAnalysisService` implementation must honor the contract
- **I**nterface Segregation Principle: Many client-specific interfaces are better than one general-purpose interface
  - Example: Separate `FileValidationService` and `ImagePreprocessingService` instead of one `FileService`
- **D**ependency Inversion Principle: Depend on abstractions, not concretions
  - Example: Controllers depend on service interfaces, not concrete implementations

### Decision-Making: The 1-3-1 Rule

When stuck or unsure how to proceed:

1. **State 1 clearly defined problem**
2. **Propose 3 concrete options** to solve it
3. **Give 1 recommendation** (pick one option and explain why)

**CRITICAL**: Do NOT implement any option until user confirms which one to proceed with.

### DRY Principle (Don't Repeat Yourself)

DRY is critical. Avoid duplicated logic and repeated code.

- If you're about to copy/paste similar code, **STOP** and reconsider the design
- Refactor often: extract shared helpers/utilities/modules so changes happen in one place
- Example: Don't duplicate MIME validation logic - create one `FileValidationService` and reuse it
- Spring Boot examples: Use `@ConfigurationProperties` instead of repeating `@Value` annotations

### Continuous Learning Loop

When you detect any of the following:
- Conflicting instructions or patterns
- New requirements or architectural changes
- Missing/inaccurate documentation or unclear conventions

Then:
1. **Propose a specific update** to the relevant rules/context/docs (CLAUDE.md, ./ai/ docs)
2. **Do NOT apply the update** until user confirms
3. **Ask clarifying questions** when needed
4. **After the rules update**, retry the original task from before the mistake

### Wiki Synchronization Rule

The project maintains a **GitHub Wiki** (`./wiki/pages/`) that documents CI pipelines, workflows, review rules, security model, and application architecture. The wiki must stay in sync with the actual project state.

**When making changes that affect wiki-documented content, always verify and update the relevant wiki pages.** Areas that require wiki verification:

| Change Type | Wiki Pages to Check |
|-------------|---------------------|
| Workflow YAML changes (`.github/workflows/`) | 02-Pipeline-Overview, 03-CI-Pipeline-Deep-Dive, 05-Claude-Code-Review-Job, 07-Interactive-Claude-Assistant, 08-Pattern-Police, 09-Security-and-Permissions |
| CLAUDE.md review rules | 04-CLAUDE-MD-as-Review-Brain, 09-Security-and-Permissions |
| `--allowedTools` changes | 09-Security-and-Permissions |
| PR template changes | 06-PR-Enrichment-and-Task-Workflow |
| Checkstyle config changes | 10-Checkstyle-Configuration |
| Package structure / new modules | 11-Application-Architecture |
| Build commands / setup changes | 12-Contributing-Guide |
| REST API endpoints | 11-Application-Architecture, 12-Contributing-Guide |
| New workflow or pipeline job | 01-Home, 02-Pipeline-Overview |

**Workflow:**
1. After implementing the change, check the table above for affected wiki pages
2. Read the relevant wiki page(s) in `./wiki/pages/`
3. Update any outdated content, diagrams, or examples
4. If the wiki update is non-trivial, include it in the same commit or as a follow-up commit

### Plan-First Approach

For any complex, multi-step task:

1. **Before writing code**, create a short plan and a checklist/todo list (use TodoWrite tool)
2. **Execute step-by-step**, keeping the list updated so you don't drift off track
3. **Mark tasks as completed** immediately after finishing them (don't batch completions)
4. **On task completion**, follow the Completion Checklist in `PROGRESS.md`:
   - Update `ai/tasks.md` if the task originated there
   - Verify CLAUDE.md, wiki, and documentation consistency
   - Only then remove the task from `PROGRESS.md`

## Code Conventions

### DTOs and Data Classes

Use **Java Records** for all DTOs and immutable data structures:
```java
public record BillAnalysisResult(
    List<LineItem> items,
    BigDecimal totalAmount,
    List<String> categoryTags
) {}
```

### Use of `final`

Use `final` wherever a variable, parameter, or field should not be reassigned:
- **Constructor parameters**: `FileValidationServiceImpl(final UploadProperties uploadProperties)`
- **Method parameters**: `public void validateFile(final MultipartFile file)`
- **Local variables**: when the variable is assigned once and never reassigned
- **Private fields** injected via constructor: `private final UploadProperties uploadProperties;`
- **Catch block variables**: `catch (final IOException e)`
- **NOT in interface method signatures** — Checkstyle (Google Style) treats `final` on interface parameters as redundant

This makes intent explicit, prevents accidental reassignment, and improves code readability.

### Auto-formatting with Spotless

The project uses `spotless-maven-plugin` with `google-java-format` (GOOGLE style, 2-space indent).

- `./mvnw spotless:apply` — auto-fix all formatting issues (run before commit)
- `./mvnw spotless:check` — verify formatting (CI gate, runs before checkstyle)
- `.editorconfig` — IDE-agnostic indent settings (2-space for all file types)
- Use `// spotless:off` / `// spotless:on` comments to exclude code blocks where google-java-format produces lines exceeding 120 characters (e.g., Records with multiple validation annotations)

### Spring AI Integration

Use Spring AI interfaces (`ChatModel`, `ChatClient`) instead of direct HTTP clients for LLM communication.

### Groq API Communication Requirements

Every Groq API call must include:
- **Timeout**: Configure appropriate timeout values
- **Retry mechanism**: Implement Exponential Backoff for transient failures
- **Error handling**: Graceful degradation when LLM is unavailable

## Claude Code Actions Review Setup

This repository uses Claude Code Actions (`anthropics/claude-code-action@v1`) for automated Code Review in CI pipelines. Claude reads this CLAUDE.md file automatically during review.

**Note:** When running as GitHub Action for code review, always write in **English** (as specified in Language Rules).

### Review Scope (Global)

**DO Review** (Logic & Architecture):

- **Logical errors**: null checks, boundary conditions, race conditions
- **Security vulnerabilities**: injection attacks, improper input validation, secrets exposure
- **API design**: REST conventions, error responses, proper HTTP status codes
- **Performance**: N+1 queries, missing pagination, inefficient algorithms
- **Architecture**: proper use of interfaces, dependency injection, separation of concerns
- **Error handling**: appropriate exception types, meaningful error messages

**DO NOT Review** (handled by CI automation):

- Code formatting (Spotless + Checkstyle)
- Import ordering
- Variable naming conventions
- Missing semicolons or spaces
- Line length violations
- Test failures (JUnit)
- Code coverage (JaCoCo)

### Project-Specific Review Requirements

- All DTOs must use **Java Records**, not classes with getters/setters
- Use `ChatModel` and `ChatClient` interfaces from Spring AI (never raw HTTP clients)
- API keys must come from environment variables or secrets manager
- Never log request/response bodies that may contain sensitive data
- Validate all file uploads for MIME type and size

### Path-Specific Review Rules

#### AI Module (`**/ai/**`) — LLM Integration

Every Groq API call MUST have:

1. **Timeout**: Explicit timeout configuration (30s)
2. **Retry**: Exponential Backoff — initial delay 1s, max 3 retries, multiplier 2x. Acceptable: Spring Retry `@Retryable`, Resilience4j, or custom implementation
3. **Token Limits**: Prompt size validation before sending, response token limits in options
4. **Error Handling**: Catch `ChatClientException` and subtypes, provide fallback when LLM unavailable, never expose raw API errors
5. **Structured Output**: Use `BeanOutputConverter` or equivalent, validate parsed output

#### Upload Module (`**/upload/**`) — File Handling

1. **MIME Validation**: Validate by file content (magic bytes), NOT by extension or Content-Type header. Allowed: `image/jpeg`, `image/png`, `application/pdf`
2. **File Size**: Max 10MB, check BEFORE reading entire file into memory
3. **Path Traversal**: Sanitize filenames — remove `..`, path separators. Verify resolved path stays within upload directory
4. **Temp Files**: Delete after processing, use try-with-resources
5. **Image Preprocessing**: Resize to max 1200px width, strip EXIF metadata

#### Config Module (`**/config/**`) — Security & Secrets

1. **Secrets Management**: NEVER allow hardcoded API keys, passwords, tokens. No default values for sensitive properties
2. **Environment Separation**: `application.properties` (defaults only), env-specific profiles, environment variables for all secrets
3. **Logging**: API keys must never appear in logs, mask sensitive data
4. **External Services**: Base URLs must be configurable, explicit timeout values
5. **Security Beans**: Review CORS (not overly permissive), CSRF settings, auth setup

### Review Style

- Be concise and actionable
- Focus on **why** something is problematic, not just **what**
- Suggest specific fixes when possible
- Prioritize security and correctness over style preferences

## Pre-Review Automation Pipeline

```
PR opened / "rerun" label          PR synchronize (code push)
    │                                    │
    ▼                                    │
┌──────────────────┐     fail            │
│ Enrich PR Desc.  │ ────► STOP          │
│ (opened/rerun)   │                     │
└─────┬────────────┘                     │
      │ pass/skip                        │
      ▼                                  ▼
┌─────────────────────────────────────────┐     fail
│  Spotless (formatting) + Checkstyle     │ ────► STOP
│  (runs on every trigger)                │
└─────────────────┬───────────────────────┘
                  │ pass
                  ▼
┌─────────────────────────────────────────┐     fail
│  Unit Tests                             │ ────► STOP
│  (runs on every trigger)                │
└─────────────────┬───────────────────────┘
                  │ pass
                  ▼
┌─────────────────────────────────────────┐
│  Claude Code Actions Review             │ ──► Logic, Architecture, Context
│  + Structured Report → artifact upload  │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│  Cleanup: remove "rerun" label          │  (only on "rerun" trigger)
└─────────────────────────────────────────┘
```

**Key behavior:** On `synchronize` (code push to PR), `enrich-description` is skipped but
`spotless+checkstyle → test → claude-review` run normally. Each job uses `always()` with explicit
success check on its dependency to avoid being skipped when upstream jobs are skipped.

### Branch Naming Convention

**Required format:** `feat/task-{N}-{short-description}` or `chore/task-{N}-{description}`

This convention is used by the `enrich-description` job in `ci.yml` to automatically extract the task number from the branch name and populate the PR description with task context from `./ai/tasks.md`. The regex `task-([0-9]+)` is used for extraction.

**Examples:**
- `feat/task-5-data-models` → extracts task number 5
- `chore/task-11-pr-enrich` → extracts task number 11
- `feat/PROJ-1234-description` → no task number extracted (Jira format, future support)

### Re-running the Full Pipeline

Add the `rerun` label to a PR to trigger the full pipeline (including PR description enrichment). The label is automatically removed after the pipeline completes, allowing re-use.

### GitHub Actions Workflows

Pipeline is defined in `.github/workflows/`:
- **ci.yml** — CI pipeline: enrich-description → checkstyle → test → claude-review (on every PR)
  - Triggers: `opened`, `synchronize`, `ready_for_review`, `reopened`, `labeled` (rerun only)
  - Claude review produces structured report uploaded as artifact (`claude-review-report-pr-{N}`)
- **claude.yml** — Interactive @claude mentions (on PRs and issues)
  - Scoped `--allowedTools` whitelist: Read, Write, Edit, gh CLI (pr/issue), git (diff/log/status), mvnw (spotless/checkstyle/test)
- **pattern-police.yml** — On-demand architecture drift checker (`workflow_dispatch`)
  - Input: `pr_number`, optional `rules_path` (defaults to `CLAUDE.md`)
  - Reads path-specific review rules and verifies package boundaries in PR diff
  - Report uploaded as artifact (`pattern-audit-pr-{N}`)

### Workflow File Validation Limitation

Claude Code Action validates that the workflow file invoking it must be identical on the PR branch and the default branch (master). This is a security feature preventing PRs from tampering with the review workflow.

**Consequence:** PRs that modify `ci.yml` will always fail the `claude-review` job with: `Workflow validation failed. The workflow file must exist and have identical content to the version on the repository's default branch.`

**Workaround:** Merge such PRs with `gh pr merge <N> --squash --admin` (bypassing the failed check). After merge, subsequent PRs will pass because master now has the updated workflow. This limitation only affects PRs that directly modify workflow files — all other PRs are unaffected.

## Related Repositories

- **claude-code-action** (cloned locally): `/home/andrew/projects/review-actions/claude-code-action`
  - Source: `anthropics/claude-code-action` — the GitHub Action used for automated code review in this project
