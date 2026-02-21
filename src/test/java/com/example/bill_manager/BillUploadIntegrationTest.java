package com.example.bill_manager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bill_manager.ai.BillAnalysisException;
import com.example.bill_manager.ai.BillAnalysisService;
import com.example.bill_manager.dto.BillAnalysisResult;
import com.example.bill_manager.dto.LineItem;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class BillUploadIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private BillAnalysisService billAnalysisService;

  private byte[] validJpegBytes;

  @BeforeEach
  void setUp() throws Exception {
    final BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(image, "jpg", output);
    validJpegBytes = output.toByteArray();
  }

  @Nested
  class HappyPath {

    @Test
    void shouldAnalyzeJpegBillAndReturn201() throws Exception {
      final BillAnalysisResult mockResult =
          new BillAnalysisResult(
              "Sklep ABC",
              List.of(
                  new LineItem(
                      "Mleko", BigDecimal.ONE, new BigDecimal("3.49"), new BigDecimal("3.49"))),
              new BigDecimal("3.49"),
              "PLN",
              List.of("grocery"));

      when(billAnalysisService.analyze(any(byte[].class), eq("image/jpeg"))).thenReturn(mockResult);

      final MockMultipartFile file =
          new MockMultipartFile("file", "receipt.jpg", "image/jpeg", validJpegBytes);

      mockMvc
          .perform(multipart("/api/bills/upload").file(file))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").isNotEmpty())
          .andExpect(jsonPath("$.originalFileName").value("receipt.jpg"))
          .andExpect(jsonPath("$.analysis.merchantName").value("Sklep ABC"))
          .andExpect(jsonPath("$.analysis.totalAmount").value(3.49))
          .andExpect(jsonPath("$.analysis.currency").value("PLN"))
          .andExpect(jsonPath("$.analyzedAt").isNotEmpty());
    }

    @Test
    void shouldRetrieveStoredResultById() throws Exception {
      final BillAnalysisResult mockResult =
          new BillAnalysisResult(
              "Shop XYZ",
              List.of(
                  new LineItem(
                      "Bread", BigDecimal.ONE, new BigDecimal("5.00"), new BigDecimal("5.00"))),
              new BigDecimal("5.00"),
              "PLN",
              null);

      when(billAnalysisService.analyze(any(byte[].class), eq("image/jpeg"))).thenReturn(mockResult);

      final MockMultipartFile file =
          new MockMultipartFile("file", "bill.jpg", "image/jpeg", validJpegBytes);

      final MvcResult uploadResult =
          mockMvc
              .perform(multipart("/api/bills/upload").file(file))
              .andExpect(status().isCreated())
              .andReturn();

      final String responseBody = uploadResult.getResponse().getContentAsString();
      final String id = com.jayway.jsonpath.JsonPath.read(responseBody, "$.id");

      mockMvc
          .perform(get("/api/bills/" + id))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(id))
          .andExpect(jsonPath("$.analysis.merchantName").value("Shop XYZ"));
    }
  }

  @Nested
  class ErrorPaths {

    @Test
    void shouldReturn415WhenPdfUploaded() throws Exception {
      final byte[] pdfBytes = "%PDF-1.4 test".getBytes();

      when(billAnalysisService.analyze(any(byte[].class), eq("application/pdf")))
          .thenThrow(
              new BillAnalysisException(
                  BillAnalysisException.ErrorCode.UNSUPPORTED_FORMAT,
                  "PDF analysis is not yet supported"));

      final MockMultipartFile file =
          new MockMultipartFile("file", "invoice.pdf", "application/pdf", pdfBytes);

      mockMvc
          .perform(multipart("/api/bills/upload").file(file))
          .andExpect(status().isUnsupportedMediaType())
          .andExpect(jsonPath("$.code").value("UNSUPPORTED_FORMAT"));
    }

    @Test
    void shouldReturn503WhenAnalysisServiceUnavailable() throws Exception {
      when(billAnalysisService.analyze(any(byte[].class), any()))
          .thenThrow(
              new BillAnalysisException(
                  BillAnalysisException.ErrorCode.SERVICE_UNAVAILABLE,
                  "Bill analysis service is temporarily unavailable"));

      final MockMultipartFile file =
          new MockMultipartFile("file", "receipt.jpg", "image/jpeg", validJpegBytes);

      mockMvc
          .perform(multipart("/api/bills/upload").file(file))
          .andExpect(status().isServiceUnavailable())
          .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"));
    }

    @Test
    void shouldReturn415WhenUploadingTextFile() throws Exception {
      final byte[] textBytes = "This is plain text, not an image".getBytes();

      final MockMultipartFile file =
          new MockMultipartFile("file", "document.txt", "text/plain", textBytes);

      mockMvc
          .perform(multipart("/api/bills/upload").file(file))
          .andExpect(status().isUnsupportedMediaType())
          .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void shouldReturn404ForUnknownId() throws Exception {
      mockMvc
          .perform(get("/api/bills/00000000-0000-0000-0000-000000000000"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("ANALYSIS_NOT_FOUND"));
    }
  }

  @Nested
  class HealthEndpoint {

    @Test
    void shouldReturnUpStatus() throws Exception {
      mockMvc
          .perform(get("/api/health"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("UP"));
    }
  }
}
