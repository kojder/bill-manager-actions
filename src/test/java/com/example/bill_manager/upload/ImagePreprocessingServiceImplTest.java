package com.example.bill_manager.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ImagePreprocessingServiceImplTest {

  private ImagePreprocessingServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new ImagePreprocessingServiceImpl();
  }

  @Nested
  class PdfPassthrough {

    @Test
    void shouldReturnPdfBytesUnchanged() {
      final byte[] pdfContent = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};

      final byte[] result = service.preprocess(pdfContent, "application/pdf");

      assertThat(result).isEqualTo(pdfContent);
    }
  }

  @Nested
  class JpegPreprocessing {

    @Test
    void shouldResizeJpegWiderThan1200px() throws IOException {
      final byte[] input = createTestJpeg(2400, 1600);

      final byte[] output = service.preprocess(input, "image/jpeg");

      final BufferedImage result = readImage(output);
      assertThat(result.getWidth()).isEqualTo(1200);
      assertThat(result.getHeight()).isEqualTo(800);
    }

    @Test
    void shouldNotResizeJpegNarrowerThan1200px() throws IOException {
      final byte[] input = createTestJpeg(800, 600);

      final byte[] output = service.preprocess(input, "image/jpeg");

      final BufferedImage result = readImage(output);
      assertThat(result.getWidth()).isEqualTo(800);
      assertThat(result.getHeight()).isEqualTo(600);
    }

    @Test
    void shouldPreserveAspectRatioWhenResizingJpeg() throws IOException {
      final byte[] input = createTestJpeg(1800, 1200);

      final byte[] output = service.preprocess(input, "image/jpeg");

      final BufferedImage result = readImage(output);
      assertThat(result.getWidth()).isEqualTo(1200);
      assertThat(result.getHeight()).isEqualTo(800);
    }

    @Test
    void shouldStripExifMetadataFromJpeg() throws IOException {
      final byte[] inputWithExif = createJpegWithExifMarker(800, 600);

      final byte[] output = service.preprocess(inputWithExif, "image/jpeg");

      assertThat(containsExifApp1Marker(output)).isFalse();
    }

    @Test
    void shouldProduceValidJpegOutput() throws IOException {
      final byte[] input = createTestJpeg(1600, 1200);

      final byte[] output = service.preprocess(input, "image/jpeg");

      assertThat(output[0]).isEqualTo((byte) 0xFF);
      assertThat(output[1]).isEqualTo((byte) 0xD8);
      assertThat(readImage(output)).isNotNull();
    }
  }

  @Nested
  class PngPreprocessing {

    @Test
    void shouldResizePngWiderThan1200px() throws IOException {
      final byte[] input = createTestPng(2400, 1600);

      final byte[] output = service.preprocess(input, "image/png");

      final BufferedImage result = readImage(output);
      assertThat(result.getWidth()).isEqualTo(1200);
      assertThat(result.getHeight()).isEqualTo(800);
    }

    @Test
    void shouldNotResizePngNarrowerThan1200px() throws IOException {
      final byte[] input = createTestPng(800, 600);

      final byte[] output = service.preprocess(input, "image/png");

      final BufferedImage result = readImage(output);
      assertThat(result.getWidth()).isEqualTo(800);
      assertThat(result.getHeight()).isEqualTo(600);
    }

    @Test
    void shouldPreserveTransparencyInResizedPng() throws IOException {
      final byte[] input = createTestPng(2400, 1600);

      final byte[] output = service.preprocess(input, "image/png");

      final BufferedImage result = readImage(output);
      assertThat(result.getColorModel().hasAlpha()).isTrue();
    }
  }

  @Nested
  class EdgeCases {

    @Test
    void shouldHandleImageAtExactly1200pxWidth() throws IOException {
      final byte[] input = createTestJpeg(1200, 900);

      final byte[] output = service.preprocess(input, "image/jpeg");

      final BufferedImage result = readImage(output);
      assertThat(result.getWidth()).isEqualTo(1200);
      assertThat(result.getHeight()).isEqualTo(900);
    }

    @Test
    void shouldHandleVerySmallImage() throws IOException {
      final byte[] input = createTestPng(1, 1);

      final byte[] output = service.preprocess(input, "image/png");

      final BufferedImage result = readImage(output);
      assertThat(result.getWidth()).isEqualTo(1);
      assertThat(result.getHeight()).isEqualTo(1);
    }
  }

  @Nested
  class ErrorHandling {

    @Test
    void shouldThrowExceptionForCorruptedImageData() {
      final byte[] corrupted = {0x00, 0x01, 0x02, 0x03, 0x04};

      assertThatThrownBy(() -> service.preprocess(corrupted, "image/jpeg"))
          .isInstanceOf(ImagePreprocessingException.class)
          .extracting(e -> ((ImagePreprocessingException) e).getErrorCode())
          .isEqualTo(ImagePreprocessingException.ErrorCode.IMAGE_READ_FAILED);
    }

    @Test
    void shouldThrowExceptionForNullContent() {
      assertThatThrownBy(() -> service.preprocess(null, "image/jpeg"))
          .isInstanceOf(ImagePreprocessingException.class)
          .extracting(e -> ((ImagePreprocessingException) e).getErrorCode())
          .isEqualTo(ImagePreprocessingException.ErrorCode.IMAGE_READ_FAILED);
    }

    @Test
    void shouldThrowExceptionForNullMimeType() throws IOException {
      final byte[] input = createTestJpeg(100, 100);

      assertThatThrownBy(() -> service.preprocess(input, null))
          .isInstanceOf(ImagePreprocessingException.class)
          .extracting(e -> ((ImagePreprocessingException) e).getErrorCode())
          .isEqualTo(ImagePreprocessingException.ErrorCode.IMAGE_READ_FAILED);
    }
  }

  private byte[] createTestJpeg(final int width, final int height)
      throws IOException {
    final BufferedImage image = new BufferedImage(
        width, height, BufferedImage.TYPE_INT_RGB);
    final Graphics2D g = image.createGraphics();
    try {
      g.setColor(Color.BLUE);
      g.fillRect(0, 0, width, height);
    } finally {
      g.dispose();
    }
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(image, "jpg", baos);
    return baos.toByteArray();
  }

  private byte[] createTestPng(final int width, final int height)
      throws IOException {
    final BufferedImage image = new BufferedImage(
        width, height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    try {
      g.setColor(new Color(255, 0, 0, 128));
      g.fillRect(0, 0, width, height);
    } finally {
      g.dispose();
    }
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(image, "png", baos);
    return baos.toByteArray();
  }

  // Synthetic EXIF: splices a minimal APP1 (0xFFE1) segment between the SOI
  // marker and the encoder's first marker. ImageIO tolerates this on standard
  // JREs (OpenJDK, Oracle). If this test becomes flaky on an exotic JRE,
  // replace with a pre-built JPEG fixture containing real EXIF data.
  private byte[] createJpegWithExifMarker(final int width, final int height)
      throws IOException {
    final byte[] jpeg = createTestJpeg(width, height);
    final byte[] exifSegment = buildMinimalExifSegment();
    final byte[] combined = new byte[2 + exifSegment.length + jpeg.length - 2];
    combined[0] = (byte) 0xFF;
    combined[1] = (byte) 0xD8;
    System.arraycopy(exifSegment, 0, combined, 2, exifSegment.length);
    System.arraycopy(jpeg, 2, combined, 2 + exifSegment.length,
        jpeg.length - 2);
    return combined;
  }

  private byte[] buildMinimalExifSegment() {
    final byte[] exifHeader = {
        (byte) 0xFF, (byte) 0xE1, 0x00, 0x10,
        0x45, 0x78, 0x69, 0x66, 0x00, 0x00,
        0x4D, 0x4D, 0x00, 0x2A, 0x00, 0x00
    };
    return exifHeader;
  }

  private boolean containsExifApp1Marker(final byte[] jpegData) {
    for (int i = 0; i < jpegData.length - 1; i++) {
      if (jpegData[i] == (byte) 0xFF && jpegData[i + 1] == (byte) 0xE1) {
        if (i + 5 < jpegData.length
            && jpegData[i + 4] == 0x45
            && jpegData[i + 5] == 0x78) {
          return true;
        }
      }
    }
    return false;
  }

  private BufferedImage readImage(final byte[] imageData) throws IOException {
    return ImageIO.read(new ByteArrayInputStream(imageData));
  }
}
