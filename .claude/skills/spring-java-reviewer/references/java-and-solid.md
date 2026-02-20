# Java 17 and SOLID Review Checklist

## Table of Contents

- [SOLID Principles](#solid-principles)
- [Java Records](#java-records)
- [Use of final Keyword](#use-of-final-keyword)
- [Exception Pattern](#exception-pattern)
- [Switch Expressions](#switch-expressions)
- [Sealed Classes](#sealed-classes)
- [Pattern Matching](#pattern-matching)
- [Optional Usage](#optional-usage)

---

## SOLID Principles

### Single Responsibility Principle (SRP)

Each class must have exactly one reason to change.

**Project reference patterns (gold standard):**
- `FileValidationService` — only validates files (MIME type, size, filename)
- `ImagePreprocessingService` — only resizes and re-encodes images
- `BillAnalysisService` — only communicates with LLM and parses results
- `BillResultStore` — only manages in-memory result storage
- `GlobalExceptionHandler` — only maps exceptions to HTTP responses

**Red flags to report:**
- Controller performing business logic beyond delegation to services
- Service combining validation + processing + persistence in one class
- Exception handler containing business rules or data transformations
- Class with more than 10-15 public methods ("God Object")

### Open/Closed Principle (OCP)

Code must be open for extension, closed for modification.

**Project pattern — Interface + Impl:**
```java
// Interface (open for extension)
public interface BillAnalysisService {
  BillAnalysisResult analyze(byte[] imageData, String mimeType);
}

// Implementation (closed for modification, swappable)
@Service
public class BillAnalysisServiceImpl implements BillAnalysisService { ... }
```

**Verify:** Every `@Service` class implements an interface.

**Red flags:**
- Long if/else or switch chains on type — suggest Strategy pattern with polymorphism
- Direct instantiation of collaborators inside service methods
- Concrete class types in constructor parameters (should be interfaces)

### Liskov Substitution Principle (LSP)

Any implementation must be substitutable for its interface without breaking callers.

**Check:** Interface method contracts are honored — same preconditions, same postconditions.
No implementation should throw unexpected exceptions or silently ignore inputs.

### Interface Segregation Principle (ISP)

Prefer many small, client-specific interfaces over one general-purpose interface.

**Project pattern:** Separate interfaces for each concern:
- `FileValidationService` (validation only)
- `ImagePreprocessingService` (image processing only)
- `BillAnalysisService` (LLM analysis only)

**Red flag:** A single `FileService` interface combining validate + preprocess + analyze.

### Dependency Inversion Principle (DIP)

High-level modules depend on abstractions, not concrete implementations.

**Verify in controllers and services:**
```java
// Correct — depends on interface
private final FileValidationService fileValidationService;

// Wrong — depends on concrete class
private final FileValidationServiceImpl fileValidationService;
```

---

## Java Records

All DTOs must be Java Records. Never use classes with getters/setters for data transfer.

**Checklist:**
- [ ] `record` keyword used (not `class` with Lombok `@Data` or manual getters)
- [ ] Jakarta Validation annotations on record components (`@NotBlank`, `@NotNull`, etc.)
- [ ] `@Valid` on nested record collections (e.g., `@Valid List<LineItem> items`)
- [ ] `@PositiveOrZero` for prices (allows free items), `@Positive` for quantities
- [ ] `// spotless:off` / `// spotless:on` guards when record has multiple annotations
      per component that cause lines to exceed 120 characters

**Project examples:**
```java
public record BillAnalysisResult(
    @NotBlank String merchantName,
    @NotEmpty @Valid List<LineItem> items,
    @NotNull @PositiveOrZero BigDecimal totalAmount,
    @NotBlank String currency,
    List<String> categoryTags) {}

public record LineItem(
    @NotBlank String name,
    @NotNull @Positive BigDecimal quantity,
    @NotNull @PositiveOrZero BigDecimal unitPrice,
    @NotNull @PositiveOrZero BigDecimal totalPrice) {}

public record ErrorResponse(String code, String message, Instant timestamp) {}
```

---

## Use of `final` Keyword

The project enforces `final` via Checkstyle `FinalParameters` module.

**Rules:**
- `final` on all method parameters: `public void process(final MultipartFile file)`
- `final` on all constructor parameters: `ServiceImpl(final Config config)`
- `final` on catch block variables: `catch (final IOException e)`
- `final` on local variables assigned once: `final String result = compute();`
- `final` on private fields injected via constructor: `private final Config config;`
- **NO `final` on interface method parameters** — Checkstyle `RedundantModifier` flags this

**Example — correct interface vs implementation:**
```java
// Interface — no final on parameters
public interface FileValidationService {
  void validateFile(MultipartFile file);
}

// Implementation — final on all parameters
@Service
public class FileValidationServiceImpl implements FileValidationService {
  @Override
  public void validateFile(final MultipartFile file) { ... }
}
```

---

## Exception Pattern

All custom exceptions must follow this structure:

```java
@Getter
public class XxxException extends RuntimeException {

  public enum ErrorCode {
    CODE_A,
    CODE_B
  }

  private final ErrorCode errorCode;

  public XxxException(final ErrorCode errorCode, final String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public XxxException(
      final ErrorCode errorCode, final String message, final Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }
}
```

**Checklist:**
- [ ] Extends `RuntimeException` (not checked exceptions)
- [ ] Nested `ErrorCode` enum (not external enum class)
- [ ] `@Getter` from Lombok (not manual getter methods)
- [ ] Both constructors: with and without `Throwable cause`
- [ ] `final` on all parameters and `errorCode` field
- [ ] Descriptive enum values matching HTTP semantics (e.g., `FILE_TOO_LARGE`, `UNSUPPORTED_MEDIA_TYPE`)

---

## Switch Expressions

Use exhaustive switch expressions with arrow syntax (Java 17):

```java
return switch (errorCode) {
  case FILE_REQUIRED -> HttpStatus.BAD_REQUEST;
  case FILE_TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE;
  case UNSUPPORTED_MEDIA_TYPE -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
  case FILE_UNREADABLE -> HttpStatus.INTERNAL_SERVER_ERROR;
};
```

**Checklist:**
- [ ] Arrow syntax (`->`) not colon (`:`) with break
- [ ] All enum values covered (exhaustive — compiler catches missing cases)
- [ ] No `default` case for enum switches (defeats exhaustiveness checking)
- [ ] Expression form (returns value) preferred over statement form

---

## Sealed Classes

Suggest `sealed` classes/interfaces when a type hierarchy has a known, fixed set of subtypes:

```java
public sealed interface PaymentMethod permits CreditCard, BankTransfer, Cash {
  BigDecimal amount();
}
```

**When to suggest:**
- Business domain models with fixed variants (payment types, document types, status enums)
- Where exhaustive pattern matching in `switch` would be valuable
- Where preventing arbitrary subclassing improves safety

**When NOT to suggest:**
- Service interfaces meant for extension (Spring DI)
- DTOs (use Records instead)

---

## Pattern Matching

### instanceof Pattern Matching

```java
// Preferred
if (obj instanceof String s) {
  return s.length();
}

// Avoid
if (obj instanceof String) {
  return ((String) obj).length();
}
```

### Switch Pattern Matching (Java 17+)

```java
return switch (shape) {
  case Circle c -> Math.PI * c.radius() * c.radius();
  case Rectangle r -> r.width() * r.height();
};
```

**Red flag:** Explicit casts after `instanceof` checks — always suggest pattern variable.

---

## Optional Usage

**Rules:**
- Use `Optional` as method return type when absence is a valid outcome
- NEVER use `Optional` as a method parameter or field
- NEVER call `.get()` without `.isPresent()` check — prefer `.orElseThrow()`, `.orElse()`, `.map()`
- Prefer `Optional.ofNullable()` over null checks when chaining transformations

```java
// Good — return type
public Optional<BillAnalysisResult> findById(final UUID id) { ... }

// Bad — parameter
public void process(final Optional<String> name) { ... }
```
