package com.example.bill_manager.upload;

import com.example.bill_manager.config.UploadProperties;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PdfConversionServiceImpl implements PdfConversionService {

  private static final Logger LOG = LoggerFactory.getLogger(PdfConversionServiceImpl.class);
  private static final float JPEG_QUALITY = 0.9f;

  private final UploadProperties uploadProperties;

  public PdfConversionServiceImpl(final UploadProperties uploadProperties) {
    this.uploadProperties = uploadProperties;
  }

  @Override
  public List<byte[]> convertToImages(final byte[] pdfContent) {
    if (pdfContent == null) {
      throw new PdfConversionException(
          PdfConversionException.ErrorCode.PDF_READ_FAILED, "PDF content must not be null");
    }

    LOG.debug(
        "Starting PDF conversion: {} bytes, maxPages={}, dpi={}",
        pdfContent.length,
        uploadProperties.pdfMaxPages(),
        uploadProperties.pdfRenderDpi());

    try (PDDocument document = Loader.loadPDF(pdfContent)) {
      validateDocument(document);

      final PDFRenderer renderer = new PDFRenderer(document);
      final int pageCount = document.getNumberOfPages();

      if (pageCount > uploadProperties.pdfMaxPages()) {
        throw new PdfConversionException(
            PdfConversionException.ErrorCode.PDF_TOO_MANY_PAGES,
            "PDF has "
                + pageCount
                + " pages, maximum allowed is "
                + uploadProperties.pdfMaxPages());
      }

      final List<byte[]> images = new ArrayList<>(pageCount);
      for (int i = 0; i < pageCount; i++) {
        final BufferedImage pageImage =
            renderer.renderImageWithDPI(i, uploadProperties.pdfRenderDpi(), ImageType.RGB);
        try {
          images.add(ImageWriteUtils.writeJpeg(pageImage, JPEG_QUALITY));
        } finally {
          pageImage.flush();
        }
      }

      LOG.debug(
          "Converted {} PDF page(s) to JPEG images at {} DPI",
          pageCount,
          uploadProperties.pdfRenderDpi());
      return images;

    } catch (final PdfConversionException e) {
      throw e;
    } catch (final ImagePreprocessingException e) {
      throw new PdfConversionException(
          PdfConversionException.ErrorCode.CONVERSION_FAILED,
          "Failed to write PDF page as JPEG image",
          e);
    } catch (final InvalidPasswordException e) {
      throw new PdfConversionException(
          PdfConversionException.ErrorCode.PDF_ENCRYPTED,
          "Password-protected PDFs are not supported",
          e);
    } catch (final IOException e) {
      throw new PdfConversionException(
          PdfConversionException.ErrorCode.PDF_READ_FAILED, "Failed to read PDF content", e);
    }
  }

  private void validateDocument(final PDDocument document) {
    if (document.isEncrypted()) {
      throw new PdfConversionException(
          PdfConversionException.ErrorCode.PDF_ENCRYPTED,
          "Password-protected PDFs are not supported");
    }
    if (document.getNumberOfPages() == 0) {
      throw new PdfConversionException(
          PdfConversionException.ErrorCode.PDF_EMPTY, "PDF has no pages to convert");
    }
  }
}
