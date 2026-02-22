package com.example.bill_manager.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(UploadProperties.class)
@TestPropertySource(
    properties = {
      "upload.max-file-size-bytes=5242880",
      "upload.allowed-mime-types=image/jpeg,image/png",
      "upload.pdf-render-dpi=150",
      "upload.pdf-max-pages=3"
    })
class UploadPropertiesTest {

  @Autowired private UploadProperties properties;

  @Test
  void shouldLoadUploadProperties() {
    assertThat(properties).isNotNull();
    assertThat(properties.maxFileSizeBytes()).isEqualTo(5242880L);
    assertThat(properties.allowedMimeTypes()).containsExactly("image/jpeg", "image/png");
  }

  @Test
  void shouldValidateFileSize() {
    assertThat(properties.isFileSizeValid(1000L)).isTrue();
    assertThat(properties.isFileSizeValid(5242880L)).isTrue();
    assertThat(properties.isFileSizeValid(5242881L)).isFalse();
    assertThat(properties.isFileSizeValid(0L)).isFalse();
    assertThat(properties.isFileSizeValid(-1L)).isFalse();
  }

  @Test
  void shouldValidateMimeType() {
    assertThat(properties.isMimeTypeAllowed("image/jpeg")).isTrue();
    assertThat(properties.isMimeTypeAllowed("image/png")).isTrue();
    assertThat(properties.isMimeTypeAllowed("application/pdf")).isFalse();
    assertThat(properties.isMimeTypeAllowed("text/plain")).isFalse();
    assertThat(properties.isMimeTypeAllowed(null)).isFalse();
  }

  @Test
  void shouldLoadPdfProperties() {
    assertThat(properties.pdfRenderDpi()).isEqualTo(150);
    assertThat(properties.pdfMaxPages()).isEqualTo(3);
  }

  @Test
  void shouldValidateRequiredFields() {
    assertThat(properties.maxFileSizeBytes()).isPositive();
    assertThat(properties.allowedMimeTypes()).isNotEmpty();
    assertThat(properties.pdfRenderDpi()).isBetween(72, 600);
    assertThat(properties.pdfMaxPages()).isBetween(1, 5);
  }
}
