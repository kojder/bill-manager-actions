package com.example.bill_manager.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bill_manager.dto.BillAnalysisResponse;
import java.time.Instant;
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

  @Autowired private MockMvc mockMvc;

  @MockitoBean private FileValidationService fileValidationService;

  @MockitoBean private BillResultStore billResultStore;

  @Nested
  class UploadEndpoint {

    @Test
    void shouldReturn201WhenValidFileUploaded() throws Exception {
      final MockMultipartFile file =
          new MockMultipartFile(
              "file",
              "photo.jpg",
              "image/jpeg",
              new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

      doNothing().when(fileValidationService).validateFile(any(MultipartFile.class));
      when(fileValidationService.sanitizeFilename("photo.jpg")).thenReturn("photo.jpg");

      mockMvc
          .perform(multipart("/api/bills/upload").file(file))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").isNotEmpty())
          .andExpect(jsonPath("$.originalFileName").value("photo.jpg"))
          .andExpect(jsonPath("$.analyzedAt").isNotEmpty());
    }

    @Test
    void shouldReturnSanitizedFilename() throws Exception {
      final MockMultipartFile file =
          new MockMultipartFile(
              "file",
              "../../evil.jpg",
              "image/jpeg",
              new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

      doNothing().when(fileValidationService).validateFile(any(MultipartFile.class));
      when(fileValidationService.sanitizeFilename("../../evil.jpg")).thenReturn("evil.jpg");

      mockMvc
          .perform(multipart("/api/bills/upload").file(file))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.originalFileName").value("evil.jpg"));
    }

    @Test
    void shouldReturn400WhenFileIsEmpty() throws Exception {
      final MockMultipartFile file =
          new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

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
          new MockMultipartFile("file", "large.jpg", "image/jpeg", new byte[] {1, 2, 3});

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
    void shouldReturn500WhenFileUnreadable() throws Exception {
      final MockMultipartFile file =
          new MockMultipartFile("file", "broken.jpg", "image/jpeg", new byte[] {1, 2, 3});

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
          new BillAnalysisResponse(id, "receipt.jpg", null, Instant.now());

      when(billResultStore.findById(id)).thenReturn(Optional.of(stored));

      mockMvc
          .perform(get("/api/bills/" + id))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(id.toString()))
          .andExpect(jsonPath("$.originalFileName").value("receipt.jpg"));
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
          new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

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
          new MockMultipartFile("file", "broken.jpg", "image/jpeg", new byte[] {1, 2, 3});

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
