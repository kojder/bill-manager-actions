# Configuration Properties

## Table of Contents

- [ConfigurationProperties Record Template](#configurationproperties-record-template)
- [Nested Config Record](#nested-config-record)
- [Properties File Entries](#properties-file-entries)
- [Bean Registration](#bean-registration)
- [Startup Validator Pattern](#startup-validator-pattern)

---

## ConfigurationProperties Record Template

Use Java Records with `@Validated` and Jakarta Validation annotations:

```java
package com.example.bill_manager.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "TODO_PREFIX")
@Validated
// spotless:off
public record TODO_Properties(
    @NotBlank(message = "TODO field description must not be blank")
    String someField,
    @NotNull(message = "TODO field description must not be null")
    @Min(value = 1, message = "TODO field description must be at least 1")
    Integer someNumber) {
  // spotless:on
}
```

**Rules:**
- Prefix must match property key hierarchy (e.g., `groq.api` → `groq.api.base-url`)
- Every required field has `@NotNull` or `@NotBlank` with descriptive message
- No default values for sensitive properties (API keys, passwords)
- Use `// spotless:off/on` when annotations cause >120 char lines

---

## Nested Config Record

For sub-objects in configuration, use inner records:

```java
@ConfigurationProperties(prefix = "groq.api")
@Validated
// spotless:off
public record GroqApiProperties(
    @NotBlank(message = "Base URL must not be blank")
    String baseUrl,
    @NotBlank(message = "Model name must not be blank")
    String model,
    @NotNull(message = "Retry configuration must not be null")
    RetryConfig retry) {

  public record RetryConfig(
      @NotNull @Min(value = 1) Integer maxAttempts,
      @NotNull @Min(value = 100) Long initialDelayMs,
      @NotNull @Min(value = 1) Double multiplier) {}
  // spotless:on
}
```

---

## Properties File Entries

After creating a `@ConfigurationProperties` record, add entries to **three** files:

**`src/main/resources/application.properties`** — defaults and non-sensitive values:
```properties
# TODO_PREFIX configuration
TODO_PREFIX.some-field=default-value
TODO_PREFIX.some-number=10
```

**`src/main/resources/application-dev.properties`** — development overrides (optional):
```properties
# More verbose/lenient for development
TODO_PREFIX.some-number=100
```

**`src/test/resources/application.properties`** — test values (required to prevent test failures):
```properties
# Test values for TODO_Properties
TODO_PREFIX.some-field=test-value
TODO_PREFIX.some-number=5
```

---

## Bean Registration

Register the new `@ConfigurationProperties` class in the main application:

```java
// src/main/java/com/example/bill_manager/BillManagerApplication.java

@SpringBootApplication
@EnableConfigurationProperties({
    GroqApiProperties.class,
    UploadProperties.class,
    TODO_Properties.class   // <-- Add new properties class here
})
public class BillManagerApplication { ... }
```

---

## Startup Validator Pattern

For properties containing secrets that need format validation at startup:

```java
@Component
@Profile("!test")
public class TODO_KeyValidator {

  @Value("${TODO_PREFIX.api-key}")
  private String apiKey;

  @PostConstruct
  public void validateApiKey() {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException(
          "TODO_ENV_VAR environment variable must be set.");
    }

    final boolean isPlaceholder =
        apiKey.contains("your_") || apiKey.contains("${") || apiKey.contains("REPLACE_WITH");
    if (isPlaceholder) {
      throw new IllegalStateException(
          "TODO_ENV_VAR is set to a placeholder value.");
    }

    // TODO: Add format validation if the key has a known pattern
    // Example: Groq keys start with "gsk_" and are 56 chars long
  }
}
```

**Important:** `@Profile("!test")` disables the validator in test profile to avoid
requiring real API keys during CI/test runs.
