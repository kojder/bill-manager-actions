# Testing Patterns

## Table of Contents

- [Controller Test (@WebMvcTest)](#controller-test-webmvctest)
- [Service Test (Pure Unit)](#service-test-pure-unit)
- [ConfigurationProperties Test](#configurationproperties-test)
- [@Nested Organization Pattern](#nested-organization-pattern)
- [AssertJ Assertion Patterns](#assertj-assertion-patterns)
- [Exception Testing Pattern](#exception-testing-pattern)
- [ChatClient Mock Chain](#chatclient-mock-chain)
- [Test Data Constants](#test-data-constants)

---

## Controller Test (@WebMvcTest)

Slice test — loads only the web layer, mocks all services:

```java
@WebMvcTest(TODO_FeatureController.class)
class TODO_FeatureControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TODO_FeatureService featureService;

  // TODO: @MockitoBean for each service dependency

  @Nested
  class CreateEndpoint {

    @Test
    void shouldReturn201WhenValidRequest() throws Exception {
      // TODO: Setup mock responses
      doNothing().when(featureService).validate(any());

      mockMvc
          .perform(post("/api/TODO_RESOURCE")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"field\": \"value\"}"))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").isNotEmpty());
    }
  }

  @Nested
  class GetEndpoint {

    @Test
    void shouldReturn200WhenResourceExists() throws Exception {
      final UUID id = UUID.randomUUID();
      // TODO: Setup mock to return result

      mockMvc
          .perform(get("/api/TODO_RESOURCE/{id}", id))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void shouldReturn404WhenResourceNotFound() throws Exception {
      final UUID id = UUID.randomUUID();
      // TODO: Setup mock to throw NotFoundException

      mockMvc
          .perform(get("/api/TODO_RESOURCE/{id}", id))
          .andExpect(status().isNotFound());
    }
  }
}
```

For file upload tests, use `MockMultipartFile`:

```java
final MockMultipartFile file =
    new MockMultipartFile("file", "photo.jpg", "image/jpeg",
        new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

mockMvc
    .perform(multipart("/api/bills/upload").file(file))
    .andExpect(status().isCreated());
```

---

## Service Test (Pure Unit)

No Spring context — direct instantiation with mocked dependencies:

```java
class TODO_FeatureServiceImplTest {

  private TODO_Dependency dependency;
  private TODO_FeatureServiceImpl service;

  @BeforeEach
  void setUp() {
    dependency = mock(TODO_Dependency.class);
    service = new TODO_FeatureServiceImpl(dependency);
  }

  @Nested
  class InputValidation {

    @Test
    void shouldThrowExceptionForNullInput() {
      assertThatThrownBy(() -> service.process(null))
          .isInstanceOf(TODO_FeatureException.class)
          .extracting(e -> ((TODO_FeatureException) e).getErrorCode())
          .isEqualTo(TODO_FeatureException.ErrorCode.INVALID_INPUT);
    }
  }

  @Nested
  class SuccessfulProcessing {

    @Test
    void shouldReturnResultForValidInput() {
      when(dependency.compute(any())).thenReturn("value");

      final var result = service.process(validInput);

      assertThat(result).isNotNull();
      // TODO: Assert on result fields
    }
  }
}
```

---

## ConfigurationProperties Test

Validate that `@ConfigurationProperties` binds and validates correctly:

```java
@SpringJUnitConfig(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(TODO_Properties.class)
@TestPropertySource(
    properties = {
      "TODO_PREFIX.some-field=test-value",
      "TODO_PREFIX.some-number=10"
    })
class TODO_PropertiesTest {

  @Autowired
  private TODO_Properties properties;

  @Test
  void shouldLoadProperties() {
    assertThat(properties).isNotNull();
    assertThat(properties.someField()).isEqualTo("test-value");
    assertThat(properties.someNumber()).isEqualTo(10);
  }
}
```

---

## @Nested Organization Pattern

Group tests by behavior/scenario, not by method name:

```java
class ServiceImplTest {

  @Nested
  class InputValidation {
    // Tests for null/invalid input parameters
  }

  @Nested
  class SuccessfulProcessing {
    // Tests for happy path scenarios
  }

  @Nested
  class RetryBehavior {
    // Tests for retry on transient failures
  }

  @Nested
  class ErrorHandling {
    // Tests for error conditions, exception wrapping
  }
}
```

---

## AssertJ Assertion Patterns

**Basic assertions:**
```java
assertThat(result).isNotNull();
assertThat(result.name()).isEqualTo("expected");
assertThat(result.items()).hasSize(3);
assertThat(result.amount()).isEqualByComparingTo("3.49");  // BigDecimal comparison
```

**Collection assertions:**
```java
assertThat(result.tags()).containsExactly("grocery", "food");
assertThat(result.items()).isEmpty();
```

**No-exception assertion:**
```java
assertThatCode(() -> service.validate(validInput)).doesNotThrowAnyException();
```

---

## Exception Testing Pattern

**Simple — check type and ErrorCode:**
```java
assertThatThrownBy(() -> service.process(invalidInput))
    .isInstanceOf(TODO_FeatureException.class)
    .extracting(e -> ((TODO_FeatureException) e).getErrorCode())
    .isEqualTo(TODO_FeatureException.ErrorCode.INVALID_INPUT);
```

**Complex — multiple assertions on the exception:**
```java
assertThatThrownBy(() -> service.process(invalidInput))
    .isInstanceOf(TODO_FeatureException.class)
    .satisfies(
        e -> {
          final TODO_FeatureException ex = (TODO_FeatureException) e;
          assertThat(ex.getErrorCode())
              .isEqualTo(TODO_FeatureException.ErrorCode.SERVICE_UNAVAILABLE);
          assertThat(ex.getMessage()).contains("temporarily unavailable");
          assertThat(ex.getMessage()).doesNotContain("gsk_");  // No secrets leaked
        });
```

---

## ChatClient Mock Chain

The full mock setup for testing Spring AI `ChatClient` integration. This is complex
because `ChatClient` uses a fluent builder chain:

```java
private ChatClient.Builder chatClientBuilder;
private ChatClient.ChatClientRequestSpec requestSpec;
private ChatClient.CallResponseSpec callResponseSpec;

@BeforeEach
@SuppressWarnings("unchecked")
void setUp() {
  chatClientBuilder = mock(ChatClient.Builder.class);
  final ChatClient chatClient = mock(ChatClient.class);
  requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
  callResponseSpec = mock(ChatClient.CallResponseSpec.class);

  // Wire the fluent chain
  when(chatClientBuilder.defaultSystem(anyString())).thenReturn(chatClientBuilder);
  when(chatClientBuilder.build()).thenReturn(chatClient);
  when(chatClient.prompt()).thenReturn(requestSpec);
  when(requestSpec.user(any(Consumer.class))).thenReturn(requestSpec);
  when(requestSpec.call()).thenReturn(callResponseSpec);

  // Now control the response:
  when(callResponseSpec.content()).thenReturn("response text");
}
```

**Simulating failures for retry tests:**
```java
when(callResponseSpec.content())
    .thenThrow(new RestClientException("Connection refused"))
    .thenThrow(new RestClientException("Connection refused"))
    .thenReturn(VALID_JSON);
```

**Verifying call count (retry behavior):**
```java
verify(callResponseSpec, times(3)).content();
```

---

## Test Data Constants

Define test fixtures as `private static final` at the top of the test class.
Use `// spotless:off/on` for multi-line JSON blocks:

```java
private static final String MIME_JPEG = "image/jpeg";
private static final byte[] SAMPLE_IMAGE = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

// spotless:off
private static final String VALID_JSON = """
    {
      "merchantName": "Test Store",
      "items": [
        {"name": "Milk", "quantity": 1, "unitPrice": 3.49, "totalPrice": 3.49}
      ],
      "totalAmount": 3.49,
      "currency": "PLN",
      "categoryTags": ["grocery"]
    }""";
// spotless:on
```
