package com.example.bill_manager.export;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bill_manager.dto.BillAnalysisResponse;
import com.example.bill_manager.dto.BillAnalysisResult;
import com.example.bill_manager.dto.LineItem;
import com.example.bill_manager.dto.PurchaseCategory;
import com.example.bill_manager.upload.BillResultStore;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BillExportController.class)
class BillExportControllerTest {

  private static final UUID EXISTING_ID = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");
  private static final String CSV_CONTENT = "\uFEFFMerchant,File,Date,Currency,Total\n";

  // spotless:off
  private static final BillAnalysisResponse MOCK_RESPONSE =
      new BillAnalysisResponse(
          EXISTING_ID,
          "receipt.pdf",
          new BillAnalysisResult(
              "Test Store",
              List.of(new LineItem("Item", BigDecimal.ONE, new BigDecimal("1.00"), new BigDecimal("1.00"))),
              new BigDecimal("1.00"),
              "PLN",
              List.of(PurchaseCategory.GROCERY)),
          Instant.parse("2026-03-01T12:00:00Z"));
  // spotless:on

  @Autowired private MockMvc mockMvc;

  @MockitoBean private BillResultStore billResultStore;

  @MockitoBean private BillCsvExportService billCsvExportService;

  @Nested
  class ExportCsvEndpoint {

    @Test
    void shouldReturn200WithCsvContentTypeWhenFound() throws Exception {
      when(billResultStore.findById(EXISTING_ID)).thenReturn(Optional.of(MOCK_RESPONSE));
      when(billCsvExportService.exportToCsv(any())).thenReturn(CSV_CONTENT);

      mockMvc
          .perform(get("/api/bills/{id}/export/csv", EXISTING_ID))
          .andExpect(status().isOk())
          .andExpect(header().string("Content-Type", "text/csv; charset=UTF-8"))
          .andExpect(
              header()
                  .string("Content-Disposition", "attachment; filename=\"bill-receipt.pdf.csv\""));
    }

    @Test
    void shouldReturnCsvBodyMatchingServiceOutput() throws Exception {
      when(billResultStore.findById(EXISTING_ID)).thenReturn(Optional.of(MOCK_RESPONSE));
      when(billCsvExportService.exportToCsv(any())).thenReturn(CSV_CONTENT);

      mockMvc
          .perform(get("/api/bills/{id}/export/csv", EXISTING_ID))
          .andExpect(status().isOk())
          .andExpect(content().string(CSV_CONTENT));
    }

    @Test
    void shouldReturn404WhenNotFound() throws Exception {
      final UUID unknownId = UUID.randomUUID();
      when(billResultStore.findById(unknownId)).thenReturn(Optional.empty());

      mockMvc
          .perform(get("/api/bills/{id}/export/csv", unknownId))
          .andExpect(status().isNotFound());
    }
  }
}
