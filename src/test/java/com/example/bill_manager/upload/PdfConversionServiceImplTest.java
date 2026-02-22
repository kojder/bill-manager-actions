package com.example.bill_manager.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bill_manager.config.UploadProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PdfConversionServiceImplTest {

  private static final int TEST_DPI = 150;
  private static final int TEST_MAX_PAGES = 3;

  private PdfConversionServiceImpl service;

  @BeforeEach
  void setUp() {
    final UploadProperties properties =
        new UploadProperties(
            10485760L,
            List.of("image/jpeg", "image/png", "application/pdf"),
            TEST_DPI,
            TEST_MAX_PAGES);
    service = new PdfConversionServiceImpl(properties);
  }

  @Nested
  class SuccessfulConversion {

    @Test
    void shouldConvertSinglePagePdfToOneJpegImage() throws IOException {
      final byte[] pdf = createPdf(1);

      final List<byte[]> images = service.convertToImages(pdf);

      assertThat(images).hasSize(1);
      assertJpegMagicBytes(images.get(0));
    }

    @Test
    void shouldConvertMultiPagePdfToMultipleJpegImages() throws IOException {
      final byte[] pdf = createPdf(2);

      final List<byte[]> images = service.convertToImages(pdf);

      assertThat(images).hasSize(2);
      images.forEach(PdfConversionServiceImplTest::assertJpegMagicBytes);
    }

    @Test
    void shouldConvertPdfWithExactlyMaxPages() throws IOException {
      final byte[] pdf = createPdf(TEST_MAX_PAGES);

      final List<byte[]> images = service.convertToImages(pdf);

      assertThat(images).hasSize(TEST_MAX_PAGES);
      images.forEach(PdfConversionServiceImplTest::assertJpegMagicBytes);
    }

    @Test
    void shouldProduceNonEmptyJpegOutput() throws IOException {
      final byte[] pdf = createPdf(1);

      final List<byte[]> images = service.convertToImages(pdf);

      assertThat(images.get(0).length).isGreaterThan(100);
    }
  }

  @Nested
  class ErrorHandling {

    @Test
    void shouldThrowForNullContent() {
      assertThatThrownBy(() -> service.convertToImages(null))
          .isInstanceOf(PdfConversionException.class)
          .satisfies(
              e ->
                  assertThat(((PdfConversionException) e).getErrorCode())
                      .isEqualTo(PdfConversionException.ErrorCode.PDF_READ_FAILED));
    }

    @Test
    void shouldThrowForCorruptedPdf() {
      final byte[] corrupted = "not a pdf at all".getBytes();

      assertThatThrownBy(() -> service.convertToImages(corrupted))
          .isInstanceOf(PdfConversionException.class)
          .satisfies(
              e ->
                  assertThat(((PdfConversionException) e).getErrorCode())
                      .isEqualTo(PdfConversionException.ErrorCode.PDF_READ_FAILED));
    }

    @Test
    void shouldThrowForEmptyPdf() throws IOException {
      final byte[] emptyPdf = createEmptyPdf();

      assertThatThrownBy(() -> service.convertToImages(emptyPdf))
          .isInstanceOf(PdfConversionException.class)
          .satisfies(
              e ->
                  assertThat(((PdfConversionException) e).getErrorCode())
                      .isEqualTo(PdfConversionException.ErrorCode.PDF_EMPTY));
    }

    @Test
    void shouldThrowForTooManyPages() throws IOException {
      final byte[] pdf = createPdf(TEST_MAX_PAGES + 1);

      assertThatThrownBy(() -> service.convertToImages(pdf))
          .isInstanceOf(PdfConversionException.class)
          .satisfies(
              e -> {
                final PdfConversionException ex = (PdfConversionException) e;
                assertThat(ex.getErrorCode())
                    .isEqualTo(PdfConversionException.ErrorCode.PDF_TOO_MANY_PAGES);
                assertThat(ex.getMessage()).contains(String.valueOf(TEST_MAX_PAGES));
              });
    }

    @Test
    void shouldThrowForEncryptedPdf() throws IOException {
      final byte[] encrypted = createEncryptedPdf();

      assertThatThrownBy(() -> service.convertToImages(encrypted))
          .isInstanceOf(PdfConversionException.class)
          .satisfies(
              e ->
                  assertThat(((PdfConversionException) e).getErrorCode())
                      .isEqualTo(PdfConversionException.ErrorCode.PDF_ENCRYPTED));
    }
  }

  private static byte[] createPdf(final int pageCount) throws IOException {
    try (PDDocument document = new PDDocument()) {
      for (int i = 0; i < pageCount; i++) {
        final PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
          content.beginText();
          content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
          content.newLineAtOffset(100, 700);
          content.showText("Page " + (i + 1));
          content.endText();
        }
      }
      final ByteArrayOutputStream output = new ByteArrayOutputStream();
      document.save(output);
      return output.toByteArray();
    }
  }

  private static byte[] createEmptyPdf() throws IOException {
    try (PDDocument document = new PDDocument()) {
      final ByteArrayOutputStream output = new ByteArrayOutputStream();
      document.save(output);
      return output.toByteArray();
    }
  }

  private static byte[] createEncryptedPdf() throws IOException {
    try (PDDocument document = new PDDocument()) {
      final PDPage page = new PDPage(PDRectangle.A4);
      document.addPage(page);
      final AccessPermission permissions = new AccessPermission();
      final StandardProtectionPolicy policy =
          new StandardProtectionPolicy("owner-password", "user-password", permissions);
      policy.setEncryptionKeyLength(128);
      document.protect(policy);
      final ByteArrayOutputStream output = new ByteArrayOutputStream();
      document.save(output);
      return output.toByteArray();
    }
  }

  private static void assertJpegMagicBytes(final byte[] data) {
    assertThat(data).hasSizeGreaterThan(2);
    assertThat(data[0]).isEqualTo((byte) 0xFF);
    assertThat(data[1]).isEqualTo((byte) 0xD8);
  }
}
