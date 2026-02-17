package com.example.bill_manager.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validates that the Groq API key is properly configured on application startup.
 * <p>
 * This prevents the application from starting with placeholder or missing API keys,
 * enforcing the security requirement from CLAUDE.md:
 * "NEVER allow hardcoded API keys, passwords, tokens. No default values for sensitive properties"
 */
@Component
public class ApiKeyValidator {

  @Value("${spring.ai.openai.api-key}")
  private String apiKey;

  @PostConstruct
  public void validateApiKey() {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException(
          "GROQ_API_KEY environment variable must be set. "
              + "Get your API key from: https://console.groq.com/keys");
    }

    // Check for common placeholder values
    if (apiKey.equals("your_groq_api_key_here")
        || apiKey.startsWith("${")
        || apiKey.equals("REPLACE_WITH_YOUR_ACTUAL_KEY")) {
      throw new IllegalStateException(
          "GROQ_API_KEY is set to a placeholder value. "
              + "Replace it with your actual API key from: https://console.groq.com/keys");
    }

    // Basic format validation - Groq keys should have reasonable length
    if (apiKey.length() < 20) {
      throw new IllegalStateException(
          "GROQ_API_KEY appears to be invalid (too short). "
              + "Please verify your API key from: https://console.groq.com/keys");
    }
  }
}
