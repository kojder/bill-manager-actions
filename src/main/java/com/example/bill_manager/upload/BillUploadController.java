package com.example.bill_manager.upload;

import com.example.bill_manager.ai.BillAnalysisService;
import com.example.bill_manager.dto.BillAnalysisResponse;
import com.example.bill_manager.dto.BillAnalysisResult;
import com.example.bill_manager.exception.AnalysisNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private static final Logger LOG = LoggerFactory.getLogger(BillUploadController.class);

  private static final String MIME_TYPE_PDF = "application/pdf";
  private static final String MIME_TYPE_JPEG = "image/jpeg";

  private final FileValidationService fileValidationService;
  private final ImagePreprocessingService imagePreprocessingService;
  private final PdfConversionService pdfConversionService;
  private final BillAnalysisService billAnalysisService;
  private final BillResultStore billResultStore;

  public BillUploadController(
      final FileValidationService fileValidationService,
      final ImagePreprocessingService imagePreprocessingService,
      final PdfConversionService pdfConversionService,
      final BillAnalysisService billAnalysisService,
      final BillResultStore billResultStore) {
    this.fileValidationService = fileValidationService;
    this.imagePreprocessingService = imagePreprocessingService;
    this.pdfConversionService = pdfConversionService;
    this.billAnalysisService = billAnalysisService;
    this.billResultStore = billResultStore;
  }

  @PostMapping("/upload")
  public ResponseEntity<BillAnalysisResponse> uploadBill(
      @RequestParam("file") final MultipartFile file) {
    LOG.info(
        "Upload request received: filename='{}', size={} bytes",
        file.getOriginalFilename(),
        file.getSize());

    final String detectedMimeType = fileValidationService.validateFile(file);
    final String sanitizedFilename =
        fileValidationService.sanitizeFilename(file.getOriginalFilename());
    LOG.debug(
        "File validated: mimeType={}, sanitizedFilename='{}'", detectedMimeType, sanitizedFilename);
    final byte[] fileBytes = readFileBytes(file);

    final List<byte[]> processedImages;
    final String analysisMimeType;

    if (MIME_TYPE_PDF.equals(detectedMimeType)) {
      final List<byte[]> pageImages = pdfConversionService.convertToImages(fileBytes);
      processedImages =
          pageImages.stream()
              .map(img -> imagePreprocessingService.preprocess(img, MIME_TYPE_JPEG))
              .toList();
      analysisMimeType = MIME_TYPE_JPEG;
      LOG.debug("PDF detected, converted {} page(s) to images", pageImages.size());
    } else {
      final byte[] processedBytes =
          imagePreprocessingService.preprocess(fileBytes, detectedMimeType);
      processedImages = List.of(processedBytes);
      analysisMimeType = detectedMimeType;
    }

    final BillAnalysisResult analysis =
        billAnalysisService.analyze(processedImages, analysisMimeType);

    final UUID id = UUID.randomUUID();
    final BillAnalysisResponse response =
        new BillAnalysisResponse(id, sanitizedFilename, analysis, Instant.now());
    billResultStore.save(id, response);
    LOG.info(
        "Bill analysis completed: id={}, merchant='{}', items={}",
        id,
        analysis.merchantName(),
        analysis.items().size());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<BillAnalysisResponse> getAnalysisResult(@PathVariable final UUID id) {
    LOG.debug("Retrieving analysis result: id={}", id);
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
