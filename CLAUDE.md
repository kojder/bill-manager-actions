# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Language Rules

- **Communication**: Always communicate with the user in **Polish**
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

# Run application
./mvnw spring-boot:run

# Package application
./mvnw clean package
```

## Project Documentation

Detailed project documentation is maintained in the `./ai/` directory:

- `./ai/prd.md` - Product requirements, user stories, MVP scope
- `./ai/tech-stack.md` - Technology stack, architecture, module structure
- `./ai/api-plan.md` - REST API plan, data models, error codes
- `./ai/tasks.md` - Implementation task list with Claude Code Actions review mapping

## Architecture Overview

**Bill-Manager** is a Spring Boot 3.5.x application for automated bill analysis using LLMs.
The primary purpose of this repository is testing Claude Code Actions (`anthropics/claude-code-action`) automated Code Review in CI pipelines.

For detailed technology stack and module structure, see `./ai/tech-stack.md`.

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

### Spring AI Integration

Use Spring AI interfaces (`ChatModel`, `ChatClient`) instead of direct HTTP clients for LLM communication.

### Groq API Communication Requirements

Every Groq API call must include:
- **Timeout**: Configure appropriate timeout values
- **Retry mechanism**: Implement Exponential Backoff for transient failures
- **Error handling**: Graceful degradation when LLM is unavailable

## Claude Code Actions Review Setup

This repository uses Claude Code Actions (`anthropics/claude-code-action@v1`) for automated Code Review in CI pipelines. Claude reads this CLAUDE.md file automatically during review.

**Note:** When running as GitHub Action for code review, write all review comments in **English**.

### Review Scope (Global)

**DO Review** (Logic & Architecture):

- **Logical errors**: null checks, boundary conditions, race conditions
- **Security vulnerabilities**: injection attacks, improper input validation, secrets exposure
- **API design**: REST conventions, error responses, proper HTTP status codes
- **Performance**: N+1 queries, missing pagination, inefficient algorithms
- **Architecture**: proper use of interfaces, dependency injection, separation of concerns
- **Error handling**: appropriate exception types, meaningful error messages

**DO NOT Review** (handled by CI automation):

- Code formatting (Checkstyle)
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
PR Created
    │
    ▼
┌─────────────┐     fail
│  Checkstyle │ ──────────► Pipeline STOP (no Claude review)
└─────┬───────┘
      │ pass
      ▼
┌─────────────┐     fail
│ Unit Tests  │ ──────────► Pipeline STOP
└─────┬───────┘
      │ pass
      ▼
┌───────────────────┐
│  Claude Code      │ ──────────► Focus: Logic, Architecture, Context
│  Actions Review   │
└───────────────────┘
```

### GitHub Actions Workflows

Pipeline is defined in `.github/workflows/`:
- **ci.yml** — CI pipeline: checkstyle → test → claude-review (on every PR)
- **claude.yml** — Interactive @claude mentions (on PRs and issues)
