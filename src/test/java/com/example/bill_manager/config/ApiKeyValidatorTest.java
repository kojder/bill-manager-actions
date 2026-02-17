package com.example.bill_manager.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

class ApiKeyValidatorTest {

  @Test
  void testValidApiKey() {
    ApiKeyValidator validator = new ApiKeyValidator();
    // Use reflection to set the apiKey field
    setApiKey(validator, "gsk_validKeyWith20PlusCharacters12345");

    assertDoesNotThrow(validator::validateApiKey);
  }

  @Test
  void testNullApiKey() {
    ApiKeyValidator validator = new ApiKeyValidator();
    setApiKey(validator, null);

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, validator::validateApiKey);
    assert exception.getMessage().contains("GROQ_API_KEY environment variable must be set");
  }

  @Test
  void testBlankApiKey() {
    ApiKeyValidator validator = new ApiKeyValidator();
    setApiKey(validator, "   ");

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, validator::validateApiKey);
    assert exception.getMessage().contains("GROQ_API_KEY environment variable must be set");
  }

  @Test
  void testPlaceholderApiKey() {
    ApiKeyValidator validator = new ApiKeyValidator();
    setApiKey(validator, "your_groq_api_key_here");

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, validator::validateApiKey);
    assert exception.getMessage().contains("placeholder value");
  }

  @Test
  void testEnvironmentVariablePlaceholder() {
    ApiKeyValidator validator = new ApiKeyValidator();
    setApiKey(validator, "${GROQ_API_KEY}");

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, validator::validateApiKey);
    assert exception.getMessage().contains("placeholder value");
  }

  @Test
  void testTooShortApiKey() {
    ApiKeyValidator validator = new ApiKeyValidator();
    setApiKey(validator, "short");

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, validator::validateApiKey);
    assert exception.getMessage().contains("too short");
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
