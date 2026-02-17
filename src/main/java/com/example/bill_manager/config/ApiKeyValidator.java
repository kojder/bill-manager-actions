package com.example.bill_manager.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Validates that the Groq API key is properly configured on application startup.
 * <p>
 * This prevents the application from starting with placeholder or missing API keys,
 * enforcing the security requirement from CLAUDE.md:
 * "NEVER allow hardcoded API keys, passwords, tokens. No default values for sensitive properties"
 * <p>
 * Note: This validator is disabled in test profile to allow tests to run with mock API keys.
 */
@Component
@Profile("!test")
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

    // Check for common placeholder patterns without exposing actual key value
    boolean isPlaceholder = apiKey.contains("your_groq_api_key")
        || apiKey.contains("${")
        || apiKey.contains("REPLACE_WITH");

    if (isPlaceholder) {
      throw new IllegalStateException(
          "GROQ_API_KEY is set to a placeholder value. "
              + "Replace it with your actual API key from: https://console.groq.com/keys");
    }

    // Groq API keys format: gsk_[52 characters] = 56 total length
    if (!apiKey.startsWith("gsk_") || apiKey.length() != 56) {
      throw new IllegalStateException(
          "GROQ_API_KEY format is invalid. Expected format: gsk_[52-characters]. "
              + "Please verify your API key from: https://console.groq.com/keys");
    }
  }
}
