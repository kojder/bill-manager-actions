# Product Requirements Document: Bill-Manager

## 1. Product Overview

Bill-Manager is a Spring Boot application for automated bill analysis (images/PDF) using the Groq LLM. The project serves a dual purpose:

1. **Working POC** — functional pipeline: file upload → validation → image preprocessing → AI analysis → structured JSON result
2. **Testing platform** — testing ground for Claude Code Actions automated Code Review in CI pipelines, with emphasis on path-specific review rules defined in CLAUDE.md

## 2. User Problem

Manual analysis of bills (receipts, invoices) is time-consuming and error-prone. Users need a tool that automatically extracts line items, unit prices, total amounts, and expense categories from bill images.

## 3. MVP Scope

### In Scope

- File upload (JPG, PNG, PDF) via REST API and simple HTML form
- MIME type validation by file content (magic bytes), size limit (10MB), filename sanitization
- Image preprocessing: resize to max 1200px width, strip EXIF metadata
- AI analysis via Groq API (Spring AI ChatClient) with Structured Output
- Analysis result returned as JSON (line items, amounts, categories)
- In-memory result storage (ConcurrentHashMap)
- Health check endpoint
- CI pipeline: Checkstyle → Tests → Claude Code Actions Review

### Out of Scope

- SPA frontend (simple static HTML form is provided instead)
- User authentication and authorization
- Multi-tenancy
- Supabase / external database integration
- Production deployment (Docker, Kubernetes)
- Monitoring and observability
- Batch bill processing
- Bill history and search

## 4. User Stories

### US-01: Upload and Analyze a Bill
**As a** user, **I want to** upload a bill image or PDF via form or API, **so that** I receive automatically extracted data: line items, prices, and total amount.

**Acceptance Criteria:**
- Endpoint `POST /api/bills/upload` accepts multipart/form-data file
- Supported formats: JPEG, PNG, PDF
- MIME validation by file content (not by extension)
- Size limit: 10MB
- Response contains structured JSON with analysis result

### US-02: Retrieve Analysis Result
**As a** user, **I want to** retrieve a previous analysis result by ID, **so that** I can reference it later.

**Acceptance Criteria:**
- Endpoint `GET /api/bills/{id}` returns the analysis result
- Returns 404 when analysis with given ID does not exist
- Result contains: ID, original filename, analysis data, timestamp

### US-03: CI Pipeline with Claude Code Actions Review
**As a** developer, **I want** every Pull Request to go through an automated Checkstyle → Tests → Claude Code Actions Review pipeline, **so that** Claude focuses on logic and architecture, not formatting.

**Acceptance Criteria:**
- GitHub Actions workflow triggers on every PR
- Checkstyle blocks the pipeline on formatting errors
- Tests block the pipeline on failures
- Claude Code Actions Review runs only after all checks pass

### US-04: Path-Specific Code Review
**As a** developer, **I want** Claude to apply different review rules depending on file path, **so that** the review is more precise and contextual.

**Acceptance Criteria:**
- Files in `**/ai/**` reviewed for: timeout, retry, exponential backoff, token limits
- Files in `**/upload/**` reviewed for: MIME validation, size limits, path traversal
- Files in `**/config/**` reviewed for: secrets, hardcoded values, env separation
- Path-specific rules defined in CLAUDE.md, applied automatically by Claude during review

## 5. Non-Functional Requirements

### Groq API Communication
- Explicit timeout on every call (30 seconds)
- Retry with Exponential Backoff: initial delay 1s, max 3 retries, multiplier 2x
- Graceful degradation when Groq API is unavailable (clear error, 503 status)
- Prompt size validation before sending

### Upload Security
- MIME type validation by file content (magic bytes), not by extension
- File size limit: 10MB
- Filename sanitization (path traversal protection)
- Temporary file cleanup after processing

### Configuration
- No hardcoded API keys or secrets
- Configuration via `application.properties` + environment variables
- Separation: `application.properties` (defaults) vs `application-dev.properties`

### Code Quality
- Checkstyle (Google Java Style) — zero violations
- Unit tests for business logic
- Java Records for all DTOs

## 6. Success Criteria

### Functional
- Working end-to-end flow: file upload → preprocessing → AI analysis → JSON result
- Simple HTML form for file upload
- Correct HTTP error codes for all scenarios

### Code Review
- Each of the 3 path-specific review rule sets (ai, upload, config) in CLAUDE.md exercised in at least 1 Pull Request
- Claude generates review comments related to defined rules (timeout, MIME, secrets)
- Pipeline blocks Claude review when Checkstyle or tests fail
