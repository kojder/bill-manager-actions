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
      "groq.api.retry.max-attempts=5",
      "groq.api.retry.initial-delay-ms=2000",
      "groq.api.retry.multiplier=3.0"
    })
class GroqApiPropertiesTest {

  @Autowired private GroqApiProperties properties;

  @Test
  void shouldLoadRetryConfiguration() {
    assertThat(properties).isNotNull();
    assertThat(properties.retry()).isNotNull();
    assertThat(properties.retry().maxAttempts()).isEqualTo(5);
    assertThat(properties.retry().initialDelayMs()).isEqualTo(2000L);
    assertThat(properties.retry().multiplier()).isEqualTo(3.0);
  }

  @Test
  void shouldValidateRequiredFields() {
    assertThat(properties.retry().maxAttempts()).isPositive();
    assertThat(properties.retry().initialDelayMs()).isPositive();
    assertThat(properties.retry().multiplier()).isPositive();
  }
}
