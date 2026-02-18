package com.example.bill_manager.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class BillAnalysisResultTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldSerializeToJson() throws Exception {
    BillAnalysisResult result = new BillAnalysisResult(
        "Grocery Store",
        List.of(
            new LineItem("Milk", new BigDecimal("2"), new BigDecimal("3.49"),
                new BigDecimal("6.98")),
            new LineItem("Bread", new BigDecimal("1"), new BigDecimal("4.99"),
                new BigDecimal("4.99"))
        ),
        new BigDecimal("11.97"),
        "PLN",
        List.of("grocery", "dairy", "bakery")
    );

    String json = objectMapper.writeValueAsString(result);

    assertThat(json).contains("\"merchantName\":\"Grocery Store\"");
    assertThat(json).contains("\"totalAmount\":11.97");
    assertThat(json).contains("\"currency\":\"PLN\"");
    assertThat(json).contains("\"categoryTags\":[\"grocery\",\"dairy\",\"bakery\"]");
    assertThat(json).contains("\"items\":[");
  }

  @Test
  void shouldDeserializeFromJson() throws Exception {
    String json = """
        {
          "merchantName": "Grocery Store",
          "items": [
            {
              "name": "Milk 3.2%",
              "quantity": 2,
              "unitPrice": 3.49,
              "totalPrice": 6.98
            }
          ],
          "totalAmount": 6.98,
          "currency": "PLN",
          "categoryTags": ["grocery", "dairy"]
        }
        """;

    BillAnalysisResult result = objectMapper.readValue(json, BillAnalysisResult.class);

    assertThat(result.merchantName()).isEqualTo("Grocery Store");
    assertThat(result.items()).hasSize(1);
    assertThat(result.items().get(0).name()).isEqualTo("Milk 3.2%");
    assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("6.98"));
    assertThat(result.currency()).isEqualTo("PLN");
    assertThat(result.categoryTags()).containsExactly("grocery", "dairy");
  }

  @Test
  void shouldDeserializeWithNullCategoryTags() throws Exception {
    String json = """
        {
          "merchantName": "Shop",
          "items": [
            {"name": "Item", "quantity": 1, "unitPrice": 10.0, "totalPrice": 10.0}
          ],
          "totalAmount": 10.0,
          "currency": "PLN",
          "categoryTags": null
        }
        """;

    BillAnalysisResult result = objectMapper.readValue(json, BillAnalysisResult.class);

    assertThat(result.categoryTags()).isNull();
  }

  @Test
  void shouldDeserializeWithEmptyCategoryTags() throws Exception {
    String json = """
        {
          "merchantName": "Shop",
          "items": [
            {"name": "Item", "quantity": 1, "unitPrice": 10.0, "totalPrice": 10.0}
          ],
          "totalAmount": 10.0,
          "currency": "EUR",
          "categoryTags": []
        }
        """;

    BillAnalysisResult result = objectMapper.readValue(json, BillAnalysisResult.class);

    assertThat(result.categoryTags()).isEmpty();
  }
}
