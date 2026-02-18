package com.example.bill_manager.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bill_manager.config.UploadProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileValidationServiceImplTest {

  private static final long MAX_FILE_SIZE = 10_485_760L;

  private FileValidationServiceImpl service;

  @BeforeEach
  void setUp() {
    UploadProperties properties = new UploadProperties(
        MAX_FILE_SIZE,
        List.of("image/jpeg", "image/png", "application/pdf")
    );
    service = new FileValidationServiceImpl(properties);
  }

  @Nested
  class ValidFileValidation {

    @Test
    void shouldAcceptValidJpegFile() {
      byte[] jpegContent = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
          0x00, 0x10, 0x4A, 0x46};
      MockMultipartFile file = new MockMultipartFile(
          "file", "photo.jpg", "image/jpeg", jpegContent);

      assertThatCode(() -> service.validateFile(file)).doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptValidPngFile() {
      byte[] pngContent = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
          0x00, 0x00, 0x00, 0x0D};
      MockMultipartFile file = new MockMultipartFile(
          "file", "image.png", "image/png", pngContent);

      assertThatCode(() -> service.validateFile(file)).doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptValidPdfFile() {
      byte[] pdfContent = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};
      MockMultipartFile file = new MockMultipartFile(
          "file", "document.pdf", "application/pdf", pdfContent);

      assertThatCode(() -> service.validateFile(file)).doesNotThrowAnyException();
    }
  }

  @Nested
  class FilePresenceValidation {

    @Test
    void shouldRejectNullFile() {
      assertThatThrownBy(() -> service.validateFile(null))
          .isInstanceOf(FileValidationException.class)
          .extracting(e -> ((FileValidationException) e).getErrorCode())
          .isEqualTo(FileValidationException.ErrorCode.FILE_REQUIRED);
    }

    @Test
    void shouldRejectEmptyFile() {
      MockMultipartFile file = new MockMultipartFile(
          "file", "empty.jpg", "image/jpeg", new byte[0]);

      assertThatThrownBy(() -> service.validateFile(file))
          .isInstanceOf(FileValidationException.class)
          .extracting(e -> ((FileValidationException) e).getErrorCode())
          .isEqualTo(FileValidationException.ErrorCode.FILE_REQUIRED);
    }
  }

  @Nested
  class FileSizeValidation {

    @Test
    void shouldRejectOversizedFile() {
      byte[] jpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
      byte[] oversizedContent = new byte[(int) MAX_FILE_SIZE + 1];
      System.arraycopy(jpegHeader, 0, oversizedContent, 0, jpegHeader.length);

      MockMultipartFile file = new MockMultipartFile(
          "file", "large.jpg", "image/jpeg", oversizedContent);

      assertThatThrownBy(() -> service.validateFile(file))
          .isInstanceOf(FileValidationException.class)
          .extracting(e -> ((FileValidationException) e).getErrorCode())
          .isEqualTo(FileValidationException.ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void shouldAcceptFileAtExactMaxSize() {
      byte[] content = new byte[(int) MAX_FILE_SIZE];
      content[0] = (byte) 0xFF;
      content[1] = (byte) 0xD8;
      content[2] = (byte) 0xFF;

      MockMultipartFile file = new MockMultipartFile(
          "file", "exact.jpg", "image/jpeg", content);

      assertThatCode(() -> service.validateFile(file)).doesNotThrowAnyException();
    }
  }

  @Nested
  class MimeTypeValidation {

    @Test
    void shouldRejectFileWithUnrecognizedMagicBytes() {
      byte[] randomBytes = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
      MockMultipartFile file = new MockMultipartFile(
          "file", "unknown.bin", "application/octet-stream", randomBytes);

      assertThatThrownBy(() -> service.validateFile(file))
          .isInstanceOf(FileValidationException.class)
          .extracting(e -> ((FileValidationException) e).getErrorCode())
          .isEqualTo(FileValidationException.ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void shouldRejectTextFileWithJpgExtension() {
      MockMultipartFile file = new MockMultipartFile(
          "file", "fake.jpg", "image/jpeg", "Hello World".getBytes());

      assertThatThrownBy(() -> service.validateFile(file))
          .isInstanceOf(FileValidationException.class)
          .extracting(e -> ((FileValidationException) e).getErrorCode())
          .isEqualTo(FileValidationException.ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void shouldRejectFileTooSmallForMagicBytesDetection() {
      MockMultipartFile file = new MockMultipartFile(
          "file", "tiny.bin", "application/octet-stream", new byte[]{0x01, 0x02});

      assertThatThrownBy(() -> service.validateFile(file))
          .isInstanceOf(FileValidationException.class)
          .extracting(e -> ((FileValidationException) e).getErrorCode())
          .isEqualTo(FileValidationException.ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }
  }

  @Nested
  class FilenameSanitization {

    @Test
    void shouldSanitizePathTraversalAttempt() {
      String sanitized = service.sanitizeFilename("../../etc/passwd");

      assertThat(sanitized).doesNotContain("..");
      assertThat(sanitized).doesNotContain("/");
    }

    @Test
    void shouldRemoveBackslashPathSeparators() {
      String sanitized = service.sanitizeFilename("..\\..\\windows\\system32");

      assertThat(sanitized).doesNotContain("\\");
      assertThat(sanitized).doesNotContain("..");
    }

    @Test
    void shouldHandleNullFilename() {
      assertThat(service.sanitizeFilename(null)).isEqualTo("unnamed_file");
    }

    @Test
    void shouldHandleBlankFilename() {
      assertThat(service.sanitizeFilename("   ")).isEqualTo("unnamed_file");
    }

    @Test
    void shouldRemoveControlCharacters() {
      String sanitized = service.sanitizeFilename("file\u0000name.pdf");

      assertThat(sanitized).doesNotContain("\u0000");
      assertThat(sanitized).isEqualTo("filename.pdf");
    }

    @Test
    void shouldPreserveValidFilename() {
      assertThat(service.sanitizeFilename("receipt_2024.jpg"))
          .isEqualTo("receipt_2024.jpg");
    }

    @Test
    void shouldTruncateLongFilename() {
      String longName = "a".repeat(300) + ".jpg";

      String sanitized = service.sanitizeFilename(longName);

      assertThat(sanitized).hasSizeLessThanOrEqualTo(255);
    }

    @Test
    void shouldRemoveLeadingDots() {
      String sanitized = service.sanitizeFilename(".hidden_file.txt");

      assertThat(sanitized).doesNotStartWith(".");
      assertThat(sanitized).isEqualTo("hidden_file.txt");
    }

    @Test
    void shouldHandleNestedTraversalAttempts() {
      String sanitized = service.sanitizeFilename("....//....//etc/passwd");

      assertThat(sanitized).doesNotContain("..");
      assertThat(sanitized).doesNotContain("/");
    }
  }
}
