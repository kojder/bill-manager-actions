# Service Layer

## Table of Contents

- [Service Interface Template](#service-interface-template)
- [Service Implementation Template](#service-implementation-template)
- [Input Validation Pattern](#input-validation-pattern)
- [Private Helper Decomposition](#private-helper-decomposition)
- [Logging Guidelines](#logging-guidelines)
- [JPA Repository Integration](#jpa-repository-integration)

---

## Service Interface Template

Interface defines the contract. **No `final` on parameters** (Checkstyle `RedundantModifier`).

```java
package com.example.bill_manager.TODO_MODULE;

// TODO: Define return type and parameters matching the feature
public interface TODO_FeatureService {

  TODO_Result process(TODO_Input input);
}
```

---

## Service Implementation Template

Full `@Service` class with constructor injection, logging, and input validation:

```java
package com.example.bill_manager.TODO_MODULE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TODO_FeatureServiceImpl implements TODO_FeatureService {

  private static final Logger LOG = LoggerFactory.getLogger(TODO_FeatureServiceImpl.class);

  // TODO: Add private final fields for dependencies
  private final TODO_Dependency dependency;

  // TODO: Constructor with final params — all dependencies injected here
  public TODO_FeatureServiceImpl(final TODO_Dependency dependency) {
    this.dependency = dependency;
  }

  @Override
  public TODO_Result process(final TODO_Input input) {
    validateInput(input);
    return doProcess(input);
  }

  private void validateInput(final TODO_Input input) {
    if (input == null) {
      throw new TODO_FeatureException(
          TODO_FeatureException.ErrorCode.INVALID_INPUT, "Input must not be null");
    }
    // TODO: Add field-level validation
  }

  private TODO_Result doProcess(final TODO_Input input) {
    try {
      // TODO: Implement business logic
      LOG.info("Processing TODO_feature for: {}", input);
      return new TODO_Result(/* ... */);
    } catch (final TODO_FeatureException e) {
      throw e;
    } catch (final Exception e) {
      LOG.error("Failed to process TODO_feature", e);
      throw new TODO_FeatureException(
          TODO_FeatureException.ErrorCode.PROCESSING_FAILED, "Processing failed", e);
    }
  }
}
```

**Key rules:**
- `private static final Logger LOG` — always first field
- `private final` for all injected dependencies
- `final` on all constructor and method parameters
- Single `@Override` public method delegates to private helpers
- Re-throw custom exceptions, wrap all others

---

## Input Validation Pattern

Validate all public method parameters at entry point. Throw custom exception with specific
ErrorCode for each validation failure:

```java
private void validateInput(final byte[] imageData, final String mimeType) {
  if (imageData == null) {
    throw new BillAnalysisException(
        BillAnalysisException.ErrorCode.INVALID_INPUT, "Image data must not be null");
  }
  if (mimeType == null) {
    throw new BillAnalysisException(
        BillAnalysisException.ErrorCode.INVALID_INPUT, "MIME type must not be null");
  }
  if (imageData.length > MAX_IMAGE_SIZE_BYTES) {
    throw new BillAnalysisException(
        BillAnalysisException.ErrorCode.PROMPT_TOO_LARGE,
        "Image size exceeds maximum allowed: " + imageData.length + " bytes");
  }
}
```

**Pattern:** each check = one `if` + one `throw` with specific ErrorCode and descriptive message.

---

## Private Helper Decomposition

Decompose the public method into focused private helpers (SRP):

```java
@Override
public BillAnalysisResult analyze(final byte[] imageData, final String mimeType) {
  validateInput(imageData, mimeType);                    // Step 1: Validate
  final String response = executeWithRetry(/* ... */);   // Step 2: Call external service
  return parseAndValidateResponse(response);             // Step 3: Parse result
}
```

Each private method has a single responsibility. The public method reads as a workflow.

---

## Logging Guidelines

Use SLF4J parameterized logging. Never concatenate strings.

```java
// Correct — parameterized
LOG.info("Processing file: {}", filename);
LOG.warn("Retrying API call, attempt {}", retryCount + 1);
LOG.error("Failed to parse response (length={})", responseText.length(), e);

// Wrong — concatenation
LOG.info("Processing file: " + filename);
```

**What to log:** operation start/completion, retry attempts, error details (server-side only).
**What NOT to log:** API keys, full request/response bodies, user-uploaded file content.
For LLM responses, log length only: `LOG.error("... (length={})", text.length(), e)`.

---

## JPA Repository Integration

> **Note:** JPA is not yet in the codebase. This section will be expanded when the first
> JPA entity is added. The planned pattern follows Spring Data conventions:
> `JpaRepository<Entity, UUID>` with `@EntityGraph` for eager fetching and DTO projections
> for read-heavy queries.
