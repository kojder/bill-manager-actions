# Spring AI / ChatClient Integration

## Table of Contents

- [ChatClient.Builder Injection](#chatclientbuilder-injection)
- [System Prompt Pattern](#system-prompt-pattern)
- [ChatClient Call Chain](#chatclient-call-chain)
- [Media Builder for Multimodal](#media-builder-for-multimodal)
- [BeanOutputConverter for Structured Output](#beanoutputconverter-for-structured-output)
- [Output Validation](#output-validation)
- [RetryTemplate Configuration](#retrytemplate-configuration)
- [Exception Handling for AI Calls](#exception-handling-for-ai-calls)

---

## ChatClient.Builder Injection

Inject `ChatClient.Builder` (not `ChatClient`) — Spring auto-configures it. Build in
the constructor with the system prompt:

```java
private final ChatClient chatClient;

public BillAnalysisServiceImpl(
    final ChatClient.Builder chatClientBuilder,
    final GroqApiProperties groqApiProperties,
    final Validator validator) {
  this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
  // ... other field initialization
}
```

---

## System Prompt Pattern

Define as `private static final String` text block with spotless guards:

```java
// spotless:off
private static final String SYSTEM_PROMPT = """
    You are a bill and receipt analysis assistant. Your task is to extract structured data \
    from bill/receipt images. Analyze the image carefully and extract:
    - The merchant/store name
    - All line items with name, quantity, unit price, and total price
    - The total amount
    - The currency (use ISO 4217 codes, e.g., PLN, EUR, USD)
    - Category tags describing the type of purchase

    Be precise with numbers. Use decimal point notation (e.g., 3.49, not 3,49).
    If a field cannot be determined, provide a reasonable default or best guess.""";
// spotless:on
```

Use backslash `\` continuation for lines that would exceed 120 chars within the text block.

---

## ChatClient Call Chain

The fluent API for calling the LLM:

```java
final String responseText = chatClient
    .prompt()
    .user(u -> u.text(promptText).media(media))
    .call()
    .content();
```

For text-only prompts (no media):

```java
final String responseText = chatClient
    .prompt()
    .user(u -> u.text(promptText))
    .call()
    .content();
```

---

## Media Builder for Multimodal

Attach images or PDFs to the prompt:

```java
final MimeType mediaMimeType = MimeType.valueOf(mimeType);
final Media media = Media.builder()
    .mimeType(mediaMimeType)
    .data(imageData)
    .build();
```

Then pass to user prompt: `.user(u -> u.text(promptText).media(media))`.

---

## BeanOutputConverter for Structured Output

Parse LLM text response into a Java Record:

```java
// Initialize once (field or constructor)
private final BeanOutputConverter<BillAnalysisResult> outputConverter;

public BillAnalysisServiceImpl(...) {
  this.outputConverter = new BeanOutputConverter<>(BillAnalysisResult.class);
}

// Get format instructions to include in the prompt
final String formatInstructions = outputConverter.getFormat();
final String promptText = "Analyze this image.\n\n" + formatInstructions;

// Parse the response
final BillAnalysisResult result = outputConverter.convert(responseText);
```

**Important:** Always null-check the result before using it:
```java
if (result == null) {
  throw new BillAnalysisException(
      BillAnalysisException.ErrorCode.INVALID_RESPONSE,
      "Failed to parse analysis response into structured result");
}
```

---

## Output Validation

After parsing, validate the result against Jakarta Validation annotations on the Record:

```java
private final Validator validator;  // jakarta.validation.Validator, injected via constructor

private BillAnalysisResult parseAndValidateResponse(final String responseText) {
  // ... null/blank checks, convert with outputConverter ...

  final Set<ConstraintViolation<BillAnalysisResult>> violations = validator.validate(result);
  if (!violations.isEmpty()) {
    final String msg =
        violations.stream()
            .map(v -> v.getPropertyPath() + " " + v.getMessage())
            .collect(Collectors.joining(", "));
    throw new BillAnalysisException(
        BillAnalysisException.ErrorCode.INVALID_RESPONSE,
        "Analysis result failed validation: " + msg);
  }
  return result;
}
```

---

## RetryTemplate Configuration

Build a `RetryTemplate` with exponential backoff from configurable properties:

```java
private static RetryTemplate buildRetryTemplate(final GroqApiProperties properties) {
  final ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
  backOff.setInitialInterval(properties.retry().initialDelayMs());
  backOff.setMultiplier(properties.retry().multiplier());

  final SimpleRetryPolicy retryPolicy =
      new SimpleRetryPolicy(
          properties.retry().maxAttempts(),
          Map.of(
              RestClientException.class, true,
              ResourceAccessException.class, true,
              TransientAiException.class, true),
          true);  // traverseCauses = true

  final RetryTemplate template = new RetryTemplate();
  template.setBackOffPolicy(backOff);
  template.setRetryPolicy(retryPolicy);
  return template;
}
```

**Retryable exceptions:** `RestClientException`, `ResourceAccessException`, `TransientAiException`.
**NOT retryable:** `NonTransientAiException` (permanent failures like content policy violations).

---

## Exception Handling for AI Calls

Wrap `retryTemplate.execute()` in try/catch. Never expose raw API errors to users:

```java
private String executeWithRetry(
    final String promptText, final MimeType mimeType, final byte[] imageData) {
  try {
    return retryTemplate.execute(
        (RetryCallback<String, Exception>)
            context -> {
              if (context.getRetryCount() > 0) {
                LOG.warn("Retrying API call, attempt {}", context.getRetryCount() + 1);
              }
              final Media media = Media.builder().mimeType(mimeType).data(imageData).build();
              return chatClient
                  .prompt()
                  .user(u -> u.text(promptText).media(media))
                  .call()
                  .content();
            });
  } catch (final RestClientException | NonTransientAiException e) {
    LOG.error("API call failed after retries exhausted", e);
    throw new BillAnalysisException(
        BillAnalysisException.ErrorCode.SERVICE_UNAVAILABLE,
        "Service is temporarily unavailable. Please try again later.",
        e);
  } catch (final Exception e) {
    LOG.error("Unexpected error during analysis", e);
    throw new BillAnalysisException(
        BillAnalysisException.ErrorCode.ANALYSIS_FAILED, "Failed to analyze", e);
  }
}
```

**Key rules:**
- Log retry count inside the callback
- User-facing messages: generic ("temporarily unavailable"), never raw API errors
- Two catch blocks: known API exceptions → SERVICE_UNAVAILABLE, everything else → ANALYSIS_FAILED
