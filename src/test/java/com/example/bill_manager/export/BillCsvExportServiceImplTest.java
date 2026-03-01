package com.example.bill_manager.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bill_manager.dto.BillAnalysisResponse;
import com.example.bill_manager.dto.BillAnalysisResult;
import com.example.bill_manager.dto.LineItem;
import com.example.bill_manager.dto.PurchaseCategory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BillCsvExportServiceImplTest {

  private static final Instant FIXED_INSTANT = Instant.parse("2026-03-01T12:30:45Z");
  private static final UUID FIXED_ID = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");

  // spotless:off
  private static final BillAnalysisResult MOCK_ANALYSIS =
      new BillAnalysisResult(
          "Lidl Polska",
          List.of(
              new LineItem("Mleko 2%", BigDecimal.ONE, new BigDecimal("3.49"), new BigDecimal("3.49")),
              new LineItem("Chleb zytni", BigDecimal.ONE, new BigDecimal("2.99"), new BigDecimal("2.99"))),
          new BigDecimal("6.48"),
          "PLN",
          List.of(PurchaseCategory.GROCERY));
  // spotless:on

  private BillCsvExportServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new BillCsvExportServiceImpl();
  }

  private BillAnalysisResponse buildResponse(final List<PurchaseCategory> categories) {
    final BillAnalysisResult analysis =
        new BillAnalysisResult(
            MOCK_ANALYSIS.merchantName(),
            MOCK_ANALYSIS.items(),
            MOCK_ANALYSIS.totalAmount(),
            MOCK_ANALYSIS.currency(),
            categories);
    return new BillAnalysisResponse(FIXED_ID, "receipt.pdf", analysis, FIXED_INSTANT);
  }

  @Nested
  class CsvStructure {

    @Test
    void shouldStartWithUtf8Bom() {
      final String csv = service.exportToCsv(buildResponse(null));
      assertThat(csv).startsWith("\uFEFF");
    }

    @Test
    void shouldContainHeaderSectionLabels() {
      final String csv = service.exportToCsv(buildResponse(null));
      assertThat(csv).contains("Merchant", "File", "Date", "Currency", "Total");
    }

    @Test
    void shouldContainHeaderSectionValues() {
      final String csv = service.exportToCsv(buildResponse(null));
      assertThat(csv).contains("Lidl Polska", "receipt.pdf", "PLN", "6.48");
    }

    @Test
    void shouldContainItemsSectionLabels() {
      final String csv = service.exportToCsv(buildResponse(null));
      assertThat(csv).contains("Item Name", "Quantity", "Unit Price", "Total Price");
    }

    @Test
    void shouldContainAllLineItems() {
      final String csv = service.exportToCsv(buildResponse(null));
      assertThat(csv).contains("Mleko 2%", "Chleb zytni", "3.49", "2.99");
    }
  }

  @Nested
  class CategoriesSection {

    @Test
    void shouldContainCategoriesWhenPresent() {
      final String csv = service.exportToCsv(buildResponse(List.of(PurchaseCategory.GROCERY)));
      assertThat(csv).contains("Categories").contains("GROCERY");
    }

    @Test
    void shouldNotContainCategoriesWhenNull() {
      final String csv = service.exportToCsv(buildResponse(null));
      assertThat(csv).doesNotContain("Categories");
    }

    @Test
    void shouldNotContainCategoriesWhenEmpty() {
      final String csv = service.exportToCsv(buildResponse(List.of()));
      assertThat(csv).doesNotContain("Categories");
    }
  }

  @Nested
  class NullHandling {

    @Test
    void shouldThrowWhenResponseIsNull() {
      assertThatThrownBy(() -> service.exportToCsv(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Response must not be null");
    }
  }

  @Nested
  class CsvEscaping {

    @Test
    void shouldQuoteValuesContainingComma() {
      // spotless:off
      final BillAnalysisResult analysis =
          new BillAnalysisResult(
              "Store, Inc.",
              List.of(new LineItem("Item", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE)),
              BigDecimal.ONE,
              "PLN",
              null);
      // spotless:on
      final BillAnalysisResponse response =
          new BillAnalysisResponse(FIXED_ID, "file.pdf", analysis, FIXED_INSTANT);
      final String csv = service.exportToCsv(response);
      assertThat(csv).contains("\"Store, Inc.\"");
    }

    @Test
    void shouldPrefixFormulaInjectionTriggerCharacters() {
      // spotless:off
      final BillAnalysisResult analysis =
          new BillAnalysisResult(
              "=SUM(A1)",
              List.of(new LineItem("Item", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE)),
              BigDecimal.ONE,
              "PLN",
              null);
      // spotless:on
      final BillAnalysisResponse response =
          new BillAnalysisResponse(FIXED_ID, "file.pdf", analysis, FIXED_INSTANT);
      final String csv = service.exportToCsv(response);
      assertThat(csv).contains("\"'=SUM(A1)\"");
    }

    @Test
    void shouldEscapeInternalQuotes() {
      // spotless:off
      final BillAnalysisResult analysis =
          new BillAnalysisResult(
              "Store \"Best\"",
              List.of(new LineItem("Item", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE)),
              BigDecimal.ONE,
              "PLN",
              null);
      // spotless:on
      final BillAnalysisResponse response =
          new BillAnalysisResponse(FIXED_ID, "file.pdf", analysis, FIXED_INSTANT);
      final String csv = service.exportToCsv(response);
      assertThat(csv).contains("\"Store \"\"Best\"\"\"");
    }
  }
}
