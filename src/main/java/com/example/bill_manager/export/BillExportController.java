package com.example.bill_manager.export;

import com.example.bill_manager.dto.BillAnalysisResponse;
import com.example.bill_manager.exception.AnalysisNotFoundException;
import com.example.bill_manager.upload.BillResultStore;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bills")
public class BillExportController {

  private final BillResultStore billResultStore;
  private final BillCsvExportService billCsvExportService;

  public BillExportController(
      final BillResultStore billResultStore, final BillCsvExportService billCsvExportService) {
    this.billResultStore = billResultStore;
    this.billCsvExportService = billCsvExportService;
  }

  @GetMapping("/{id}/export/csv")
  public ResponseEntity<byte[]> exportCsv(@PathVariable final UUID id) {
    final BillAnalysisResponse response =
        billResultStore.findById(id).orElseThrow(() -> new AnalysisNotFoundException(id));
    final byte[] csvBytes =
        billCsvExportService.exportToCsv(response).getBytes(StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"bill-"
                + response.originalFileName().replaceAll("[\"\n\r]", "_")
                + ".csv\"")
        .body(csvBytes);
  }
}
