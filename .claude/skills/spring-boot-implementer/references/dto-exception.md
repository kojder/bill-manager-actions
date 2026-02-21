# DTOs and Custom Exceptions

## Table of Contents

- [Record DTO Template](#record-dto-template)
- [Record with Jakarta Validation](#record-with-jakarta-validation)
- [Response Wrapper Record](#response-wrapper-record)
- [Spotless Guard Usage](#spotless-guard-usage)
- [Custom Exception Template](#custom-exception-template)
- [ErrorCode Naming Convention](#errorcode-naming-convention)

---

## Record DTO Template

Minimal record for simple data transfer (no validation needed):

```java
public record ErrorResponse(String code, String message, Instant timestamp) {}
```

---

## Record with Jakarta Validation

Use when data requires validation (API input/output, LLM parsed results):

```java
// spotless:off
public record BillAnalysisResult(
    @NotBlank String merchantName,
    @NotEmpty @Valid List<LineItem> items,
    @NotNull @PositiveOrZero BigDecimal totalAmount,
    @NotBlank String currency,
    List<String> categoryTags) {}
// spotless:on

public record LineItem(
    @NotBlank String name,
    @NotNull @Positive BigDecimal quantity,
    @NotNull @PositiveOrZero BigDecimal unitPrice,
    @NotNull @PositiveOrZero BigDecimal totalPrice) {}
```

**Annotation selection guide:**
- `@NotBlank` — String fields that must have content (not null, not empty, not whitespace)
- `@NotNull` — non-String fields that must be present
- `@NotEmpty` — collections that must have at least one element
- `@Valid` — cascade validation to nested Records inside collections
- `@Positive` — quantities (must be > 0)
- `@PositiveOrZero` — prices (allows free items)
- Use `BigDecimal` for monetary values, never `Double`

---

## Response Wrapper Record

Wrap service results with metadata for API responses:

```java
public record BillAnalysisResponse(
    UUID id,
    String originalFileName,
    BillAnalysisResult analysisResult,
    Instant createdAt) {}
```

---

## Spotless Guard Usage

Use `// spotless:off` / `// spotless:on` **only** when `google-java-format` produces lines
exceeding 120 characters. This typically happens with Records having multiple annotations
per component:

```java
// spotless:off
public record UploadProperties(
    @NotNull(message = "Max file size must not be null")
    @Min(value = 1, message = "Max file size must be at least 1 byte")
    Long maxFileSizeBytes,
    @NotEmpty(message = "Allowed MIME types must not be empty")
    List<String> allowedMimeTypes) {
  // spotless:on
```

**Do NOT use guards for:** simple Records, single-annotation components, or any code
where `google-java-format` output stays within 120 chars.

---

## Custom Exception Template

Every module with business logic needs a custom exception. Copy and customize:

```java
package com.example.bill_manager.TODO_MODULE;

import lombok.Getter;

@Getter
public class TODO_FeatureException extends RuntimeException {

  public enum ErrorCode {
    // TODO: Define error codes for this feature
    INVALID_INPUT,
    PROCESSING_FAILED
  }

  private final ErrorCode errorCode;

  public TODO_FeatureException(final ErrorCode errorCode, final String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public TODO_FeatureException(
      final ErrorCode errorCode, final String message, final Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }
}
```

**Checklist after creating:**
1. Replace `TODO_MODULE` with package name
2. Replace `TODO_Feature` with feature name (e.g., `Payment`, `Notification`)
3. Define meaningful `ErrorCode` values
4. Register in `GlobalExceptionHandler` (see controller.md)

**Alternative — single-purpose exception** (when only one error case exists):

```java
public class AnalysisNotFoundException extends RuntimeException {

  public AnalysisNotFoundException(final UUID id) {
    super("Analysis result not found: " + id);
  }
}
```

---

## ErrorCode Naming Convention

Use `SCREAMING_SNAKE_CASE`. Group semantically by HTTP status:

| HTTP Status | ErrorCode pattern | Examples |
|-------------|------------------|---------|
| 400 Bad Request | `*_REQUIRED`, `INVALID_*` | `FILE_REQUIRED`, `INVALID_INPUT` |
| 404 Not Found | `*_NOT_FOUND` | `ANALYSIS_NOT_FOUND` |
| 413 Payload Too Large | `*_TOO_LARGE` | `FILE_TOO_LARGE`, `PROMPT_TOO_LARGE` |
| 415 Unsupported | `UNSUPPORTED_*` | `UNSUPPORTED_MEDIA_TYPE`, `UNSUPPORTED_FORMAT` |
| 422 Unprocessable | `*_READ_FAILED` | `IMAGE_READ_FAILED` |
| 500 Internal Error | `*_FAILED` | `ANALYSIS_FAILED`, `PREPROCESSING_FAILED` |
| 503 Unavailable | `SERVICE_UNAVAILABLE` | `SERVICE_UNAVAILABLE` |
