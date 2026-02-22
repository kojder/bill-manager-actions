package com.example.bill_manager.upload;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

final class ImageWriteUtils {

  private ImageWriteUtils() {}

  static byte[] writeJpeg(final BufferedImage image, final float quality) {
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
        params.setCompressionQuality(quality);
        writer.setOutput(imageOutputStream);
        writer.write(null, new IIOImage(image, null, null), params);
      } finally {
        writer.dispose();
      }
      return outputStream.toByteArray();
    } catch (final ImagePreprocessingException e) {
      throw e;
    } catch (final IOException e) {
      throw new ImagePreprocessingException(
          ImagePreprocessingException.ErrorCode.PREPROCESSING_FAILED,
          "Failed to write JPEG image",
          e);
    }
  }

  static byte[] writePng(final BufferedImage image) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      final boolean written = ImageIO.write(image, "png", outputStream);
      if (!written) {
        throw new ImagePreprocessingException(
            ImagePreprocessingException.ErrorCode.PREPROCESSING_FAILED,
            "No PNG ImageWriter available in this JRE");
      }
      return outputStream.toByteArray();
    } catch (final ImagePreprocessingException e) {
      throw e;
    } catch (final IOException e) {
      throw new ImagePreprocessingException(
          ImagePreprocessingException.ErrorCode.PREPROCESSING_FAILED,
          "Failed to write PNG image",
          e);
    }
  }
}
