---
name: spring-boot-implementer
description: "Guides implementation of Spring Boot 3.5 / Java 17 features with copy-paste-ready templates following project conventions. Use when the user asks to: implement a new feature, create a service or controller, add a REST endpoint, define DTOs or exception classes, write unit or integration tests, configure properties, integrate with Spring AI / ChatClient, or scaffold a new module. Provides step-by-step workflows ensuring SOLID compliance, proper error handling, and test coverage."
user-invokable: true
argument-hint: "[feature description, component type, or 'end-to-end' for full feature workflow]"
---

# Spring Boot Implementer

You are a Senior Java Developer implementing production-ready Spring Boot code.
Generate code that follows this project's exact conventions — templates in reference
files are extracted from the actual codebase and serve as the gold standard.

**Scope boundary:**
- This skill = **HOW to write** code (templates, step sequences, file placement)
- CLAUDE.md = **WHAT conventions** to follow (`final` rules, SOLID, formatting, build commands)
- Do NOT duplicate rules from CLAUDE.md — templates already embody them in code

## Implementation Workflow

1. **Analyze requirement** — identify which components are needed (DTO, service, controller, etc.)
2. **Load references** — use the Component Type Map below to select relevant files
3. **Implement bottom-up** — DTOs/exceptions first, then services, then controllers, config last
4. **Write tests alongside** — for each component, write its test before moving to the next
5. **Verify** — run the Verification Checklist after all code is written

## Component Type Map

Load references based on the task at hand — do not load all at once:

| Task | References to Load |
|------|-------------------|
| New DTO or data model | `dto-exception.md` |
| New service (business logic) | `service.md`, `dto-exception.md` |
| New REST endpoint | `controller.md`, `dto-exception.md`, `service.md` |
| New Spring AI / LLM integration | `spring-ai.md`, `service.md`, `dto-exception.md` |
| New configuration properties | `config.md` |
| Write tests for existing code | `testing.md` |
| New feature (end-to-end) | All references, follow End-to-End Workflow below |

## End-to-End Feature Workflow

When implementing a complete feature across all layers, follow this sequence:

1. **Define data models** — Load `dto-exception.md`.
   Create Request/Response Records in `dto/` package with Jakarta Validation annotations.

2. **Create custom exception** — Load `dto-exception.md`.
   Create exception class with nested `ErrorCode` enum in the feature module package.

3. **Implement service** — Load `service.md`.
   Create interface (no `final` on params), then `@Service` implementation.
   Inject dependencies via constructor. Add input validation in private helper.

4. **Create REST controller** — Load `controller.md`.
   Create `@RestController` that delegates to the service interface.
   Return `ResponseEntity<DTO>` with explicit HTTP status.

5. **Register exception handler** — Load `controller.md` (GlobalExceptionHandler Extension).
   Add `@ExceptionHandler` method and ErrorCode-to-HttpStatus mapping
   in `exception/GlobalExceptionHandler.java`.

6. **Add configuration** (if needed) — Load `config.md`.
   Create `@ConfigurationProperties` Record. Add entries to `application.properties`,
   `application-dev.properties`, and `src/test/resources/application.properties`.
   Register in `@EnableConfigurationProperties` in `BillManagerApplication.java`.

7. **Write tests** — Load `testing.md`.
   - Controller: `@WebMvcTest` with `@MockitoBean`
   - Service: pure unit test with `mock()` + direct instantiation
   - Config: `@SpringJUnitConfig` with `@TestPropertySource`

8. **Verify** — run the Verification Checklist below.

## Package Structure

Where new files go in the module layout:

```
src/main/java/com/example/bill_manager/
  {module}/                          # Feature module (e.g., upload/, ai/, payment/)
    {Feature}Service.java            # Interface
    {Feature}ServiceImpl.java        # @Service implementation
    {Feature}Exception.java          # Custom exception with ErrorCode enum
    {Feature}Controller.java         # @RestController (if module has endpoints)
  config/                            # All @ConfigurationProperties classes
  dto/                               # Shared DTOs (cross-module Records)
  exception/                         # GlobalExceptionHandler, shared exceptions
```

## Reference Documentation

Load only when implementing the relevant component — do not read all at once.

### [references/dto-exception.md](references/dto-exception.md)
**Load when:** Creating data models, response types, or custom exceptions.
Contains: Record DTO templates with Jakarta Validation, response wrapper pattern,
spotless guard usage, custom exception template with ErrorCode enum, naming conventions.

### [references/service.md](references/service.md)
**Load when:** Creating business logic components or service classes.
Contains: Interface + @Service Impl templates, constructor injection pattern,
input validation pattern, private helper decomposition, SLF4J logging guidelines.

### [references/controller.md](references/controller.md)
**Load when:** Creating REST endpoints or registering new exception handlers.
Contains: @RestController template, POST/GET endpoint patterns,
GlobalExceptionHandler extension procedure (step-by-step), ErrorCode-to-HttpStatus mappings.

### [references/config.md](references/config.md)
**Load when:** Adding externalized configuration or startup validators.
Contains: @ConfigurationProperties Record template, nested config records,
properties file entries (main/dev/test), bean registration, startup validator pattern.

### [references/testing.md](references/testing.md)
**Load when:** Writing tests for any component type.
Contains: @WebMvcTest controller test, pure unit service test, @ConfigurationProperties test,
@Nested organization, AssertJ patterns, exception testing, ChatClient mock chain setup.

### [references/spring-ai.md](references/spring-ai.md)
**Load when:** Integrating with LLM / Groq API via Spring AI.
Contains: ChatClient.Builder injection, system prompt pattern, ChatClient call chain,
Media builder for multimodal, BeanOutputConverter, output validation with jakarta.validation,
RetryTemplate with ExponentialBackOffPolicy, exception handling for AI calls.

## Verification Checklist

Run after every implementation, in this order:

```bash
./mvnw spotless:apply          # Auto-fix formatting
./mvnw spotless:check          # Verify formatting passes
./mvnw checkstyle:check        # Verify code conventions
./mvnw test                    # Run all tests
```

Fix any failures before proceeding. Do not commit code that fails any gate.

## Implementation Principles

Short reminders — CLAUDE.md has the full rules:

- Create interface before implementation (Open/Closed Principle)
- Controllers delegate immediately — no business logic
- Every custom exception must be registered in `GlobalExceptionHandler`
- Every `@ConfigurationProperties` must be registered in `BillManagerApplication`
- Test properties in `src/test/resources/application.properties` must cover all new configs
- Never expose raw API errors or stack traces in responses
