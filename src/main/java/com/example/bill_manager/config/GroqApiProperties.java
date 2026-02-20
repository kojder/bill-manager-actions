package com.example.bill_manager.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "groq.api")
@Validated
// spotless:off
public record GroqApiProperties(
    @NotBlank(message = "Base URL must not be blank")
    String baseUrl,
    @NotBlank(message = "Model name must not be blank")
    String model,
    @NotNull(message = "Timeout must not be null")
    @Min(value = 1, message = "Timeout must be at least 1 second")
    Integer timeoutSeconds,
    @NotNull(message = "Retry configuration must not be null")
    RetryConfig retry) {

  public record RetryConfig(
      @NotNull(message = "Max attempts must not be null")
      @Min(value = 1, message = "Max attempts must be at least 1")
      Integer maxAttempts,
      @NotNull(message = "Initial delay must not be null")
      @Min(value = 100, message = "Initial delay must be at least 100ms")
      Long initialDelayMs,
      @NotNull(message = "Multiplier must not be null")
      @Min(value = 1, message = "Multiplier must be at least 1.0")
      Double multiplier) {}
  // spotless:on
}
