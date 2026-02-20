package com.example.bill_manager.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(GroqApiProperties.class)
@TestPropertySource(
    properties = {
      "groq.api.base-url=https://test.groq.com/api",
      "groq.api.model=test-model",
      "groq.api.timeout-seconds=45",
      "groq.api.retry.max-attempts=5",
      "groq.api.retry.initial-delay-ms=2000",
      "groq.api.retry.multiplier=3.0"
    })
class GroqApiPropertiesTest {

  @Autowired private GroqApiProperties properties;

  @Test
  void shouldLoadGroqApiProperties() {
    assertThat(properties).isNotNull();
    assertThat(properties.baseUrl()).isEqualTo("https://test.groq.com/api");
    assertThat(properties.model()).isEqualTo("test-model");
    assertThat(properties.timeoutSeconds()).isEqualTo(45);
  }

  @Test
  void shouldLoadRetryConfiguration() {
    assertThat(properties.retry()).isNotNull();
    assertThat(properties.retry().maxAttempts()).isEqualTo(5);
    assertThat(properties.retry().initialDelayMs()).isEqualTo(2000L);
    assertThat(properties.retry().multiplier()).isEqualTo(3.0);
  }

  @Test
  void shouldValidateRequiredFields() {
    assertThat(properties.baseUrl()).isNotBlank();
    assertThat(properties.model()).isNotBlank();
    assertThat(properties.timeoutSeconds()).isPositive();
    assertThat(properties.retry().maxAttempts()).isPositive();
    assertThat(properties.retry().initialDelayMs()).isPositive();
    assertThat(properties.retry().multiplier()).isPositive();
  }
}
