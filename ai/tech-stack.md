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
| Linter | Checkstyle | - | Google Java Style, blocking in CI |
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
│   ├── GroqApiProperties.java      # timeout, retry, base-url, model
│   └── UploadProperties.java       # max-size, allowed MIME types
│
├── ai/             # LLM integration (Groq via Spring AI)
│   └── BillAnalysisService.java    # ChatClient, structured output, retry
│
├── upload/         # Upload, validation, preprocessing
│   ├── BillUploadController.java   # REST endpoints
│   ├── FileValidationService.java  # MIME, size, sanitization
│   └── ImagePreprocessingService.java  # resize, strip EXIF
│
├── dto/            # Java Records (immutable DTOs)
│   ├── BillAnalysisResult.java     # AI analysis result
│   ├── BillAnalysisResponse.java   # response wrapper (id, filename, result, timestamp)
│   ├── LineItem.java               # single bill line item
│   └── ErrorResponse.java          # standardized error
│
└── exception/      # Global error handling
    └── GlobalExceptionHandler.java # @ControllerAdvice
```

### Structural Decisions

| Decision | Rationale |
|----------|-----------|
| `processing/` merged into `upload/` | Preprocessing is part of the upload flow. CLAUDE.md Upload Module review rules already cover "Image Preprocessing" section. No need for a separate package. |
| No `storage/` package | POC uses in-memory `ConcurrentHashMap`. No need for a dedicated persistence module. |
| Added `exception/` | Centralized error handling with `@ControllerAdvice` instead of scattered try-catch blocks. |
| Added `config/` | Dedicated configuration — exercised by CLAUDE.md Config Module review rules in Claude Code Actions review. |

## 4. Groq API Integration

Groq provides an OpenAI-compatible endpoint, allowing the use of Spring AI OpenAI starter without additional configuration.

### Configuration in `application.properties`

```properties
spring.ai.openai.api-key=${GROQ_API_KEY}
spring.ai.openai.base-url=https://api.groq.com/openai
spring.ai.openai.chat.options.model=llama-3.3-70b-versatile
spring.ai.openai.chat.options.temperature=0.1
```

### Integration Pattern

- **ChatClient** (Spring AI) — primary interface for LLM communication
- **BeanOutputConverter** — conversion of LLM response to Java Records (structured output)
- **@Retryable** (Spring Retry) — Exponential Backoff for transient failures (1s → 2s → 4s, max 3 retries)
- **Timeout** — 30 seconds per call

## 5. Storage Strategy

### MVP (current)
- **ConcurrentHashMap** in application memory
- Key: UUID (generated on upload), Value: `BillAnalysisResponse`
- Data lost on restart — acceptable for POC

### Upgrade Path (future)
1. **H2 in-memory** — Spring Data JPA, Flyway migrations, data lost on restart
2. **PostgreSQL** — Supabase (local Docker) or standalone PostgreSQL

## 6. CI/CD Pipeline

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
│  Checkstyle                             │ ────► STOP
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
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│  Cleanup: remove "rerun" label          │  (only on "rerun" trigger)
└─────────────────────────────────────────┘
```

On every code push (`synchronize`), the full chain `checkstyle → test → claude-review` runs.
Enrichment only runs on PR open or `rerun` label. Each job uses `always()` with explicit
success check on its dependency to propagate correctly when upstream jobs are skipped.
