package com.example.bill_manager.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ValidationTest {

  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Nested
  class LineItemValidation {

    @Test
    void shouldPassForValidLineItem() {
      LineItem item = new LineItem("Milk", new BigDecimal("2"),
          new BigDecimal("3.49"), new BigDecimal("6.98"));

      Set<ConstraintViolation<LineItem>> violations = validator.validate(item);

      assertThat(violations).isEmpty();
    }

    @Test
    void shouldAllowZeroPrices() {
      LineItem item = new LineItem("Free Item", new BigDecimal("1"),
          BigDecimal.ZERO, BigDecimal.ZERO);

      Set<ConstraintViolation<LineItem>> violations = validator.validate(item);

      assertThat(violations).isEmpty();
    }

    @Test
    void shouldRejectBlankName() {
      LineItem item = new LineItem("", new BigDecimal("1"),
          new BigDecimal("1.00"), new BigDecimal("1.00"));

      Set<ConstraintViolation<LineItem>> violations = validator.validate(item);

      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    void shouldRejectNegativeUnitPrice() {
      LineItem item = new LineItem("Item", new BigDecimal("1"),
          new BigDecimal("-1.00"), new BigDecimal("1.00"));

      Set<ConstraintViolation<LineItem>> violations = validator.validate(item);

      assertThat(violations).isNotEmpty();
      assertThat(violations)
          .anyMatch(v -> v.getPropertyPath().toString().equals("unitPrice"));
    }

    @Test
    void shouldRejectZeroQuantity() {
      LineItem item = new LineItem("Item", BigDecimal.ZERO,
          new BigDecimal("1.00"), new BigDecimal("0.00"));

      Set<ConstraintViolation<LineItem>> violations = validator.validate(item);

      assertThat(violations).isNotEmpty();
      assertThat(violations)
          .anyMatch(v -> v.getPropertyPath().toString().equals("quantity"));
    }

    @Test
    void shouldRejectNullFields() {
      LineItem item = new LineItem(null, null, null, null);

      Set<ConstraintViolation<LineItem>> violations = validator.validate(item);

      assertThat(violations).hasSize(4);
    }
  }

  @Nested
  class BillAnalysisResultValidation {

    @Test
    void shouldPassForValidResult() {
      BillAnalysisResult result = createValidResult();

      Set<ConstraintViolation<BillAnalysisResult>> violations =
          validator.validate(result);

      assertThat(violations).isEmpty();
    }

    @Test
    void shouldRejectNegativeTotalAmount() {
      BillAnalysisResult result = new BillAnalysisResult(
          "Store", List.of(createValidLineItem()),
          new BigDecimal("-10.00"), "PLN", List.of());

      Set<ConstraintViolation<BillAnalysisResult>> violations =
          validator.validate(result);

      assertThat(violations).isNotEmpty();
      assertThat(violations)
          .anyMatch(v -> v.getPropertyPath().toString().equals("totalAmount"));
    }

    @Test
    void shouldRejectBlankMerchantName() {
      BillAnalysisResult result = new BillAnalysisResult(
          "", List.of(createValidLineItem()),
          new BigDecimal("10.00"), "PLN", List.of());

      Set<ConstraintViolation<BillAnalysisResult>> violations =
          validator.validate(result);

      assertThat(violations).isNotEmpty();
      assertThat(violations)
          .anyMatch(v -> v.getPropertyPath().toString().equals("merchantName"));
    }

    @Test
    void shouldRejectEmptyItems() {
      BillAnalysisResult result = new BillAnalysisResult(
          "Store", List.of(),
          new BigDecimal("10.00"), "PLN", List.of());

      Set<ConstraintViolation<BillAnalysisResult>> violations =
          validator.validate(result);

      assertThat(violations).isNotEmpty();
      assertThat(violations)
          .anyMatch(v -> v.getPropertyPath().toString().equals("items"));
    }

    @Test
    void shouldCascadeValidationToLineItems() {
      LineItem invalidItem = new LineItem("", null, null, null);
      BillAnalysisResult result = new BillAnalysisResult(
          "Store", List.of(invalidItem),
          new BigDecimal("10.00"), "PLN", List.of());

      Set<ConstraintViolation<BillAnalysisResult>> violations =
          validator.validate(result);

      assertThat(violations).hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    void shouldAllowNullCategoryTags() {
      BillAnalysisResult result = new BillAnalysisResult(
          "Store", List.of(createValidLineItem()),
          new BigDecimal("10.00"), "PLN", null);

      Set<ConstraintViolation<BillAnalysisResult>> violations =
          validator.validate(result);

      assertThat(violations).isEmpty();
    }
  }

  @Nested
  class BillAnalysisResponseValidation {

    @Test
    void shouldPassForValidResponse() {
      BillAnalysisResponse response = new BillAnalysisResponse(
          UUID.randomUUID(), "receipt.jpg",
          createValidResult(), LocalDateTime.now());

      Set<ConstraintViolation<BillAnalysisResponse>> violations =
          validator.validate(response);

      assertThat(violations).isEmpty();
    }

    @Test
    void shouldRejectNullId() {
      BillAnalysisResponse response = new BillAnalysisResponse(
          null, "receipt.jpg",
          createValidResult(), LocalDateTime.now());

      Set<ConstraintViolation<BillAnalysisResponse>> violations =
          validator.validate(response);

      assertThat(violations).isNotEmpty();
      assertThat(violations)
          .anyMatch(v -> v.getPropertyPath().toString().equals("id"));
    }

    @Test
    void shouldRejectBlankFileName() {
      BillAnalysisResponse response = new BillAnalysisResponse(
          UUID.randomUUID(), "",
          createValidResult(), LocalDateTime.now());

      Set<ConstraintViolation<BillAnalysisResponse>> violations =
          validator.validate(response);

      assertThat(violations).isNotEmpty();
      assertThat(violations)
          .anyMatch(v -> v.getPropertyPath().toString()
              .equals("originalFileName"));
    }
  }

  private static LineItem createValidLineItem() {
    return new LineItem("Item", new BigDecimal("1"),
        new BigDecimal("9.99"), new BigDecimal("9.99"));
  }

  private static BillAnalysisResult createValidResult() {
    return new BillAnalysisResult(
        "Store", List.of(createValidLineItem()),
        new BigDecimal("9.99"), "PLN", List.of("test"));
  }
}
