package com.example.bill_manager.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class LineItemTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldSerializeToJson() throws Exception {
    LineItem item = new LineItem(
        "Milk 3.2%",
        new BigDecimal("2"),
        new BigDecimal("3.49"),
        new BigDecimal("6.98")
    );

    String json = objectMapper.writeValueAsString(item);

    assertThat(json).contains("\"name\":\"Milk 3.2%\"");
    assertThat(json).contains("\"quantity\":2");
    assertThat(json).contains("\"unitPrice\":3.49");
    assertThat(json).contains("\"totalPrice\":6.98");
  }

  @Test
  void shouldDeserializeFromJson() throws Exception {
    String json = """
        {
          "name": "Wheat Bread",
          "quantity": 1,
          "unitPrice": 4.99,
          "totalPrice": 4.99
        }
        """;

    LineItem item = objectMapper.readValue(json, LineItem.class);

    assertThat(item.name()).isEqualTo("Wheat Bread");
    assertThat(item.quantity()).isEqualByComparingTo(BigDecimal.ONE);
    assertThat(item.unitPrice()).isEqualByComparingTo(new BigDecimal("4.99"));
    assertThat(item.totalPrice()).isEqualByComparingTo(new BigDecimal("4.99"));
  }

  @Test
  void shouldHandleDecimalPrecision() throws Exception {
    String json = """
        {
          "name": "Item",
          "quantity": 0.5,
          "unitPrice": 10.00,
          "totalPrice": 5.00
        }
        """;

    LineItem item = objectMapper.readValue(json, LineItem.class);

    assertThat(item.quantity()).isEqualByComparingTo(new BigDecimal("0.5"));
    assertThat(item.totalPrice()).isEqualByComparingTo(new BigDecimal("5.00"));
  }
}
