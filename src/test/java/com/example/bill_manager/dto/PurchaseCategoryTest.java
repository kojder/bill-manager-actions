package com.example.bill_manager.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PurchaseCategoryTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Nested
  class Serialization {

    @ParameterizedTest
    @EnumSource(PurchaseCategory.class)
    void shouldSerializeToLowercaseDisplayName(final PurchaseCategory category)
        throws JsonProcessingException {
      final String json = MAPPER.writeValueAsString(category);

      assertThat(json).isEqualTo("\"" + category.getDisplayName() + "\"");
      assertThat(json).doesNotContain(category.name());
    }

    @Test
    void shouldSerializeGroceryAsLowercase() throws JsonProcessingException {
      final String json = MAPPER.writeValueAsString(PurchaseCategory.GROCERY);

      assertThat(json).isEqualTo("\"grocery\"");
    }

    @Test
    void shouldSerializeHomeAndGardenWithUnderscore() throws JsonProcessingException {
      final String json = MAPPER.writeValueAsString(PurchaseCategory.HOME_AND_GARDEN);

      assertThat(json).isEqualTo("\"home_and_garden\"");
    }
  }

  @Nested
  class Deserialization {

    @ParameterizedTest
    @EnumSource(PurchaseCategory.class)
    void shouldDeserializeFromDisplayName(final PurchaseCategory category)
        throws JsonProcessingException {
      final String json = "\"" + category.getDisplayName() + "\"";

      final PurchaseCategory result = MAPPER.readValue(json, PurchaseCategory.class);

      assertThat(result).isEqualTo(category);
    }

    @Test
    void shouldDeserializeCaseInsensitively() throws JsonProcessingException {
      assertThat(MAPPER.readValue("\"GROCERY\"", PurchaseCategory.class))
          .isEqualTo(PurchaseCategory.GROCERY);
      assertThat(MAPPER.readValue("\"Grocery\"", PurchaseCategory.class))
          .isEqualTo(PurchaseCategory.GROCERY);
      assertThat(MAPPER.readValue("\"grocery\"", PurchaseCategory.class))
          .isEqualTo(PurchaseCategory.GROCERY);
    }

    @Test
    void shouldFallbackToOtherForUnknownValue() throws JsonProcessingException {
      final PurchaseCategory result = MAPPER.readValue("\"food\"", PurchaseCategory.class);

      assertThat(result).isEqualTo(PurchaseCategory.OTHER);
    }

    @Test
    void shouldFallbackToOtherForNullValue() {
      final PurchaseCategory result = PurchaseCategory.fromString(null);

      assertThat(result).isEqualTo(PurchaseCategory.OTHER);
    }
  }

  @Nested
  class EnumIntegrity {

    @Test
    void shouldHaveExactlyTenCategories() {
      assertThat(PurchaseCategory.values()).hasSize(10);
    }

    @Test
    void shouldHaveUniqueDisplayNames() {
      final Set<String> displayNames =
          Arrays.stream(PurchaseCategory.values())
              .map(PurchaseCategory::getDisplayName)
              .collect(Collectors.toSet());

      assertThat(displayNames).hasSize(PurchaseCategory.values().length);
    }

    @Test
    void shouldContainAllExpectedCategories() {
      assertThat(PurchaseCategory.values())
          .extracting(PurchaseCategory::getDisplayName)
          .containsExactlyInAnyOrder(
              "grocery",
              "electronics",
              "restaurant",
              "pharmacy",
              "clothing",
              "home_and_garden",
              "transport",
              "entertainment",
              "services",
              "other");
    }
  }
}
