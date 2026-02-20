# Technology Stack: Bill-Manager

## 1. Technologies

| Component | Technology | Version | Rationale |
|-----------|-----------|---------|-----------|
| Runtime | Java | 17 | LTS, required by Spring Boot 3.x |
| Framework | Spring Boot | 3.5.10 | Current LTS, Spring AI ecosystem |
| AI | Spring AI (OpenAI starter) | 1.1.2 | Groq API compatibility via OpenAI protocol |
| LLM Provider | Groq | - | Fast inference, free tier, OpenAI-compatible endpoint |
| Web | Spring Web | - | REST endpoints (upload, analysis) |
| Reactive | Spring WebFlux | - | Reactive operations (Groq communication) |
| Validation | Bean Validation (Jakarta) | - | Standard DTO validation |
| Build | Maven (wrapper) | 3.9.12 | mvnw included in repository |
| Linter | Checkstyle | 10.23.1 | Static code analysis, blocking in CI |
| CI/CD | GitHub Actions | - | Native integration with Claude Code Actions Review |
| Code Review | Claude Code Actions | v1 | Automated review via `anthropics/claude-code-action` |
| Utility | Lombok | - | Boilerplate reduction |

## 2. Application Architecture

### Flow Diagram

```
                    POST /api/bills/upload
                           │
                           ▼
                ┌──────────────────────┐
                │  BillUploadController │  (upload/)
                └──────────┬───────────┘
                           │
                           ▼
                ┌──────────────────────┐
                │ FileValidationService │  (upload/)
                │  - MIME (magic bytes) │
                │  - size (10MB)       │
                │  - filename sanitize │
                └──────────┬───────────┘
                           │
                           ▼
                ┌──────────────────────┐
                │ImagePreprocessing-   │  (upload/)
                │Service               │
                │  - resize to 1200px  │
                │  - strip EXIF        │
                └──────────┬───────────┘
                           │
                           ▼
                ┌──────────────────────┐
                │ BillAnalysisService  │  (ai/)
                │  - ChatClient        │
                │  - timeout 30s       │
                │  - retry 3x (exp.)   │
                │  - structured output │
                └──────────┬───────────┘
                           │
                           ▼
                ┌──────────────────────┐
                │ BillAnalysisResult   │  (dto/)
                │  - merchantName      │
                │  - items[]           │
                │  - totalAmount       │
                │  - categoryTags[]    │
                └──────────────────────┘
```

## 3. Package Structure

```
com.example.bill_manager/
├── BillManagerApplication.java
├── config/         # Application configuration
│   ├── ApiKeyValidator.java         # Fail-fast startup validation
│   ├── GroqApiProperties.java       # retry, base-url, model
│   └── UploadProperties.java        # max-size, allowed MIME types
│
├── ai/             # LLM integration (Groq via Spring AI)
│   ├── BillAnalysisService.java     # Interface
│   ├── BillAnalysisServiceImpl.java # ChatClient, structured output, retry
│   └── BillAnalysisException.java   # Custom exception with ErrorCode enum
│
├── upload/         # Upload, validation, preprocessing
│   ├── BillUploadController.java    # REST endpoints
│   ├── BillResultStore.java         # In-memory storage (ConcurrentHashMap)
│   ├── FileValidationService.java   # Interface
│   ├── FileValidationServiceImpl.java # MIME magic bytes, size, filename
│   ├── FileValidationException.java # Custom exception with ErrorCode enum
│   ├── ImagePreprocessingService.java    # Interface
│   ├── ImagePreprocessingServiceImpl.java # resize, strip EXIF
│   └── ImagePreprocessingException.java  # Custom exception with ErrorCode enum
│
├── dto/            # Java Records (immutable DTOs)
│   ├── BillAnalysisResult.java     # AI analysis result
│   ├── BillAnalysisResponse.java   # response wrapper (id, filename, result, timestamp)
│   ├── LineItem.java               # single bill line item
│   └── ErrorResponse.java          # standardized error
│
└── exception/      # Global error handling
    ├── GlobalExceptionHandler.java  # @ControllerAdvice
    └── AnalysisNotFoundException.java
```

### Structural Decisions

| Decision | Rationale |
|----------|-----------|
| `processing/` merged into `upload/` | Preprocessing is part of the upload flow. CLAUDE.md Upload Module review rules already cover "Image Preprocessing" section. No need for a separate package. |
| No `storage/` package | POC uses in-memory `ConcurrentHashMap`. No need for a dedicated persistence module. |
| Added `exception/` | Centralized error handling with `@ControllerAdvice` instead of scattered try-catch blocks. |
| Added `config/` | Dedicated configuration — exercised by CLAUDE.md Config Module review rules in Claude Code Actions review. |

## 4. Checkstyle — Static Code Analysis

Checkstyle is a static analysis tool that enforces coding conventions and style rules at compile time
(no application execution required). It parses Java source files into an AST (Abstract Syntax Tree)
and runs configurable modules (rules) against it.

- **Documentation**: https://checkstyle.org/
- **Available modules**: https://checkstyle.org/checks.html

### Configuration

| File | Purpose |
|------|---------|
| `checkstyle.xml` | Module configuration — list of active rules |
| `pom.xml` (maven-checkstyle-plugin) | Maven integration — config location, fail policy, severity |

### How It Works

```
./mvnw checkstyle:check
        │
        ▼
maven-checkstyle-plugin (pom.xml)
        │  reads configLocation = checkstyle.xml
        ▼
Checker (root module)
├── FileTabCharacter          ← file-level checks
├── LineLength (max 120)
└── TreeWalker                ← parses Java AST
    ├── AvoidStarImport       ← import rules
    ├── UnusedImports
    ├── TypeName              ← naming rules
    ├── EqualsHashCode        ← coding rules
    ├── LeftCurly             ← block checks
    ├── WhitespaceAround      ← whitespace rules
    ├── ModifierOrder         ← modifier rules
    ├── FinalParameters       ← enforces final on parameters
    └── ...
```

### Active Modules

Modules are organized in `checkstyle.xml` by category:

| Category | Modules | Purpose |
|----------|---------|---------|
| Imports | `AvoidStarImport`, `UnusedImports`, `RedundantImport` | Clean import statements |
| Naming | `TypeName`, `MethodName`, `ConstantName`, `LocalVariableName`, `MemberName`, `PackageName`, `ParameterName` | Java naming conventions |
| Coding | `EqualsHashCode`, `SimplifyBooleanExpression`, `SimplifyBooleanReturn`, `EmptyStatement`, `MissingSwitchDefault`, `FallThrough`, `OneStatementPerLine`, `MultipleVariableDeclarations` | Common coding mistakes |
| Blocks | `LeftCurly`, `RightCurly`, `NeedBraces`, `EmptyBlock` | Brace style |
| Whitespace | `WhitespaceAround`, `GenericWhitespace`, `NoWhitespaceBefore` | Spacing rules |
| Modifiers | `ModifierOrder`, `RedundantModifier`, `FinalParameters` | Modifier usage |

### Known Interaction

`FinalParameters` enforces `final` on method/constructor/catch/for-each parameters.
`RedundantModifier` flags `final` on interface abstract method parameters as redundant.
These two modules conflict for interface methods — convention is to skip `final` on interface
method signatures (documented in CLAUDE.md).

## 5. Groq API Integration

Groq provides an OpenAI-compatible endpoint, allowing the use of Spring AI OpenAI starter without additional configuration.

### Configuration in `application.properties`

```properties
spring.ai.openai.api-key=${GROQ_API_KEY}
spring.ai.openai.base-url=https://api.groq.com/openai/v1
spring.ai.openai.chat.options.model=llama-3.2-11b-vision-preview
spring.ai.openai.chat.options.temperature=0.3
spring.ai.openai.chat.options.max-tokens=2048
```

### Integration Pattern

- **ChatClient** (Spring AI) — primary interface for LLM communication
- **BeanOutputConverter** — conversion of LLM response to Java Records (structured output)
- **RetryTemplate** (Spring Retry, programmatic) — Exponential Backoff for transient failures (1s → 2s → 4s, max 3 retries)
- **jakarta.validation.Validator** — Bean Validation of parsed LLM output
- **Timeout** — 30 seconds per call (`spring.http.client.read-timeout`)

## 6. Storage Strategy

### MVP (current)
- **ConcurrentHashMap** in application memory
- Key: UUID (generated on upload), Value: `BillAnalysisResponse`
- Data lost on restart — acceptable for POC

### Upgrade Path (future)
1. **H2 in-memory** — Spring Data JPA, Flyway migrations, data lost on restart
2. **PostgreSQL** — Supabase (local Docker) or standalone PostgreSQL

## 7. CI/CD Pipeline

```
PR opened / "rerun" label          PR synchronize (code push)
    │                                    │
    ▼                                    │
┌──────────────────┐                     │
│ Enrich PR Desc.  │                     │
│ (opened/rerun)   │                     │
└─────┬────────────┘                     │
      │ pass/skip                        │
      ▼                                  ▼
┌─────────────────────────────────────────┐     fail
│  Spotless (formatting) + Checkstyle     │ ────► STOP
└─────────────────┬───────────────────────┘
                  │ pass
                  ▼
┌─────────────────────────────────────────┐     fail
│  Unit Tests                             │ ────► STOP
└─────────────────┬───────────────────────┘
                  │ pass
                  ▼
┌─────────────────────────────────────────┐
│  Claude Code Actions Review             │ ──► Logic, Architecture
│  + Structured Report → artifact upload  │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│  Cleanup: remove "rerun" label          │  (only on "rerun" trigger)
└─────────────────────────────────────────┘
```

On every code push (`synchronize`), the full chain `spotless+checkstyle → test → claude-review` runs.
Enrichment only runs on PR open or `rerun` label. Each job uses `always()` with explicit
success check on its dependency to propagate correctly when upstream jobs are skipped.

### Structured Review Reports

Claude review produces a markdown report uploaded as a workflow artifact (`claude-review-report-pr-{N}`).
Report structure: Execution Plan → Summary → Strengths → Risks/Bugs → Path-Specific Rule Compliance → Suggested Patches → Next Actions.

### On-Demand Workflows

- **pattern-police.yml** — Architecture drift checker (`workflow_dispatch`). Reads CLAUDE.md review rules
  and verifies PR changes respect package boundaries. Report: `pattern-audit-pr-{N}`.

### Tool Restrictions

All Claude workflows use scoped `--allowedTools` whitelists to enforce least-privilege access.
Interactive `@claude` (claude.yml) is limited to: Read, Write, Edit, gh CLI, git read commands, and mvnw.
