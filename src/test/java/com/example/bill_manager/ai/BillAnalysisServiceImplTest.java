package com.example.bill_manager.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bill_manager.config.GroqApiProperties;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.ChatClient.PromptUserSpec;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

class BillAnalysisServiceImplTest {

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

  private static final String VALID_JSON_NULL_TAGS = """
      {
        "merchantName": "Test Store",
        "items": [
          {"name": "Bread", "quantity": 2, "unitPrice": 4.50, "totalPrice": 9.00}
        ],
        "totalAmount": 9.00,
        "currency": "PLN",
        "categoryTags": null
      }""";

  private static final String JSON_NO_ITEMS = """
      {
        "merchantName": "Test Store",
        "items": [],
        "totalAmount": 0,
        "currency": "PLN",
        "categoryTags": null
      }""";
  // spotless:on

  private ChatClient.Builder chatClientBuilder;
  private ChatClientRequestSpec requestSpec;
  private CallResponseSpec callResponseSpec;
  private BillAnalysisServiceImpl service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    chatClientBuilder = mock(ChatClient.Builder.class);
    final ChatClient chatClient = mock(ChatClient.class);
    requestSpec = mock(ChatClientRequestSpec.class);
    callResponseSpec = mock(CallResponseSpec.class);

    when(chatClientBuilder.defaultSystem(anyString())).thenReturn(chatClientBuilder);
    when(chatClientBuilder.build()).thenReturn(chatClient);
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(any(Consumer.class))).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponseSpec);

    final GroqApiProperties properties =
        new GroqApiProperties(
            "https://api.groq.com/openai/v1",
            "llama-3.2-11b-vision-preview",
            30,
            new GroqApiProperties.RetryConfig(3, 1000L, 2.0));

    service = new BillAnalysisServiceImpl(chatClientBuilder, properties);
  }

  @Nested
  class InputValidation {

    @Test
    void shouldThrowExceptionForNullImageData() {
      assertThatThrownBy(() -> service.analyze(null, MIME_JPEG))
          .isInstanceOf(BillAnalysisException.class)
          .extracting(e -> ((BillAnalysisException) e).getErrorCode())
          .isEqualTo(BillAnalysisException.ErrorCode.ANALYSIS_FAILED);
    }

    @Test
    void shouldThrowExceptionForNullMimeType() {
      assertThatThrownBy(() -> service.analyze(SAMPLE_IMAGE, null))
          .isInstanceOf(BillAnalysisException.class)
          .extracting(e -> ((BillAnalysisException) e).getErrorCode())
          .isEqualTo(BillAnalysisException.ErrorCode.ANALYSIS_FAILED);
    }

    @Test
    void shouldThrowExceptionForPdfMimeType() {
      assertThatThrownBy(() -> service.analyze(SAMPLE_IMAGE, "application/pdf"))
          .isInstanceOf(BillAnalysisException.class)
          .satisfies(
              e -> {
                final BillAnalysisException ex = (BillAnalysisException) e;
                assertThat(ex.getErrorCode())
                    .isEqualTo(BillAnalysisException.ErrorCode.ANALYSIS_FAILED);
                assertThat(ex.getMessage()).contains("PDF");
              });
    }

    @Test
    void shouldThrowExceptionForOversizedImage() {
      final byte[] oversized = new byte[6 * 1024 * 1024];

      assertThatThrownBy(() -> service.analyze(oversized, MIME_JPEG))
          .isInstanceOf(BillAnalysisException.class)
          .extracting(e -> ((BillAnalysisException) e).getErrorCode())
          .isEqualTo(BillAnalysisException.ErrorCode.PROMPT_TOO_LARGE);
    }
  }

  @Nested
  class SuccessfulAnalysis {

    @Test
    void shouldReturnBillAnalysisResultForValidImage() {
      when(callResponseSpec.content()).thenReturn(VALID_JSON);

      final var result = service.analyze(SAMPLE_IMAGE, MIME_JPEG);

      assertThat(result).isNotNull();
      assertThat(result.merchantName()).isEqualTo("Test Store");
      assertThat(result.items()).hasSize(1);
      assertThat(result.items().get(0).name()).isEqualTo("Milk");
      assertThat(result.totalAmount()).isEqualByComparingTo("3.49");
      assertThat(result.currency()).isEqualTo("PLN");
      assertThat(result.categoryTags()).containsExactly("grocery");
    }

    @Test
    void shouldHandleNullCategoryTags() {
      when(callResponseSpec.content()).thenReturn(VALID_JSON_NULL_TAGS);

      final var result = service.analyze(SAMPLE_IMAGE, MIME_JPEG);

      assertThat(result).isNotNull();
      assertThat(result.categoryTags()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCallChatClientWithUserPrompt() {
      when(callResponseSpec.content()).thenReturn(VALID_JSON);

      service.analyze(SAMPLE_IMAGE, MIME_JPEG);

      final ArgumentCaptor<Consumer<PromptUserSpec>> captor =
          ArgumentCaptor.forClass(Consumer.class);
      verify(requestSpec).user(captor.capture());
      assertThat(captor.getValue()).isNotNull();
    }

    @Test
    void shouldAcceptPngMimeType() {
      when(callResponseSpec.content()).thenReturn(VALID_JSON);

      final var result = service.analyze(SAMPLE_IMAGE, "image/png");

      assertThat(result).isNotNull();
      assertThat(result.merchantName()).isEqualTo("Test Store");
    }
  }

  @Nested
  class RetryBehavior {

    @Test
    void shouldRetryOnRestClientException() {
      when(callResponseSpec.content())
          .thenThrow(new RestClientException("Connection refused"))
          .thenThrow(new RestClientException("Connection refused"))
          .thenReturn(VALID_JSON);

      final var result = service.analyze(SAMPLE_IMAGE, MIME_JPEG);

      assertThat(result).isNotNull();
      assertThat(result.merchantName()).isEqualTo("Test Store");
      verify(callResponseSpec, times(3)).content();
    }

    @Test
    void shouldRetryOnResourceAccessException() {
      when(callResponseSpec.content())
          .thenThrow(new ResourceAccessException("Timeout"))
          .thenReturn(VALID_JSON);

      final var result = service.analyze(SAMPLE_IMAGE, MIME_JPEG);

      assertThat(result).isNotNull();
      verify(callResponseSpec, times(2)).content();
    }

    @Test
    void shouldThrowServiceUnavailableAfterRetriesExhausted() {
      when(callResponseSpec.content()).thenThrow(new RestClientException("Connection refused"));

      assertThatThrownBy(() -> service.analyze(SAMPLE_IMAGE, MIME_JPEG))
          .isInstanceOf(BillAnalysisException.class)
          .satisfies(
              e -> {
                final BillAnalysisException ex = (BillAnalysisException) e;
                assertThat(ex.getErrorCode())
                    .isEqualTo(BillAnalysisException.ErrorCode.SERVICE_UNAVAILABLE);
                assertThat(ex.getMessage()).contains("temporarily unavailable");
              });
    }
  }

  @Nested
  class ErrorHandling {

    @Test
    void shouldThrowInvalidResponseForEmptyLlmResponse() {
      when(callResponseSpec.content()).thenReturn("");

      assertThatThrownBy(() -> service.analyze(SAMPLE_IMAGE, MIME_JPEG))
          .isInstanceOf(BillAnalysisException.class)
          .extracting(e -> ((BillAnalysisException) e).getErrorCode())
          .isEqualTo(BillAnalysisException.ErrorCode.INVALID_RESPONSE);
    }

    @Test
    void shouldThrowInvalidResponseForNullLlmResponse() {
      when(callResponseSpec.content()).thenReturn(null);

      assertThatThrownBy(() -> service.analyze(SAMPLE_IMAGE, MIME_JPEG))
          .isInstanceOf(BillAnalysisException.class)
          .extracting(e -> ((BillAnalysisException) e).getErrorCode())
          .isEqualTo(BillAnalysisException.ErrorCode.INVALID_RESPONSE);
    }

    @Test
    void shouldThrowInvalidResponseForUnparsableJson() {
      when(callResponseSpec.content()).thenReturn("not a valid json {{{");

      assertThatThrownBy(() -> service.analyze(SAMPLE_IMAGE, MIME_JPEG))
          .isInstanceOf(BillAnalysisException.class)
          .extracting(e -> ((BillAnalysisException) e).getErrorCode())
          .isEqualTo(BillAnalysisException.ErrorCode.INVALID_RESPONSE);
    }

    @Test
    void shouldThrowInvalidResponseForResultWithNoItems() {
      when(callResponseSpec.content()).thenReturn(JSON_NO_ITEMS);

      assertThatThrownBy(() -> service.analyze(SAMPLE_IMAGE, MIME_JPEG))
          .isInstanceOf(BillAnalysisException.class)
          .satisfies(
              e -> {
                final BillAnalysisException ex = (BillAnalysisException) e;
                assertThat(ex.getErrorCode())
                    .isEqualTo(BillAnalysisException.ErrorCode.INVALID_RESPONSE);
                assertThat(ex.getMessage()).contains("no line items");
              });
    }

    @Test
    void shouldThrowAnalysisFailedForUnexpectedException() {
      when(callResponseSpec.content()).thenThrow(new RuntimeException("Unexpected"));

      assertThatThrownBy(() -> service.analyze(SAMPLE_IMAGE, MIME_JPEG))
          .isInstanceOf(BillAnalysisException.class)
          .extracting(e -> ((BillAnalysisException) e).getErrorCode())
          .isEqualTo(BillAnalysisException.ErrorCode.ANALYSIS_FAILED);
    }

    @Test
    void shouldNeverExposeRawApiErrorMessages() {
      when(callResponseSpec.content())
          .thenThrow(new RestClientException("API key invalid: gsk_abc123..."));

      assertThatThrownBy(() -> service.analyze(SAMPLE_IMAGE, MIME_JPEG))
          .isInstanceOf(BillAnalysisException.class)
          .satisfies(
              e -> {
                final BillAnalysisException ex = (BillAnalysisException) e;
                assertThat(ex.getMessage()).doesNotContain("gsk_");
                assertThat(ex.getMessage()).doesNotContain("API key invalid");
              });
    }
  }
}
