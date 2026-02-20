# Security Review Checklist (OWASP 2025)

## Table of Contents

- [Upload Security](#upload-security)
- [Secrets Management](#secrets-management)
- [Logging Security](#logging-security)
- [A01: Broken Access Control](#a01-broken-access-control)
- [A02: Security Misconfiguration](#a02-security-misconfiguration)
- [A05: Injection](#a05-injection)
- [A10: Error Response Security](#a10-error-response-security)

---

## Upload Security

### MIME Validation (Magic Bytes)

File type MUST be validated by content inspection, NEVER by extension or Content-Type header.

**Required magic bytes:**

| Format | Magic Bytes (hex) | Length |
|--------|------------------|--------|
| JPEG | `FF D8 FF` | 3 bytes |
| PNG | `89 50 4E 47 0D 0A 1A 0A` | 8 bytes |
| PDF | `25 50 44 46` (`%PDF`) | 4 bytes |

**Checklist:**
- [ ] File content read with `InputStream`, not `getContentType()` or filename extension
- [ ] Magic bytes compared using `Arrays.equals()` on byte prefix
- [ ] Allowed MIME types checked against configurable list from `UploadProperties`
- [ ] `IOException` during content read wrapped in custom exception (not propagated raw)
- [ ] Empty file check before magic byte inspection

### File Size Validation

- [ ] Size checked via `MultipartFile.getSize()` (metadata) BEFORE reading content into memory
- [ ] Max size from `@ConfigurationProperties` (not hardcoded constant)
- [ ] Spring multipart limit (`spring.servlet.multipart.max-file-size`) set ABOVE app limit
      to let application provide structured `ErrorResponse` instead of generic 413

### Filename Sanitization

- [ ] Path separators (`\`, `/`) replaced with `_`
- [ ] Directory traversal (`..`) removed in a loop (handles `....` collapsing to `..`)
- [ ] Control characters filtered (codepoints below 32)
- [ ] Leading dots stripped (prevents hidden files on Unix)
- [ ] Length capped (255 characters max)
- [ ] Blank result defaults to safe name (`unnamed_file`)
- [ ] Resolved path verified to stay within upload directory (canonical path check)

### Image Preprocessing

- [ ] `ImageWriter` availability checked with `hasNext()` before calling `.next()`
- [ ] `ImageIO.write()` return value checked (returns `false` if no writer found)
- [ ] `Graphics2D` disposed in `finally` block (prevent resource leak)
- [ ] `BICUBIC` interpolation hint for quality resize
- [ ] Aspect ratio preserved during resize (max 1200px width)
- [ ] EXIF metadata stripped via re-encoding (write to new BufferedImage)
- [ ] PNG preserves alpha channel (`TYPE_INT_ARGB`), JPEG uses `TYPE_INT_RGB`
- [ ] Temp files deleted after processing (try-with-resources or finally block)

---

## Secrets Management

### API Keys

- [ ] All API keys from environment variables: `${GROQ_API_KEY}`
- [ ] NO default values for sensitive properties in `@ConfigurationProperties`
- [ ] Fail-fast validation on startup (`@PostConstruct` in `ApiKeyValidator`)
- [ ] Key format validated where possible (e.g., Groq keys: `gsk_` prefix + 56 chars)
- [ ] `.env` file in `.gitignore` (never committed to repository)
- [ ] `.env.example` contains placeholder values only (no real keys)

### Configuration Security

- [ ] `application.properties` contains only non-sensitive defaults
- [ ] Profile-specific properties (`application-dev.properties`) for development
- [ ] Environment variables for all secrets (not system properties or command-line args)
- [ ] Base URLs for external services configurable via properties (not hardcoded)
- [ ] Explicit timeout values on all external service connections

---

## Logging Security

- [ ] API keys NEVER appear in log output (at any log level)
- [ ] LLM response content logged by length only, not full text:
      `LOG.error("Failed to parse LLM response (length={})", responseText.length(), e)`
- [ ] Request/response bodies that may contain sensitive data NOT logged
- [ ] User-uploaded file content NOT logged (log filename and size only)
- [ ] Exception messages reviewed — no internal paths or credentials leaked
- [ ] SLF4J parameterized logging used (not string concatenation):
      `LOG.info("Processing file: {}", filename)` not `LOG.info("Processing: " + filename)`

---

## A01: Broken Access Control

- [ ] All REST endpoints annotated with `@PreAuthorize` (method-level security)
- [ ] Actuator endpoints restricted (see A02 below)
- [ ] No endpoint returns data belonging to other users without authorization check
- [ ] CORS configuration not overly permissive (no `allowedOrigins("*")` in production)
- [ ] CSRF protection enabled for state-changing operations (or explicitly justified if disabled)
- [ ] HTTP method restrictions match intended operations (no GET for mutations)

---

## A02: Security Misconfiguration

### Actuator Endpoints

- [ ] Only `/actuator/health` exposed by default
- [ ] `/actuator/env`, `/actuator/configprops`, `/actuator/beans` NOT publicly accessible
- [ ] Actuator endpoints require authentication in production
- [ ] `management.endpoints.web.exposure.include` explicitly configured (not `*`)

### Spring Security Configuration

- [ ] `SecurityFilterChain` bean defined (not deprecated `WebSecurityConfigurerAdapter`)
- [ ] Debug mode disabled in production (`spring.security.debug=false`)
- [ ] Default Spring Security headers enabled (X-Frame-Options, X-Content-Type-Options, etc.)
- [ ] HTTPS enforced in production (redirect HTTP to HTTPS)

---

## A05: Injection

### SQL/JPQL Injection

- [ ] NO string concatenation in any query (SQL, JPQL, HQL, native)
- [ ] Parameter binding used everywhere: `:paramName` or `?1`
- [ ] `@Query` values are static strings (no dynamic query building)
- [ ] If `Specification` or `CriteriaBuilder` used — parameters bound via API, not concatenated

**Red flag (report as Critical):**
```java
// NEVER — SQL injection vulnerability
@Query("SELECT u FROM User u WHERE u.name = '" + name + "'")

// CORRECT — parameterized query
@Query("SELECT u FROM User u WHERE u.name = :name")
List<User> findByName(@Param("name") String name);
```

### Command Injection

- [ ] No `Runtime.exec()` or `ProcessBuilder` with user-supplied input
- [ ] If file paths constructed from user input — canonicalize and validate against allowed base

---

## A10: Error Response Security

All error responses must be sanitized before reaching the client.

**Checklist:**
- [ ] Generic `Exception` handler returns "An unexpected error occurred" (no details)
- [ ] No stacktraces in ANY error response (including validation errors)
- [ ] No internal class names, package paths, or framework internals exposed
- [ ] No database column names, table names, or SQL fragments in error messages
- [ ] Custom exceptions wrap all third-party exceptions before propagating
- [ ] Error codes are enum-based (finite, documented set)

**Verify in GlobalExceptionHandler — generic catch-all handler:**
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGenericException(final Exception ex) {
  LOG.error("Unhandled exception", ex);  // Log full details server-side
  return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(new ErrorResponse(
          "INTERNAL_ERROR",
          "An unexpected error occurred",  // Generic message to client
          Instant.now()));
}
```

The generic handler MUST NOT include `ex.getMessage()` in the response body —
internal exception messages may reveal infrastructure details.
