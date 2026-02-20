package com.example.bill_manager.ai;

import com.example.bill_manager.config.GroqApiProperties;
import com.example.bill_manager.dto.BillAnalysisResult;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

@Service
public class BillAnalysisServiceImpl implements BillAnalysisService {

  private static final Logger LOG = LoggerFactory.getLogger(BillAnalysisServiceImpl.class);

  private static final int MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;
  private static final String MIME_TYPE_PDF = "application/pdf";

  // spotless:off
  private static final String SYSTEM_PROMPT = """
      You are a bill and receipt analysis assistant. Your task is to extract structured data \
      from bill/receipt images. Analyze the image carefully and extract:
      - The merchant/store name
      - All line items with name, quantity, unit price, and total price
      - The total amount
      - The currency (use ISO 4217 codes, e.g., PLN, EUR, USD). Default to PLN if unclear.
      - Category tags describing the type of purchase (e.g., grocery, restaurant, electronics)

      Be precise with numbers. Use decimal point notation (e.g., 3.49, not 3,49).
      If a field cannot be determined, provide a reasonable default or best guess.""";
  // spotless:on

  private final ChatClient chatClient;
  private final RetryTemplate retryTemplate;
  private final BeanOutputConverter<BillAnalysisResult> outputConverter;

  public BillAnalysisServiceImpl(
      final ChatClient.Builder chatClientBuilder, final GroqApiProperties groqApiProperties) {
    this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
    this.retryTemplate = buildRetryTemplate(groqApiProperties);
    this.outputConverter = new BeanOutputConverter<>(BillAnalysisResult.class);
  }

  @Override
  public BillAnalysisResult analyze(final byte[] imageData, final String mimeType) {
    validateInput(imageData, mimeType);

    final String formatInstructions = outputConverter.getFormat();
    final MimeType mediaMimeType = MimeType.valueOf(mimeType);
    final String userPromptText =
        "Analyze this bill/receipt image and extract the structured data.\n\n" + formatInstructions;

    final String responseText = executeWithRetry(userPromptText, mediaMimeType, imageData);
    return parseAndValidateResponse(responseText);
  }

  private void validateInput(final byte[] imageData, final String mimeType) {
    if (imageData == null) {
      throw new BillAnalysisException(
          BillAnalysisException.ErrorCode.ANALYSIS_FAILED, "Image data must not be null");
    }
    if (mimeType == null) {
      throw new BillAnalysisException(
          BillAnalysisException.ErrorCode.ANALYSIS_FAILED, "MIME type must not be null");
    }
    if (MIME_TYPE_PDF.equals(mimeType)) {
      throw new BillAnalysisException(
          BillAnalysisException.ErrorCode.ANALYSIS_FAILED,
          "PDF analysis is not yet supported. Please upload an image (JPEG or PNG).");
    }
    if (imageData.length > MAX_IMAGE_SIZE_BYTES) {
      throw new BillAnalysisException(
          BillAnalysisException.ErrorCode.PROMPT_TOO_LARGE,
          "Image size exceeds maximum allowed for analysis: " + imageData.length + " bytes");
    }
  }

  private String executeWithRetry(
      final String promptText, final MimeType mimeType, final byte[] imageData) {
    try {
      return retryTemplate.execute(
          (RetryCallback<String, Exception>)
              context -> {
                if (context.getRetryCount() > 0) {
                  LOG.warn("Retrying Groq API call, attempt {}", context.getRetryCount() + 1);
                }
                final Media media = Media.builder().mimeType(mimeType).data(imageData).build();
                return chatClient
                    .prompt()
                    .user(u -> u.text(promptText).media(media))
                    .call()
                    .content();
              });
    } catch (final RestClientException e) {
      LOG.error("Groq API call failed after retries exhausted", e);
      throw new BillAnalysisException(
          BillAnalysisException.ErrorCode.SERVICE_UNAVAILABLE,
          "Bill analysis service is temporarily unavailable. Please try again later.",
          e);
    } catch (final Exception e) {
      LOG.error("Unexpected error during bill analysis", e);
      throw new BillAnalysisException(
          BillAnalysisException.ErrorCode.ANALYSIS_FAILED, "Failed to analyze bill image", e);
    }
  }

  private BillAnalysisResult parseAndValidateResponse(final String responseText) {
    if (responseText == null || responseText.isBlank()) {
      throw new BillAnalysisException(
          BillAnalysisException.ErrorCode.INVALID_RESPONSE,
          "Received empty response from analysis service");
    }

    try {
      final BillAnalysisResult result = outputConverter.convert(responseText);
      if (result == null) {
        throw new BillAnalysisException(
            BillAnalysisException.ErrorCode.INVALID_RESPONSE,
            "Failed to parse analysis response into structured result");
      }
      if (result.items() == null || result.items().isEmpty()) {
        throw new BillAnalysisException(
            BillAnalysisException.ErrorCode.INVALID_RESPONSE,
            "Analysis result contains no line items");
      }
      return result;
    } catch (final BillAnalysisException e) {
      throw e;
    } catch (final Exception e) {
      LOG.error("Failed to parse LLM response: {}", responseText, e);
      throw new BillAnalysisException(
          BillAnalysisException.ErrorCode.INVALID_RESPONSE, "Failed to parse analysis response", e);
    }
  }

  private static RetryTemplate buildRetryTemplate(final GroqApiProperties properties) {
    final ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
    backOff.setInitialInterval(properties.retry().initialDelayMs());
    backOff.setMultiplier(properties.retry().multiplier());

    final SimpleRetryPolicy retryPolicy =
        new SimpleRetryPolicy(
            properties.retry().maxAttempts(),
            Map.of(RestClientException.class, true, ResourceAccessException.class, true),
            true);

    final RetryTemplate template = new RetryTemplate();
    template.setBackOffPolicy(backOff);
    template.setRetryPolicy(retryPolicy);
    return template;
  }
}
