# Spring Ecosystem Review Checklist

## Table of Contents

- [Constructor Injection](#constructor-injection)
- [ConfigurationProperties as Records](#configurationproperties-as-records)
- [Layered Architecture](#layered-architecture)
- [RestControllerAdvice Error Handling](#restcontrolleradvice-error-handling)
- [Spring Data JPA](#spring-data-jpa)
- [Spring AI / ChatClient Integration](#spring-ai--chatclient-integration)
- [RetryTemplate Configuration](#retrytemplate-configuration)
- [Spring Integration and External APIs](#spring-integration-and-external-apis)
- [Testing Patterns](#testing-patterns)

---

## Constructor Injection

**Mandatory pattern — all dependencies via constructor with `final` fields:**

```java
@Service
public class BillAnalysisServiceImpl implements BillAnalysisService {

  private final ChatClient chatClient;
  private final RetryTemplate retryTemplate;
  private final BeanOutputConverter<BillAnalysisResult> outputConverter;
  private final Validator validator;

  public BillAnalysisServiceImpl(
      final ChatClient.Builder chatClientBuilder,
      final GroqApiProperties groqApiProperties,
      final Validator validator) {
    this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
    this.retryTemplate = buildRetryTemplate(groqApiProperties);
    this.outputConverter = new BeanOutputConverter<>(BillAnalysisResult.class);
    this.validator = validator;
  }
}
```

**Red flags (always report as Warning):**
- `@Autowired` on fields — must use constructor injection
- `@Value("${property}")` annotations — must use `@ConfigurationProperties`
- Non-`final` fields for injected dependencies
- Missing `final` on constructor parameters
- Multiple constructors with `@Autowired` — single constructor preferred

---

## @ConfigurationProperties as Records

Configuration classes must be Records with Jakarta Validation:

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
  // spotless:on

  public record RetryConfig(
      @NotNull @Min(value = 1) Integer maxAttempts,
      @NotNull @Min(value = 100) Long initialDelayMs,
      @NotNull @Min(value = 1) Double multiplier) {}
}
```

**Checklist:**
- [ ] `@ConfigurationProperties` with meaningful prefix
- [ ] `@Validated` annotation present (enables validation)
- [ ] Jakarta Validation annotations on all required components
- [ ] Validation messages descriptive and specific
- [ ] Nested Records for sub-configurations (not flat structure)
- [ ] `// spotless:off/on` guards when multiple annotations cause line overflow (>120 chars)
- [ ] No default values for sensitive properties (API keys, passwords)

---

## Layered Architecture

### Controllers

Controllers serve ONLY as REST routing layer. No business logic.

```java
@RestController
@RequestMapping("/api/bills")
public class BillUploadController {
  private final FileValidationService fileValidationService;
  private final BillResultStore billResultStore;

  // Constructor injection with final parameters...

  @PostMapping("/upload")
  public ResponseEntity<BillAnalysisResponse> upload(
      @RequestParam("file") final MultipartFile file) {
    fileValidationService.validateFile(file);
    // Delegate to services, never implement logic here
  }
}
```

**Checklist:**
- [ ] Controller methods delegate to services immediately
- [ ] No business logic in controllers (validation, transformation, persistence)
- [ ] Return `ResponseEntity` with explicit HTTP status codes
- [ ] `@RequestMapping` on class level for base path
- [ ] Method-level annotations (`@PostMapping`, `@GetMapping`) for actions

### Entity Leak Prevention

Database entities must NEVER be returned directly from controllers.

**Verify:**
- [ ] Controllers return Record DTOs, not JPA `@Entity` classes
- [ ] Service layer converts entities to DTOs before returning
- [ ] No `@Entity` class appears in controller method signatures

---

## @RestControllerAdvice Error Handling

Centralized exception handling via `GlobalExceptionHandler`:

**Checklist:**
- [ ] All custom exceptions have a dedicated `@ExceptionHandler` method
- [ ] Each handler returns `ResponseEntity<ErrorResponse>` (Record DTO)
- [ ] Error codes map to correct HTTP status codes via exhaustive `switch`
- [ ] Generic `Exception` handler exists as catch-all (returns 500)
- [ ] No stacktraces exposed in any error response
- [ ] `Instant.now()` for timestamps (not `LocalDateTime`)
- [ ] Handlers log error details server-side, return sanitized response to client

---

## Spring Data JPA

### N+1 Query Detection

The most common JPA performance issue. Look for these patterns:

**Red flags (report as Critical if in hot path, Warning otherwise):**
- Iterating over a collection of entities and accessing lazy-loaded relations:
  ```java
  // N+1 problem — triggers one query per order
  for (final Customer c : customerRepository.findAll()) {
    c.getOrders().size();  // Lazy load triggers separate query
  }
  ```

**Solutions to suggest:**
1. `@EntityGraph` for declarative fetch:
   ```java
   @EntityGraph(attributePaths = {"orders"})
   List<Customer> findAllWithOrders();
   ```
2. `JOIN FETCH` in JPQL:
   ```java
   @Query("SELECT c FROM Customer c JOIN FETCH c.orders")
   List<Customer> findAllWithOrders();
   ```
3. DTO Projection for read-only views:
   ```java
   @Query("SELECT new com.example.dto.CustomerSummary(c.name, SIZE(c.orders)) FROM Customer c")
   List<CustomerSummary> findCustomerSummaries();
   ```

### Fetch Strategy Checklist

- [ ] Default `FetchType.LAZY` for all `@OneToMany` and `@ManyToMany` relations
- [ ] `FetchType.EAGER` explicitly justified and documented if used
- [ ] `Set` preferred over `List` for `@OneToMany` (avoids Cartesian product with multiple bags)
- [ ] DTO projections used for read-heavy queries (not full entity loading)
- [ ] Pagination used for unbounded queries (`Pageable` parameter)

### Repository Patterns

- [ ] Repository extends `JpaRepository` or `CrudRepository` (not custom base)
- [ ] Derived query methods for simple queries (no `@Query` needed)
- [ ] `@Query` with JPQL for complex queries (never native SQL unless justified)
- [ ] No string concatenation in `@Query` values — use parameter binding (`:paramName`)

---

## Spring AI / ChatClient Integration

### ChatClient Setup

```java
// Inject Builder, not ChatClient directly
public BillAnalysisServiceImpl(
    final ChatClient.Builder chatClientBuilder, ...) {
  this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
}
```

### ChatClient Call Pattern

```java
final Media media = Media.builder()
    .mimeType(mediaMimeType)
    .data(imageData)
    .build();

final String response = chatClient
    .prompt()
    .user(u -> u.text(promptText).media(media))
    .call()
    .content();
```

### Structured Output with BeanOutputConverter

```java
final BeanOutputConverter<BillAnalysisResult> outputConverter =
    new BeanOutputConverter<>(BillAnalysisResult.class);
final String formatInstructions = outputConverter.getFormat();
// Append formatInstructions to user prompt text
final BillAnalysisResult result = outputConverter.convert(responseText);
```

**Checklist:**
- [ ] `ChatClient.Builder` injected (not `ChatClient` directly)
- [ ] System prompt set via `.defaultSystem()`
- [ ] `BeanOutputConverter` for structured output (not manual JSON parsing)
- [ ] Format instructions appended to user prompt
- [ ] Parsed output validated with `jakarta.validation.Validator`
- [ ] Null check on converter result before validation
- [ ] `Media.builder()` for multimodal content (images, PDFs)

---

## RetryTemplate Configuration

```java
final ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
backOff.setInitialInterval(properties.retry().initialDelayMs());  // 1000ms
backOff.setMultiplier(properties.retry().multiplier());            // 2.0

final SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
    properties.retry().maxAttempts(),                               // 3
    Map.of(
        RestClientException.class, true,
        ResourceAccessException.class, true,
        TransientAiException.class, true),
    true);  // traverseCauses = true
```

**Checklist:**
- [ ] Retry configured from `@ConfigurationProperties` (not hardcoded values)
- [ ] `ExponentialBackOffPolicy` used (not fixed or linear backoff)
- [ ] Retryable exceptions: `RestClientException`, `ResourceAccessException`, `TransientAiException`
- [ ] `NonTransientAiException` caught but NOT retried (permanent failures)
- [ ] `traverseCauses = true` to match wrapped exceptions
- [ ] Final exception wrapped in custom exception (not raw Spring/AI exceptions)
- [ ] Retry count available for logging/observability

---

## Spring Integration and External APIs

### Timeout Requirements

Every external API call must have explicit timeout configuration:

- [ ] Connection timeout configured (`spring.http.client.connect-timeout`)
- [ ] Read timeout configured (`spring.http.client.read-timeout=30s`)
- [ ] Timeout values from `@ConfigurationProperties` (not hardcoded)

### Circuit Breaker Pattern

For external service calls (LLM APIs, third-party services), verify:

- [ ] Circuit breaker implemented (Resilience4j `@CircuitBreaker` or Spring Retry)
- [ ] Fallback behavior defined for when circuit is open
- [ ] Half-open state allows gradual recovery
- [ ] Circuit breaker metrics exposed via Actuator (if Resilience4j)

### Adapter Pattern for External Services

Code communicating with external APIs must be isolated:

- [ ] Adapter class wraps external API client
- [ ] Business logic depends on an interface, not the adapter directly
- [ ] Adapter handles protocol-specific concerns (HTTP, serialization)
- [ ] Easy to swap implementations (e.g., mock for testing, different provider)

---

## Testing Patterns

### Controller Tests (@WebMvcTest)

```java
@WebMvcTest(BillUploadController.class)
class BillUploadControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private FileValidationService fileValidationService;
}
```

### Service Tests (Pure Unit — No Spring Context)

- Mock all dependencies with Mockito
- No `@SpringBootTest` — pure unit tests for speed
- Test each public method independently

### Checklist

- [ ] `@WebMvcTest` for controller tests (not `@SpringBootTest`)
- [ ] `@MockitoBean` for mocking Spring beans (not deprecated `@MockBean`)
- [ ] `@Nested` classes grouping tests by scenario (InputValidation, ErrorHandling, etc.)
- [ ] AssertJ assertions (`assertThat`, `assertThatThrownBy`) — not JUnit `assertEquals`
- [ ] Descriptive test method names: `shouldReturn201WhenValidFileUploaded`
- [ ] Test profile active for tests (`spring.profiles.active=test`) to bypass startup validators
