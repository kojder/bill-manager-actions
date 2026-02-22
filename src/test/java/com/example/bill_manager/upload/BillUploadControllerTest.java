package com.example.bill_manager.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bill_manager.ai.BillAnalysisService;
import com.example.bill_manager.dto.BillAnalysisResponse;
import com.example.bill_manager.dto.BillAnalysisResult;
import com.example.bill_manager.dto.LineItem;
import com.example.bill_manager.dto.PurchaseCategory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(BillUploadController.class)
class BillUploadControllerTest {

  private static final String MIME_JPEG = "image/jpeg";
  private static final String MIME_PDF = "application/pdf";
  private static final byte[] SAMPLE_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

  // spotless:off
  private static final BillAnalysisResult MOCK_ANALYSIS =
      new BillAnalysisResult(
          "Test Store",
          List.of(new LineItem("Milk", BigDecimal.ONE, new BigDecimal("3.49"), new BigDecimal("3.49"))),
          new BigDecimal("3.49"),
          "PLN",
          List.of(PurchaseCategory.GROCERY));
  // spotless:on

  @Autowired private MockMvc mockMvc;

  @MockitoBean private FileValidationService fileValidationService;

  @MockitoBean private ImagePreprocessingService imagePreprocessingService;

  @MockitoBean private PdfConversionService pdfConversionService;

  @MockitoBean private BillAnalysisService billAnalysisService;

  @MockitoBean private BillResultStore billResultStore;

  private void setupSuccessfulImagePipeline() {
    when(fileValidationService.validateFile(any(MultipartFile.class))).thenReturn(MIME_JPEG);
    when(imagePreprocessingService.preprocess(any(byte[].class), eq(MIME_JPEG)))
        .thenReturn(SAMPLE_JPEG);
    when(billAnalysisService.analyze(anyList(), eq(MIME_JPEG))).thenReturn(MOCK_ANALYSIS);
  }

  private void setupSuccessfulPdfPipeline() {
    when(fileValidationService.validateFile(any(MultipartFile.class))).thenReturn(MIME_PDF);
    when(pdfConversionService.convertToImages(any(byte[].class))).thenReturn(List.of(SAMPLE_JPEG));
    when(imagePreprocessingService.preprocess(any(byte[].class), eq(MIME_JPEG)))
        .thenReturn(SAMPLE_JPEG);
    when(billAnalysisService.analyze(anyList(), eq(MIME_JPEG))).thenReturn(MOCK_ANALYSIS);
  }

  @Nested
  class UploadEndpoint {

    @Test
    void shouldReturn201WhenValidImageUploaded() throws Exception {
      setupSuccessfulImagePipeline();
      when(fileValidationService.sanitizeFilename("photo.jpg")).thenReturn("photo.jpg");

      final MockMultipartFile file =
          new MockMultipartFile("file", "photo.jpg", MIME_JPEG, SAMPLE_JPEG);

      mockMvc
          .perform(multipart("/api/bills/upload").file(file))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").isNotEmpty())
          .andExpect(jsonPath("$.originalFileName").value("photo.jpg"))
          .andExpect(jsonPath("$.analysis.merchantName").value("Test Store"))
          .andExpect(jsonPath("$.analysis.totalAmount").value(3.49))
          .andExpect(jsonPath("$.analysis.currency").value("PLN"))
          .andExpect(jsonPath("$.analyzedAt").isNotEmpty());
    }

    @Test
    void shouldReturn201WhenValidPdfUploaded() throws Exception {
      setupSuccessfulPdfPipeline();
      when(fileValidationService.sanitizeFilename("invoice.pdf")).thenReturn("invoice.pdf");

      final MockMultipartFile file =
          new MockMultipartFile("file", "invoice.pdf", MIME_PDF, "%PDF-1.4 test".getBytes());

      mockMvc
          .perform(multipart("/api/bills/upload").file(file))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.originalFileName").value("invoice.pdf"))
          .andExpect(jsonPath("$.analysis.merchantName").value("Test Store"));

      verify(pdfConversionService).convertToImages(any(byte[].class));
    }

    @Test
    void shouldNotCallPdfConversionForImageUpload() throws Exception {
      setupSuccessfulImagePipeline();
      when(fileValidationService.sanitizeFilename("photo.jpg")).thenReturn("photo.jpg");

      final MockMultipartFile file =
          new MockMultipartFile("file", "photo.jpg", MIME_JPEG, SAMPLE_JPEG);

      mockMvc.perform(multipart("/api/bills/upload").file(file)).andExpect(status().isCreated());

      verify(pdfConversionService, never()).convertToImages(any());
    }

    @Test
    void shouldReturnSanitizedFilename() throws Exception {
      setupSuccessfulImagePipeline();
      when(fileValidationService.sanitizeFilename("../../evil.jpg")).thenReturn("evil.jpg");

      final MockMultipartFile file =
          new MockMultipartFile("file", "../../evil.jpg", MIME_JPEG, SAMPLE_JPEG);

      mockMvc
          .perform(multipart("/api/bills/upload").file(file))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.originalFileName").value("evil.jpg"));
    }

    @Test
    void shouldReturn400WhenFileIsEmpty() throws Exception {
      final MockMultipartFile file =
          new MockMultipartFile("file", "empty.jpg", MIME_JPEG, new byte[0]);

      doThrow(
              new FileValidationException(
                  FileValidationException.ErrorCode.FILE_REQUIRED,
                  "File is required and must not be empty"))
          .when(fileValidationService)
          .validateFile(any(MultipartFile.class));

      mockMvc
          .perform(multipart("/api/bills/upload").file(file))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("FILE_REQUIRED"))
          .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void shouldReturn413WhenFileTooLarge() throws Exception {
      final MockMultipartFile file =
          new MockMultipartFile("file", "large.jpg", MIME_JPEG, new byte[] {1, 2, 3});

      doThrow(
              new FileValidationException(
                  FileValidationException.ErrorCode.FILE_TOO_LARGE,
                  "File size exceeds maximum allowed size"))
          .when(fileValidationService)
          .validateFile(any(MultipartFile.class));

      mockMvc
          .perform(multipart("/api/bills/upload").file(file))
          .andExpect(status().isPayloadTooLarge())
          .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
    }

    @Test
    void shouldReturn415WhenUnsupportedMimeType() throws Exception {
      final MockMultipartFile file =
          new MockMultipartFile("file", "doc.txt", "text/plain", "hello".getBytes());

      doThrow(
              new FileValidationException(
                  FileValidationException.ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                  "File type is not supported"))
          .when(fileValidationService)
          .validateFile(any(MultipartFile.class));

      mockMvc
          .perform(multipart("/api/bills/upload").file(file))
          .andExpect(status().isUnsupportedMediaType())
          .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void shouldReturn422WhenPdfCorrupted() throws Exception {
      when(fileValidationService.validateFile(any(MultipartFile.class))).thenReturn(MIME_PDF);
      when(fileValidationService.sanitizeFilename("broken.pdf")).thenReturn("broken.pdf");
      doThrow(
              new PdfConversionException(
                  PdfConversionException.ErrorCode.PDF_READ_FAILED, "Failed to read PDF content"))
          .when(pdfConversionService)
          .convertToImages(any(byte[].class));

      final MockMultipartFile file =
          new MockMultipartFile("file", "broken.pdf", MIME_PDF, "%PDF-broken".getBytes());

      mockMvc
          .perform(multipart("/api/bills/upload").file(file))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.code").value("PDF_READ_FAILED"));
    }

    @Test
    void shouldReturn400WhenPdfEncrypted() throws Exception {
      when(fileValidationService.validateFile(any(MultipartFile.class))).thenReturn(MIME_PDF);
      when(fileValidationService.sanitizeFilename("encrypted.pdf")).thenReturn("encrypted.pdf");
      doThrow(
              new PdfConversionException(
                  PdfConversionException.ErrorCode.PDF_ENCRYPTED,
                  "Password-protected PDFs are not supported"))
          .when(pdfConversionService)
          .convertToImages(any(byte[].class));

      final MockMultipartFile file =
          new MockMultipartFile("file", "encrypted.pdf", MIME_PDF, "%PDF-encrypted".getBytes());

      mockMvc
          .perform(multipart("/api/bills/upload").file(file))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("PDF_ENCRYPTED"));
    }

    @Test
    void shouldReturn400WhenPdfHasTooManyPages() throws Exception {
      when(fileValidationService.validateFile(any(MultipartFile.class))).thenReturn(MIME_PDF);
      when(fileValidationService.sanitizeFilename("large.pdf")).thenReturn("large.pdf");
      doThrow(
              new PdfConversionException(
                  PdfConversionException.ErrorCode.PDF_TOO_MANY_PAGES,
                  "PDF has 10 pages, maximum allowed is 5"))
          .when(pdfConversionService)
          .convertToImages(any(byte[].class));

      final MockMultipartFile file =
          new MockMultipartFile("file", "large.pdf", MIME_PDF, "%PDF-large".getBytes());

      mockMvc
          .perform(multipart("/api/bills/upload").file(file))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("PDF_TOO_MANY_PAGES"));
    }

    @Test
    void shouldReturn500WhenFileUnreadable() throws Exception {
      final MockMultipartFile file =
          new MockMultipartFile("file", "broken.jpg", MIME_JPEG, new byte[] {1, 2, 3});

      doThrow(
              new FileValidationException(
                  FileValidationException.ErrorCode.FILE_UNREADABLE, "Failed to read file content"))
          .when(fileValidationService)
          .validateFile(any(MultipartFile.class));

      mockMvc
          .perform(multipart("/api/bills/upload").file(file))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.code").value("FILE_UNREADABLE"));
    }
  }

  @Nested
  class RetrieveEndpoint {

    @Test
    void shouldReturn200WhenResultExists() throws Exception {
      final UUID id = UUID.randomUUID();
      final BillAnalysisResponse stored =
          new BillAnalysisResponse(id, "receipt.jpg", MOCK_ANALYSIS, Instant.now());

      when(billResultStore.findById(id)).thenReturn(Optional.of(stored));

      mockMvc
          .perform(get("/api/bills/" + id))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(id.toString()))
          .andExpect(jsonPath("$.originalFileName").value("receipt.jpg"))
          .andExpect(jsonPath("$.analysis.merchantName").value("Test Store"));
    }

    @Test
    void shouldReturn404WhenResultNotFound() throws Exception {
      final UUID randomUuid = UUID.randomUUID();

      when(billResultStore.findById(randomUuid)).thenReturn(Optional.empty());

      mockMvc
          .perform(get("/api/bills/" + randomUuid))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("ANALYSIS_NOT_FOUND"));
    }

    @Test
    void shouldReturn400WhenIdIsNotValidUuid() throws Exception {
      mockMvc
          .perform(get("/api/bills/not-a-uuid"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_ID_FORMAT"));
    }
  }

  @Nested
  class ErrorResponseFormat {

    @Test
    void shouldReturnStandardizedErrorStructure() throws Exception {
      final MockMultipartFile file =
          new MockMultipartFile("file", "empty.jpg", MIME_JPEG, new byte[0]);

      doThrow(
              new FileValidationException(
                  FileValidationException.ErrorCode.FILE_REQUIRED,
                  "File is required and must not be empty"))
          .when(fileValidationService)
          .validateFile(any(MultipartFile.class));

      mockMvc
          .perform(multipart("/api/bills/upload").file(file))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").isString())
          .andExpect(jsonPath("$.message").isString())
          .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void shouldNotExposeStackTrace() throws Exception {
      final MockMultipartFile file =
          new MockMultipartFile("file", "broken.jpg", MIME_JPEG, new byte[] {1, 2, 3});

      doThrow(new RuntimeException("Internal failure"))
          .when(fileValidationService)
          .validateFile(any(MultipartFile.class));

      final String responseBody =
          mockMvc
              .perform(multipart("/api/bills/upload").file(file))
              .andExpect(status().isInternalServerError())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertThat(responseBody).doesNotContain("stackTrace");
      assertThat(responseBody).doesNotContain("java.lang");
      assertThat(responseBody).doesNotContain("Internal failure");
    }
  }
}
