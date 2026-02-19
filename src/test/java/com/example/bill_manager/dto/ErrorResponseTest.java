package com.example.bill_manager.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ErrorResponseTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
  }

  @Test
  void shouldSerializeToJson() throws Exception {
    ErrorResponse error = new ErrorResponse(
        "UNSUPPORTED_MEDIA_TYPE",
        "File type not supported. Allowed: JPEG, PNG, PDF",
        Instant.parse("2026-02-06T14:30:00Z")
    );

    String json = objectMapper.writeValueAsString(error);

    assertThat(json).contains("\"code\":\"UNSUPPORTED_MEDIA_TYPE\"");
    assertThat(json).contains("\"message\":\"File type not supported. Allowed: JPEG, PNG, PDF\"");
    assertThat(json).contains("\"timestamp\"");
  }

  @Test
  void shouldDeserializeFromJson() throws Exception {
    String json = """
        {
          "code": "FILE_TOO_LARGE",
          "message": "Exceeded 10MB limit",
          "timestamp": "2026-02-06T14:30:00Z"
        }
        """;

    ErrorResponse error = objectMapper.readValue(json, ErrorResponse.class);

    assertThat(error.code()).isEqualTo("FILE_TOO_LARGE");
    assertThat(error.message()).isEqualTo("Exceeded 10MB limit");
    assertThat(error.timestamp()).isEqualTo(
        Instant.parse("2026-02-06T14:30:00Z"));
  }

  @Test
  void shouldRoundTripSerializeDeserialize() throws Exception {
    ErrorResponse original = new ErrorResponse(
        "ANALYSIS_NOT_FOUND",
        "Analysis with given ID not found",
        Instant.parse("2026-02-06T15:00:00Z")
    );

    String json = objectMapper.writeValueAsString(original);
    ErrorResponse deserialized = objectMapper.readValue(json, ErrorResponse.class);

    assertThat(deserialized.code()).isEqualTo(original.code());
    assertThat(deserialized.message()).isEqualTo(original.message());
    assertThat(deserialized.timestamp()).isEqualTo(original.timestamp());
  }
}
