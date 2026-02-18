package com.example.bill_manager.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BillAnalysisResponseTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
  }

  @Test
  void shouldSerializeToJson() throws Exception {
    BillAnalysisResponse response = createSampleResponse();

    String json = objectMapper.writeValueAsString(response);

    assertThat(json).contains("\"id\":\"550e8400-e29b-41d4-a716-446655440000\"");
    assertThat(json).contains("\"originalFileName\":\"receipt.jpg\"");
    assertThat(json).contains("\"merchantName\":\"Test Store\"");
    assertThat(json).contains("\"analyzedAt\"");
  }

  @Test
  void shouldDeserializeFromJson() throws Exception {
    String json = """
        {
          "id": "550e8400-e29b-41d4-a716-446655440000",
          "originalFileName": "receipt.jpg",
          "analysis": {
            "merchantName": "Test Store",
            "items": [
              {"name": "Item A", "quantity": 1, "unitPrice": 9.99, "totalPrice": 9.99}
            ],
            "totalAmount": 9.99,
            "currency": "PLN",
            "categoryTags": ["test"]
          },
          "analyzedAt": [2026, 2, 6, 14, 30, 0]
        }
        """;

    BillAnalysisResponse response = objectMapper.readValue(json,
        BillAnalysisResponse.class);

    assertThat(response.id()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
    assertThat(response.originalFileName()).isEqualTo("receipt.jpg");
    assertThat(response.analysis()).isNotNull();
    assertThat(response.analysis().merchantName()).isEqualTo("Test Store");
    assertThat(response.analysis().items()).hasSize(1);
    assertThat(response.analyzedAt()).isEqualTo(
        LocalDateTime.of(2026, 2, 6, 14, 30, 0));
  }

  @Test
  void shouldRoundTripSerializeDeserialize() throws Exception {
    BillAnalysisResponse original = createSampleResponse();

    String json = objectMapper.writeValueAsString(original);
    BillAnalysisResponse deserialized = objectMapper.readValue(json,
        BillAnalysisResponse.class);

    assertThat(deserialized.id()).isEqualTo(original.id());
    assertThat(deserialized.originalFileName()).isEqualTo(original.originalFileName());
    assertThat(deserialized.analysis().merchantName())
        .isEqualTo(original.analysis().merchantName());
    assertThat(deserialized.analysis().totalAmount())
        .isEqualByComparingTo(original.analysis().totalAmount());
    assertThat(deserialized.analyzedAt()).isEqualTo(original.analyzedAt());
  }

  private BillAnalysisResponse createSampleResponse() {
    return new BillAnalysisResponse(
        "550e8400-e29b-41d4-a716-446655440000",
        "receipt.jpg",
        new BillAnalysisResult(
            "Test Store",
            List.of(new LineItem("Item A", new BigDecimal("1"),
                new BigDecimal("9.99"), new BigDecimal("9.99"))),
            new BigDecimal("9.99"),
            "PLN",
            List.of("test")
        ),
        LocalDateTime.of(2026, 2, 6, 14, 30, 0)
    );
  }
}
