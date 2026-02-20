# Implementation Task List: Bill-Manager

## Rules

- Each task = 1 branch → 1 Pull Request → test Claude Code Actions Review
- Branch naming: `feat/task-{N}-{short-description}` or `chore/task-{N}-{description}`
- Tests: each task includes appropriate unit/integration tests
- DoD (Definition of Done): code compiles, tests pass, Checkstyle clean (from Task 3 onwards)

---

## Phase 0: Infrastructure

### Task 1: Repository Initialization + Documentation ✅ COMPLETED

**Status:** Committed to master (initial setup)

**Description:** Git init, file cleanup, initial commit, push to GitHub. Create documentation in `./ai/`.

**Scope:**
- `git init` + initial commit
- Files `./ai/prd.md`, `./ai/tech-stack.md`, `./ai/api-plan.md`, `./ai/tasks.md`
- Update `CLAUDE.md` (slim down, add references to `./ai/`, add review guidelines)
- Push to `git@github.com:kojder/bill-manager-actions.git`

**Claude review:** None (initial commit on main, not a PR)

**Size:** S

---

### Task 2: GitHub Actions CI Pipeline ✅ COMPLETED

**Status:** Merged to master (PR #1)

**Description:** Create CI workflow with jobs: checkstyle → test → claude-review. Configure JaCoCo for coverage reports.

**Scope:**
- New: `.github/workflows/ci.yml`
- New: `.github/workflows/claude.yml` (interactive @claude mentions)
- Modified: `pom.xml` (add JaCoCo plugin)

**Claude review:** CLAUDE.md review section (global) — first PR, workflow quality verification

**Expected review points:**
- Correct workflow configuration
- Proper job dependencies (checkstyle → test → claude-review)
- JaCoCo integration

**Size:** M

---

### Task 3: Checkstyle Configuration ✅ COMPLETED

**Status:** Merged to master (part of PR #1)

**Description:** Add Checkstyle configuration (Google Java Style), Maven plugin, format existing code.

**Scope:**
- New: `checkstyle.xml` (in root)
- Modified: `pom.xml` (add maven-checkstyle-plugin)
- Modified: existing Java files (formatting to match Google Style)

**Claude review:** CLAUDE.md review section (global)

**Verification:** `./mvnw checkstyle:check` passes with zero violations

**Size:** S

---

## Phase 1: Foundation (config/ + dto/)

### Task 4: Application Configuration Module ✅ COMPLETED

**Status:** Merged to master (PR #3)

**Description:** Package `config/` with configuration classes for Groq API and upload. Parameters configured via `application.properties` and environment variables.

**Scope:**
- New: `src/main/java/.../config/GroqApiProperties.java` — timeout, retry (count, delay, multiplier), base-url, model
- New: `src/main/java/.../config/UploadProperties.java` — max file size, allowed MIME types
- New: `src/main/java/.../config/ApiKeyValidator.java` — fail-fast validation on startup
- Modified: `src/main/resources/application.properties` — default values
- New: `src/main/resources/application-dev.properties` — development configuration
- Tests: verify configuration loading

**Claude review:** **CLAUDE.md Config Module review rules**

**Expected review points:**
- [x] No hardcoded API keys or secrets
- [x] Configurable URLs and timeouts
- [x] Environment separation (properties vs env vars)
- [x] No secrets in logs

**Implementation notes:**
- Added `ApiKeyValidator` with `@PostConstruct` for fail-fast startup validation
- API key format validation: `gsk_` prefix + 56 characters total
- Temperature set to 0.3 for optimal bill analysis
- Custom properties for timeout & retry configuration (Exponential Backoff)

**Size:** M

---

### Task 5: Data Models (DTOs) ✅ COMPLETED

**Status:** Merged to master (PR #4)

**Description:** Java Records for all DTOs as defined in `api-plan.md`.

**Scope:**
- New: `src/main/java/.../dto/BillAnalysisResult.java`
- New: `src/main/java/.../dto/BillAnalysisResponse.java`
- New: `src/main/java/.../dto/LineItem.java`
- New: `src/main/java/.../dto/ErrorResponse.java`
- Tests: JSON serialization/deserialization + validation constraint tests

**Claude review:** CLAUDE.md review section (global)

**Expected review points:**
- [x] Java Records used (not classes)
- [x] Validation annotations where needed

**Implementation notes:**
- All DTOs implemented as Java Records with Jakarta Validation annotations
- `@PositiveOrZero` on prices (allows free/discounted items), `@Positive` on quantity
- `UUID` type for `BillAnalysisResponse.id` (type-safe vs String)
- `categoryTags` in `BillAnalysisResult` is nullable (LLM may not return tags)
- Code review findings addressed: `@Positive` → `@PositiveOrZero`, `String id` → `UUID id`, added `ValidationTest`

**Size:** S

---

## Phase 2: Upload Module (upload/)

### Task 6: File Validation ✅ COMPLETED

**Status:** Merged to master (PR #5)

**Description:** File validation service: MIME type by content (magic bytes), size limit, filename sanitization.

**Scope:**
- New: `src/main/java/.../upload/FileValidationService.java` (interface)
- New: `src/main/java/.../upload/FileValidationServiceImpl.java` (implementation)
- New: `src/main/java/.../upload/FileValidationException.java` (custom exception with ErrorCode enum)
- Modified: `CLAUDE.md` — added `final` modifier convention to Code Conventions
- Tests: various file types (valid/invalid MIME), oversized file, malicious filename (`../../etc/passwd`)

**Claude review:** **CLAUDE.md Upload Module review rules**

**Expected review points:**
- [x] MIME validation by file content (magic bytes), not by extension
- [x] Size check BEFORE loading entire file into memory
- [x] Path traversal protection (sanitize `..`, path separators)
- [x] Whitelist of allowed MIME types

**Implementation notes:**
- Custom magic bytes detection for JPEG (`FF D8 FF`), PNG (`89 50 4E 47...`), PDF (`%PDF`) — no external dependency
- File size validated via `MultipartFile.getSize()` (metadata) before any content read
- Filename sanitization: path separators → `_`, `..` removal, control char filtering, leading dot stripping, 255 char limit
- `FileValidationException` with `ErrorCode` enum: `FILE_REQUIRED`, `FILE_TOO_LARGE`, `UNSUPPORTED_MEDIA_TYPE`, `FILE_UNREADABLE`
- Constructor overload with `Throwable cause` for exception chaining (IOException in MIME detection)
- Code review findings addressed: `RuntimeException` → `FileValidationException(FILE_UNREADABLE)`, missing `final` on parameter
- `final` modifier convention added to CLAUDE.md (excluding interface method parameters — Checkstyle RedundantModifier)

**Size:** M

---

### Task 7: Upload REST Controller + Error Handling ✅ COMPLETED

**Status:** Merged to master (PR #7)

**Description:** Controller with endpoints `POST /api/bills/upload` and `GET /api/bills/{id}`. Global exception handler with `@ControllerAdvice`.

**Scope:**
- New: `src/main/java/.../upload/BillUploadController.java`
- New: `src/main/java/.../upload/BillResultStore.java` (extracted from controller after review)
- New: `src/main/java/.../exception/GlobalExceptionHandler.java`
- New: `src/main/java/.../exception/AnalysisNotFoundException.java`
- Modified: `src/main/java/.../dto/BillAnalysisResponse.java` — `@NotNull` removed from `analysis` (nullable until AI), `LocalDateTime` → `Instant`
- Modified: `src/main/java/.../dto/ErrorResponse.java` — `LocalDateTime` → `Instant`
- Modified: `application.properties` — multipart upload limits
- Tests: MockMvc — 11 tests (upload valid/invalid file, retrieve result, 404, error formats, stacktrace suppression)

**Claude review:** **CLAUDE.md Upload Module review rules**

**Expected review points:**
- [x] Correct REST conventions (HTTP codes, Content-Type)
- [x] Input validation at controller level
- [x] Standardized error responses (ErrorResponse)
- [x] No internal stacktraces exposed to users

**Implementation notes:**
- `GlobalExceptionHandler` maps 9 exception types: `FileValidationException` (4 error codes), `AnalysisNotFoundException`, `MethodArgumentTypeMismatchException`, `MissingServletRequestPartException`, `MaxUploadSizeExceededException`, generic `Exception`
- `BillResultStore` (`@Component`) with `ConcurrentHashMap` — extracted from controller after code review (SRP). Task 10 may evolve this into `InMemoryResultStore`
- `Instant` instead of `LocalDateTime` for timezone-safe timestamps (review finding)
- Multipart limit set to 11MB (intentionally above 10MB app limit so `FileValidationService` provides structured error)
- `analysis: null` in response — AI analysis not yet implemented (Task 9)

**Size:** M

---

### Task 8: Image Preprocessing ✅ COMPLETED

**Status:** Merged to master (PR #8)

**Description:** Service for preparing images before sending to LLM: resize to max 1200px width, strip EXIF metadata.

**Scope:**
- New: `src/main/java/.../upload/ImagePreprocessingService.java` (interface)
- New: `src/main/java/.../upload/ImagePreprocessingServiceImpl.java` (implementation)
- New: `src/main/java/.../upload/ImagePreprocessingException.java` (custom exception with ErrorCode enum)
- Modified: `src/main/java/.../exception/GlobalExceptionHandler.java` — added `ImagePreprocessingException` handler
- Tests: 14 tests — JPEG/PNG resize, aspect ratio, EXIF stripping, PDF passthrough, transparency, edge cases, error handling

**Claude review:** **CLAUDE.md Upload Module review rules**

**Expected review points:**
- [x] Resize to max 1200px preserving aspect ratio
- [x] EXIF metadata stripped
- [x] Multiple format support (JPEG, PNG)
- [x] PDF passthrough (PDFs are not preprocessed)

**Implementation notes:**
- Pure Java (`ImageIO` + `Graphics2D`) — no external image processing dependencies
- EXIF stripping automatic via ImageIO re-encoding (metadata not carried over)
- JPEG output with explicit quality control (`ImageWriteParam`, quality=0.9)
- PNG preserves alpha channel (`TYPE_INT_ARGB`), JPEG uses `TYPE_INT_RGB`
- `BICUBIC` interpolation + `QUALITY` rendering hints for resize
- `ImagePreprocessingException` with `ErrorCode` enum: `IMAGE_READ_FAILED` (422), `PREPROCESSING_FAILED` (500)
- Images at or below 1200px width are re-encoded (strips EXIF) without resize

**Size:** M

---

## Phase 3: AI Module (ai/) + Integration

### Task 9: AI Analysis Service (Groq via Spring AI) ✅ COMPLETED

**Status:** Merged to master (PR #11)

**Description:** Core bill analysis service with LLM. ChatClient from Spring AI, timeout, retry with Exponential Backoff, Structured Output (BeanOutputConverter), graceful degradation.

**Scope:**
- New: `src/main/java/.../ai/BillAnalysisService.java` (interface)
- New: `src/main/java/.../ai/BillAnalysisServiceImpl.java` (implementation)
- New: `src/main/java/.../ai/BillAnalysisException.java` (custom exception with ErrorCode enum)
- Modified: `src/main/java/.../exception/GlobalExceptionHandler.java` — added `BillAnalysisException` handler
- Modified: `src/main/java/.../config/GroqApiProperties.java` — removed dead `timeoutSeconds` field
- Modified: `src/main/resources/application.properties` — HTTP timeout, max-tokens, model change to vision
- Modified: `src/main/resources/application-dev.properties` — removed `groq.api.timeout-seconds`
- New: `src/test/resources/logback-test.xml` — suppress expected ERROR logs from AI service during tests
- Tests: 20 tests — mocked ChatClient, input validation, retry behavior (incl. Spring AI exceptions), Bean Validation, error handling

**Claude review:** **CLAUDE.md AI Module review rules** — KEY TASK

**Expected review points:**
- [x] Explicit timeout on every Groq API call (30s) — `spring.http.client.read-timeout=30s`
- [x] Retry with Exponential Backoff (1s initial, 3 retries, 2x multiplier) — `RetryTemplate` + `ExponentialBackOffPolicy` from `GroqApiProperties`
- [x] Prompt size validation before sending — `validateInput()` checks image size (max 5MB)
- [x] Response token limits in options — `spring.ai.openai.chat.options.max-tokens=2048`
- [x] Catch Spring AI exceptions — `NonTransientAiException` (catch → SERVICE_UNAVAILABLE), `TransientAiException` (retry), `RestClientException` (retry + catch)
- [x] Fallback behavior when LLM unavailable — `SERVICE_UNAVAILABLE` error code → HTTP 503
- [x] No raw API errors exposed to users — all exceptions wrapped in `BillAnalysisException`
- [x] No PII in logs — LLM response logged by length only, not content
- [x] BeanOutputConverter or equivalent for structured output — `BeanOutputConverter<BillAnalysisResult>` with manual parse + Bean Validation
- [x] Parsed output validation before returning — `jakarta.validation.Validator` enforces `@NotBlank`, `@NotEmpty`, `@PositiveOrZero` annotations on `BillAnalysisResult`

**Implementation notes:**
- `ChatClient` fluent API with `ChatClient.Builder` (auto-configured by Spring AI) — `.prompt().user(u -> u.text(...).media(...)).call().content()`
- `RetryTemplate` (programmatic, from `spring-retry` 2.0.12 — transitive via `spring-ai-retry`) — no `spring-boot-starter-aop` needed
- Retry on `RestClientException`, `ResourceAccessException`, `TransientAiException`; catch `NonTransientAiException` as non-retryable
- `BeanOutputConverter<BillAnalysisResult>` generates JSON schema format instructions appended to user prompt
- `jakarta.validation.Validator` injected for Bean Validation of parsed LLM output (replaces manual checks)
- Model changed from `llama-3.3-70b-versatile` (text-only) to `llama-3.2-11b-vision-preview` (vision-capable)
- `BillAnalysisException` with `ErrorCode` enum: `INVALID_INPUT` (400), `UNSUPPORTED_FORMAT` (415), `PROMPT_TOO_LARGE` (400), `ANALYSIS_FAILED` (500), `INVALID_RESPONSE` (500), `SERVICE_UNAVAILABLE` (503)
- `timeoutSeconds` removed from `GroqApiProperties` — timeout controlled via `spring.http.client.read-timeout`
- PDF analysis explicitly rejected (vision API does not support PDFs) — clear error message with `UNSUPPORTED_FORMAT` (415)
- System prompt instructs LLM to extract merchant, line items, total, currency (ISO 4217), and category tags

**Size:** L

---

### Task 10: End-to-End Integration + Simple UI

**Description:** Connect the entire flow: upload → validation → preprocessing → AI analysis → result. In-memory storage, health endpoint, simple HTML upload form.

**Scope:**
- Modified: `BillUploadController.java` — orchestrate full flow
- New: `src/main/java/.../upload/InMemoryResultStore.java` — ConcurrentHashMap storage
- New: `src/main/resources/static/index.html` — simple upload form (no framework, served by Spring Boot)
- New: health endpoint (`GET /api/health`)
- Tests: integration test of the full flow with mocked Groq API

**Claude review:** CLAUDE.md review section (global) + potentially AI Module and Upload Module review rules

**Expected review points:**
- [ ] Correct component orchestration
- [ ] Thread-safe storage (ConcurrentHashMap)
- [ ] Temporary resource cleanup
- [ ] Integration test covers happy path and error paths

**Size:** L

---

## Phase 4: CI/CD Enhancements

### Task 11: PR Enrich — tasks.md Integration ✅ COMPLETED

**Status:** Merged to master (PR #4 included CI fixes)

**Description:** Enrich PR description job integrated into CI pipeline (`ci.yml`). Extracts task number from branch name (`feat/task-{N}-...`), parses task details from `./ai/tasks.md`, and injects them into the PR body via placeholder. Triggers on PR `opened` event or when `rerun` label is added. Job dependency chain: enrich → checkstyle → test → claude-review.

**Scope:**
- Modified: `.github/workflows/ci.yml` — added `enrich-description` job, job dependency chain, `rerun` label trigger, `cleanup-label` job
- New: `.github/pull_request_template.md` — PR template with `<!-- TASK_PLACEHOLDER -->` marker and review evaluation sections
- New: GitHub label `rerun` — triggers full pipeline re-run including enrich

**Implementation notes:**
- Task content written to temp file + python3 for safe placeholder replacement (avoids bash shell interpolation of markdown)
- `gh pr edit --body-file` for safe PR body update
- Graceful warnings when: no task number in branch name, `tasks.md` not found, task not found in file
- `rerun` label auto-removed by `cleanup-label` job after pipeline completes
- `checkstyle` runs when enrich succeeds or is skipped (synchronize event), blocks on failure
- Random labels (other than `rerun`) do not trigger the pipeline

**Claude review:** CLAUDE.md review section (global) — workflow quality, script correctness

**Size:** M

---

### Task 12: PR Enrich — Jira Integration

**Description:** Extend the PR Enrich workflow with Jira integration. Fetch ticket description via Jira REST API based on ticket key extracted from branch name (e.g., `feat/PROJ-1234-description`). Replace a dedicated placeholder in PR body with Jira ticket title, description, acceptance criteria, and a direct link to the ticket.

**Scope:**
- Modified: `.github/workflows/ci.yml` — add Jira API step to `enrich-description` job (conditional, runs only when Jira key detected)
- Modified: `.github/pull_request_template.md` — add `<!-- JIRA_PLACEHOLDER -->` marker
- New: GitHub repository secrets: `JIRA_BASE_URL`, `JIRA_API_TOKEN`, `JIRA_USER_EMAIL`

**Claude review:** **CLAUDE.md Config Module review rules** (secrets handling)

**Expected review points:**
- [ ] Jira API token stored as GitHub secret, never hardcoded
- [ ] Graceful fallback when Jira API is unavailable or ticket not found
- [ ] No sensitive data (API token, email) exposed in workflow logs
- [ ] Branch name regex handles both task-N and PROJ-1234 patterns

**Implementation notes:**
- Jira REST API: `GET /rest/api/3/issue/{issueKey}` with Basic Auth (email + API token)
- Extract fields: `summary`, `description`, `acceptance criteria` (custom field)
- Consider reusable workflow (`workflow_call`) for use across multiple repositories
- Rate limiting: Jira Cloud API has rate limits — single call per PR is fine

**Size:** M

---

### Task 13: Enhanced Review Pipeline — Structured Reports, Security & Pattern Police ✅ COMPLETED

**Status:** Committed to master

**Description:** Enhance CI review pipeline with structured reports, security hardening, and on-demand architecture audit. Inspired by patterns from `claude-code-action-ideas` repository (diff-aware-pr-reviewer, pattern-police, allowedTools security).

**Scope:**
- Modified: `.github/workflows/ci.yml` — enhanced `claude-review` job with execution plan, structured markdown report, artifact upload
- Modified: `.github/workflows/claude.yml` — added scoped `--allowedTools` whitelist (security hardening)
- New: `.github/workflows/pattern-police.yml` — on-demand architecture drift checker (`workflow_dispatch`)
- Modified: `.gitignore` — added `reports/`
- Modified: `CLAUDE.md` — updated pipeline diagram and workflow documentation
- Modified: `README.md` — added structured reports, pattern-police, and tool restrictions sections
- Modified: `ai/tech-stack.md` — updated pipeline diagram and added new sections
- Modified: `docs/claude-actions-context.md` — updated workflow descriptions and security section

**Claude review:** CLAUDE.md review section (global) — workflow quality, security patterns

**Implementation notes:**
- `ci.yml` review now uses `fetch-depth: 0` (full history) and `GH_TOKEN` env pattern
- Structured report saved as `reports/pr-{N}-review.md` and uploaded as artifact `claude-review-report-pr-{N}`
- `claude.yml` allowedTools: Read, Write, Edit, gh CLI (pr/issue), git (diff/log/status), mvnw (checkstyle/test)
- Pattern Police reads CLAUDE.md path-specific rules and checks PR diff for architecture violations
- All patterns based on `claude-code-action-ideas` repository best practices

**Size:** M

---

## Claude Code Actions Review Mapping

| CLAUDE.md review section | Tasks | Key review points |
|--------------------------|-------|-------------------|
| Global review scope | 2, 3, 5, 10, 11, 13 | Architecture, Records, REST conventions, workflow quality, CI security |
| Config Module rules | 4, 12 | Secrets, env separation, configurable URLs, Jira API secrets |
| Upload Module rules | 6, 7, 8 | MIME validation, size limits, path traversal, preprocessing |
| AI Module rules | 9 | Timeout, retry, exponential backoff, structured output |

**Coverage:** Each review rule set exercised in at least 1 PR.
