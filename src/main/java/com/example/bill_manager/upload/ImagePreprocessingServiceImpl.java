package com.example.bill_manager.upload;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.stereotype.Service;

@Service
public class ImagePreprocessingServiceImpl implements ImagePreprocessingService {

  private static final int MAX_WIDTH_PX = 1200;
  private static final float JPEG_QUALITY = 0.9f;
  private static final String MIME_TYPE_PDF = "application/pdf";
  private static final String MIME_TYPE_JPEG = "image/jpeg";
  private static final String MIME_TYPE_PNG = "image/png";

  @Override
  public byte[] preprocess(final byte[] fileContent, final String mimeType) {
    if (fileContent == null) {
      throw new ImagePreprocessingException(
          ImagePreprocessingException.ErrorCode.IMAGE_READ_FAILED, "File content must not be null");
    }
    if (mimeType == null) {
      throw new ImagePreprocessingException(
          ImagePreprocessingException.ErrorCode.IMAGE_READ_FAILED, "MIME type must not be null");
    }

    if (MIME_TYPE_PDF.equals(mimeType)) {
      return fileContent;
    }

    final BufferedImage originalImage = readImage(fileContent);
    final int imageType = resolveImageType(mimeType);
    final BufferedImage processedImage =
        originalImage.getWidth() > MAX_WIDTH_PX
            ? resizeImage(originalImage, MAX_WIDTH_PX, imageType)
            : ensureImageType(originalImage, imageType);

    return writeImage(processedImage, mimeType);
  }

  private BufferedImage readImage(final byte[] fileContent) {
    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent)) {
      final BufferedImage image = ImageIO.read(inputStream);
      if (image == null) {
        throw new ImagePreprocessingException(
            ImagePreprocessingException.ErrorCode.IMAGE_READ_FAILED,
            "Failed to decode image — content may be corrupted");
      }
      return image;
    } catch (final IOException e) {
      throw new ImagePreprocessingException(
          ImagePreprocessingException.ErrorCode.IMAGE_READ_FAILED,
          "Failed to read image content",
          e);
    }
  }

  private BufferedImage resizeImage(
      final BufferedImage original, final int targetWidth, final int imageType) {
    final int targetHeight =
        (int) Math.round((double) original.getHeight() / original.getWidth() * targetWidth);
    final BufferedImage resized = new BufferedImage(targetWidth, targetHeight, imageType);
    final Graphics2D graphics = resized.createGraphics();
    try {
      graphics.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      graphics.drawImage(original, 0, 0, targetWidth, targetHeight, null);
    } finally {
      graphics.dispose();
    }
    return resized;
  }

  private BufferedImage ensureImageType(final BufferedImage original, final int imageType) {
    if (original.getType() == imageType) {
      return original;
    }
    final BufferedImage converted =
        new BufferedImage(original.getWidth(), original.getHeight(), imageType);
    final Graphics2D graphics = converted.createGraphics();
    try {
      graphics.drawImage(original, 0, 0, null);
    } finally {
      graphics.dispose();
    }
    return converted;
  }

  private byte[] writeImage(final BufferedImage image, final String mimeType) {
    if (MIME_TYPE_JPEG.equals(mimeType)) {
      return writeJpeg(image);
    }
    if (MIME_TYPE_PNG.equals(mimeType)) {
      return writePng(image);
    }
    throw new ImagePreprocessingException(
        ImagePreprocessingException.ErrorCode.PREPROCESSING_FAILED,
        "Unsupported MIME type for image write: " + mimeType);
  }

  private byte[] writeJpeg(final BufferedImage image) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      final Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
      if (!writers.hasNext()) {
        throw new ImagePreprocessingException(
            ImagePreprocessingException.ErrorCode.PREPROCESSING_FAILED,
            "No JPEG ImageWriter available in this JRE");
      }
      final ImageWriter writer = writers.next();
      try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
        final ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(JPEG_QUALITY);
        writer.setOutput(imageOutputStream);
        writer.write(null, new IIOImage(image, null, null), params);
      } finally {
        writer.dispose();
      }
      return outputStream.toByteArray();
    } catch (final IOException e) {
      throw new ImagePreprocessingException(
          ImagePreprocessingException.ErrorCode.PREPROCESSING_FAILED,
          "Failed to write preprocessed JPEG image",
          e);
    }
  }

  private byte[] writePng(final BufferedImage image) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      final boolean written = ImageIO.write(image, "png", outputStream);
      if (!written) {
        throw new ImagePreprocessingException(
            ImagePreprocessingException.ErrorCode.PREPROCESSING_FAILED,
            "No PNG ImageWriter available in this JRE");
      }
      return outputStream.toByteArray();
    } catch (final IOException e) {
      throw new ImagePreprocessingException(
          ImagePreprocessingException.ErrorCode.PREPROCESSING_FAILED,
          "Failed to write preprocessed PNG image",
          e);
    }
  }

  private int resolveImageType(final String mimeType) {
    return MIME_TYPE_PNG.equals(mimeType)
        ? BufferedImage.TYPE_INT_ARGB
        : BufferedImage.TYPE_INT_RGB;
  }
}
