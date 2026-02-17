package com.example.bill_manager.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ApiKeyValidatorTest {

  @Test
  void testValidApiKey() {
    ApiKeyValidator validator = new ApiKeyValidator();
    // Valid Groq API key format: gsk_ + 52 characters = 56 total
    setApiKey(validator, "gsk_1234567890123456789012345678901234567890123456789012");

    assertDoesNotThrow(validator::validateApiKey);
  }

  @Test
  void testNullApiKey() {
    ApiKeyValidator validator = new ApiKeyValidator();
    setApiKey(validator, null);

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, validator::validateApiKey);
    assertThat(exception.getMessage()).contains("GROQ_API_KEY environment variable must be set");
  }

  @Test
  void testBlankApiKey() {
    ApiKeyValidator validator = new ApiKeyValidator();
    setApiKey(validator, "   ");

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, validator::validateApiKey);
    assertThat(exception.getMessage()).contains("GROQ_API_KEY environment variable must be set");
  }

  @Test
  void testPlaceholderApiKey() {
    ApiKeyValidator validator = new ApiKeyValidator();
    setApiKey(validator, "your_groq_api_key_here");

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, validator::validateApiKey);
    assertThat(exception.getMessage()).contains("placeholder value");
  }

  @Test
  void testEnvironmentVariablePlaceholder() {
    ApiKeyValidator validator = new ApiKeyValidator();
    setApiKey(validator, "${GROQ_API_KEY}");

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, validator::validateApiKey);
    assertThat(exception.getMessage()).contains("placeholder value");
  }

  @Test
  void testInvalidFormatApiKey() {
    ApiKeyValidator validator = new ApiKeyValidator();
    setApiKey(validator, "short");

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, validator::validateApiKey);
    assertThat(exception.getMessage()).contains("format is invalid");
  }

  private void setApiKey(ApiKeyValidator validator, String apiKey) {
    try {
      var field = ApiKeyValidator.class.getDeclaredField("apiKey");
      field.setAccessible(true);
      field.set(validator, apiKey);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set apiKey field", e);
    }
  }
}
