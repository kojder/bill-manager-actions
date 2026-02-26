package com.example.bill_manager.exception;

import com.example.bill_manager.ai.BillAnalysisException;
import com.example.bill_manager.dto.ErrorResponse;
import com.example.bill_manager.upload.FileValidationException;
import com.example.bill_manager.upload.ImagePreprocessingException;
import com.example.bill_manager.upload.PdfConversionException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(FileValidationException.class)
  public ResponseEntity<ErrorResponse> handleFileValidation(final FileValidationException ex) {
    final HttpStatus status = mapErrorCodeToStatus(ex.getErrorCode());
    if (status.is5xxServerError()) {
      LOG.error(
          "File validation error: code={}, message='{}'", ex.getErrorCode(), ex.getMessage(), ex);
    } else {
      LOG.warn("File validation failed: code={}, message='{}'", ex.getErrorCode(), ex.getMessage());
    }
    final ErrorResponse response =
        new ErrorResponse(ex.getErrorCode().name(), ex.getMessage(), Instant.now());
    return ResponseEntity.status(status).body(response);
  }

  @ExceptionHandler(AnalysisNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleAnalysisNotFound(final AnalysisNotFoundException ex) {
    LOG.warn("Analysis not found: {}", ex.getMessage());
    final ErrorResponse response =
        new ErrorResponse("ANALYSIS_NOT_FOUND", ex.getMessage(), Instant.now());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(
      final MethodArgumentTypeMismatchException ex) {
    LOG.warn("Invalid ID format: '{}'", ex.getValue());
    final ErrorResponse response =
        new ErrorResponse("INVALID_ID_FORMAT", "ID must be a valid UUID", Instant.now());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(MissingServletRequestPartException.class)
  public ResponseEntity<ErrorResponse> handleMissingRequestPart(
      final MissingServletRequestPartException ex) {
    LOG.warn("Missing request part: {}", ex.getRequestPartName());
    final ErrorResponse response =
        new ErrorResponse("FILE_REQUIRED", "File is required and must not be empty", Instant.now());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleMaxUploadSize(
      final MaxUploadSizeExceededException ex) {
    LOG.warn("Upload size exceeded: {}", ex.getMessage());
    final ErrorResponse response =
        new ErrorResponse(
            "FILE_TOO_LARGE", "File size exceeds the maximum allowed limit", Instant.now());
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
  }

  @ExceptionHandler(PdfConversionException.class)
  public ResponseEntity<ErrorResponse> handlePdfConversion(final PdfConversionException ex) {
    final HttpStatus status = mapPdfConversionErrorCodeToStatus(ex.getErrorCode());
    if (status.is5xxServerError()) {
      LOG.error(
          "PDF conversion error: code={}, message='{}'", ex.getErrorCode(), ex.getMessage(), ex);
    } else {
      LOG.warn("PDF conversion failed: code={}, message='{}'", ex.getErrorCode(), ex.getMessage());
    }
    final ErrorResponse response =
        new ErrorResponse(ex.getErrorCode().name(), ex.getMessage(), Instant.now());
    return ResponseEntity.status(status).body(response);
  }

  @ExceptionHandler(ImagePreprocessingException.class)
  public ResponseEntity<ErrorResponse> handleImagePreprocessing(
      final ImagePreprocessingException ex) {
    final HttpStatus status = mapPreprocessingErrorCodeToStatus(ex.getErrorCode());
    if (status.is5xxServerError()) {
      LOG.error(
          "Image preprocessing error: code={}, message='{}'",
          ex.getErrorCode(),
          ex.getMessage(),
          ex);
    } else {
      LOG.warn(
          "Image preprocessing failed: code={}, message='{}'", ex.getErrorCode(), ex.getMessage());
    }
    final ErrorResponse response =
        new ErrorResponse(ex.getErrorCode().name(), ex.getMessage(), Instant.now());
    return ResponseEntity.status(status).body(response);
  }

  @ExceptionHandler(BillAnalysisException.class)
  public ResponseEntity<ErrorResponse> handleBillAnalysis(final BillAnalysisException ex) {
    final HttpStatus status = mapBillAnalysisErrorCodeToStatus(ex.getErrorCode());
    if (status.is5xxServerError()) {
      LOG.error(
          "Bill analysis error: code={}, message='{}'", ex.getErrorCode(), ex.getMessage(), ex);
    } else {
      LOG.warn("Bill analysis failed: code={}, message='{}'", ex.getErrorCode(), ex.getMessage());
    }
    final ErrorResponse response =
        new ErrorResponse(ex.getErrorCode().name(), ex.getMessage(), Instant.now());
    return ResponseEntity.status(status).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(final Exception ex) {
    LOG.error("Unhandled exception", ex);
    final ErrorResponse response =
        new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred", Instant.now());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  private HttpStatus mapErrorCodeToStatus(final FileValidationException.ErrorCode errorCode) {
    return switch (errorCode) {
      case FILE_REQUIRED -> HttpStatus.BAD_REQUEST;
      case FILE_TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE;
      case UNSUPPORTED_MEDIA_TYPE -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
      case FILE_UNREADABLE -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }

  private HttpStatus mapPreprocessingErrorCodeToStatus(
      final ImagePreprocessingException.ErrorCode errorCode) {
    return switch (errorCode) {
      case IMAGE_READ_FAILED -> HttpStatus.UNPROCESSABLE_ENTITY;
      case PREPROCESSING_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }

  private HttpStatus mapPdfConversionErrorCodeToStatus(
      final PdfConversionException.ErrorCode errorCode) {
    return switch (errorCode) {
      case PDF_READ_FAILED, PDF_EMPTY -> HttpStatus.UNPROCESSABLE_ENTITY;
      case PDF_ENCRYPTED, PDF_TOO_MANY_PAGES -> HttpStatus.BAD_REQUEST;
      case CONVERSION_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }

  private HttpStatus mapBillAnalysisErrorCodeToStatus(
      final BillAnalysisException.ErrorCode errorCode) {
    return switch (errorCode) {
      case INVALID_INPUT, PROMPT_TOO_LARGE -> HttpStatus.BAD_REQUEST;
      case UNSUPPORTED_FORMAT -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
      case ANALYSIS_FAILED, INVALID_RESPONSE -> HttpStatus.INTERNAL_SERVER_ERROR;
      case SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
    };
  }
}
