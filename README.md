# Bill-Manager

## 1. Project Objective

Build an application for automated bill analysis (images/PDF) using LLMs (Groq) and test remote, automated Code Review mechanisms provided by Claude Code Actions (`anthropics/claude-code-action`). The project serves as a testing ground for context management (CLAUDE.md review guidelines) for AI within the Continuous Integration (CI) pipeline.

## 2. Architecture & Technologies

### Backend (Spring Boot)

- **Runtime**: Java 17, Spring Boot 3.5.x
- **Spring AI** (OpenAI Starter): Communication with Groq API (OpenAI protocol compatibility)
- **Image Processing**: java.awt for optimizing image dimensions before LLM submission
- **Storage**: In-memory (ConcurrentHashMap) — POC scope, no external database
- **Principles**: SOLID, Clean Code, Java 17 best practices

### Frontend (Web)

- Simple static HTML upload form (no framework, served by Spring Boot)

### AI & Data Infrastructure

- **LLM**: Groq (via Spring AI ChatClient) — content analysis and data extraction to JSON

## 3. Application Workflow

1. **Upload**: User uploads a file via browser or REST API (`POST /api/bills/upload`)
2. **Validation**: MIME type validation by file content (magic bytes), size limit (10MB), filename sanitization
3. **Pre-processing**: Scale image to max 1200px width, strip EXIF metadata
4. **AI Analysis**: Spring AI `ChatClient` with Structured Output (extraction of: items, unit prices, total amount, category tags)
5. **Result**: JSON response with analysis result, retrievable via `GET /api/bills/{id}`

## 4. Checkstyle — Static Code Analysis

The project uses [Checkstyle](https://checkstyle.org/) (v10.23.1) for static code analysis, enforced as a blocking CI step. Configuration is defined in `checkstyle.xml` with modules organized by category:

- **Imports**: no star imports, no unused/redundant imports
- **Naming**: Java conventions for types, methods, variables, constants, packages
- **Coding**: equals/hashCode contract, boolean simplification, switch default, fall-through
- **Blocks**: brace style, no empty blocks
- **Whitespace**: spacing around operators and keywords
- **Modifiers**: modifier order, redundant modifiers, `final` on parameters

```bash
# Run Checkstyle locally
./mvnw checkstyle:check
```

For detailed module list see `checkstyle.xml` and [Checkstyle documentation](https://checkstyle.org/checks.html).

## 5. CI Pipeline & Automated Code Review

### Pipeline Flow

```
PR opened / "rerun" label          PR synchronize (code push)
    │                                    │
    ▼                                    │
Enrich PR Description                   │
(task context from ai/tasks.md)          │
    │                                    │
    ▼                                    ▼
Checkstyle (Google Java Style)  ──── runs on every trigger
    │
    ▼
Unit Tests (JUnit 5 + JaCoCo)  ──── runs on every trigger
    │
    ▼
Claude Code Actions Review      ──── runs on every trigger
    │
    ▼
Cleanup (auto-remove "rerun" label, only on rerun trigger)
```

**Key behavior:** On every code push (`synchronize`), the full chain `Checkstyle → Unit Tests → Claude Code Review` runs. PR description enrichment only runs on PR open or when the `rerun` label is added.

### PR Description Enrichment

The `enrich-description` job automatically populates the PR description with task context from `./ai/tasks.md` based on the branch name. Requires branch naming convention: `feat/task-{N}-description` or `chore/task-{N}-description`.

### Re-running the Pipeline

Add the `rerun` label to a PR to trigger the full pipeline including PR description enrichment. The label is automatically removed after the pipeline completes.

### Context Management (`CLAUDE.md`)

Claude Code Action reads `CLAUDE.md` automatically from the repository root. Review instructions include:

- **Scope**: Skip formatting issues (handled by Checkstyle). Focus on logical errors, security vulnerabilities, and API design
- **Architecture**: Require Java Records for DTOs and Spring AI interfaces instead of direct HTTP clients
- **Security**: Ensure API keys are neither logged nor hardcoded

### Path-specific Review Rules

Path-specific review rules are defined in `CLAUDE.md` under "Path-Specific Review Rules":

| Path Pattern | Review Focus |
|-------------|--------------|
| `**/ai/**` | Timeout, Retry (Exponential Backoff), token limits, structured output |
| `**/upload/**` | MIME validation (magic bytes), file size limits, path traversal |
| `**/config/**` | Secrets exposure, hardcoded values, env separation |

## 6. Project Setup

- **Project**: Maven
- **Language**: Java 17
- **Spring Boot**: 3.5.x
- **Dependencies**: Spring Web, Spring AI (OpenAI), Validation, Lombok, DevTools, Actuator

```bash
# Build
./mvnw clean compile

# Run tests
./mvnw test

# Run Checkstyle
./mvnw checkstyle:check

# Run application
./mvnw spring-boot:run
```

## 7. Documentation

- `./ai/prd.md` — Product requirements, user stories, MVP scope
- `./ai/tech-stack.md` — Technology stack, architecture, module structure
- `./ai/api-plan.md` — REST API plan, data models, error codes
- `./ai/tasks.md` — Implementation task list with review mapping
- `./docs/claude-actions-context.md` — Claude Code Actions context management
