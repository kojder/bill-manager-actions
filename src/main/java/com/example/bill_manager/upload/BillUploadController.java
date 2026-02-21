package com.example.bill_manager.upload;

import com.example.bill_manager.ai.BillAnalysisService;
import com.example.bill_manager.dto.BillAnalysisResponse;
import com.example.bill_manager.dto.BillAnalysisResult;
import com.example.bill_manager.exception.AnalysisNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/bills")
public class BillUploadController {

  private final FileValidationService fileValidationService;
  private final ImagePreprocessingService imagePreprocessingService;
  private final BillAnalysisService billAnalysisService;
  private final BillResultStore billResultStore;

  public BillUploadController(
      final FileValidationService fileValidationService,
      final ImagePreprocessingService imagePreprocessingService,
      final BillAnalysisService billAnalysisService,
      final BillResultStore billResultStore) {
    this.fileValidationService = fileValidationService;
    this.imagePreprocessingService = imagePreprocessingService;
    this.billAnalysisService = billAnalysisService;
    this.billResultStore = billResultStore;
  }

  @PostMapping("/upload")
  public ResponseEntity<BillAnalysisResponse> uploadBill(
      @RequestParam("file") final MultipartFile file) {
    final String detectedMimeType = fileValidationService.validateFile(file);
    final String sanitizedFilename =
        fileValidationService.sanitizeFilename(file.getOriginalFilename());

    final byte[] fileBytes = readFileBytes(file);
    final byte[] processedBytes = imagePreprocessingService.preprocess(fileBytes, detectedMimeType);
    final BillAnalysisResult analysis =
        billAnalysisService.analyze(processedBytes, detectedMimeType);

    final UUID id = UUID.randomUUID();
    final BillAnalysisResponse response =
        new BillAnalysisResponse(id, sanitizedFilename, analysis, Instant.now());
    billResultStore.save(id, response);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<BillAnalysisResponse> getAnalysisResult(@PathVariable final UUID id) {
    final BillAnalysisResponse result =
        billResultStore.findById(id).orElseThrow(() -> new AnalysisNotFoundException(id));
    return ResponseEntity.ok(result);
  }

  private byte[] readFileBytes(final MultipartFile file) {
    try {
      return file.getBytes();
    } catch (final IOException e) {
      throw new FileValidationException(
          FileValidationException.ErrorCode.FILE_UNREADABLE,
          "Failed to read uploaded file content",
          e);
    }
  }
}
