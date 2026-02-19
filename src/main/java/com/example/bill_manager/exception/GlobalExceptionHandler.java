package com.example.bill_manager.exception;

import com.example.bill_manager.dto.ErrorResponse;
import com.example.bill_manager.upload.FileValidationException;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(FileValidationException.class)
  public ResponseEntity<ErrorResponse> handleFileValidation(
      final FileValidationException ex) {
    final HttpStatus status = mapErrorCodeToStatus(ex.getErrorCode());
    final ErrorResponse response = new ErrorResponse(
        ex.getErrorCode().name(), ex.getMessage(), LocalDateTime.now());
    return ResponseEntity.status(status).body(response);
  }

  @ExceptionHandler(AnalysisNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleAnalysisNotFound(
      final AnalysisNotFoundException ex) {
    final ErrorResponse response = new ErrorResponse(
        "ANALYSIS_NOT_FOUND", ex.getMessage(), LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(
      final MethodArgumentTypeMismatchException ex) {
    final ErrorResponse response = new ErrorResponse(
        "INVALID_ID_FORMAT",
        "ID must be a valid UUID",
        LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(MissingServletRequestPartException.class)
  public ResponseEntity<ErrorResponse> handleMissingRequestPart(
      final MissingServletRequestPartException ex) {
    final ErrorResponse response = new ErrorResponse(
        "FILE_REQUIRED",
        "File is required and must not be empty",
        LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleMaxUploadSize(
      final MaxUploadSizeExceededException ex) {
    final ErrorResponse response = new ErrorResponse(
        "FILE_TOO_LARGE",
        "File size exceeds the maximum allowed limit",
        LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(
      final Exception ex) {
    final ErrorResponse response = new ErrorResponse(
        "INTERNAL_ERROR",
        "An unexpected error occurred",
        LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(response);
  }

  private HttpStatus mapErrorCodeToStatus(
      final FileValidationException.ErrorCode errorCode) {
    return switch (errorCode) {
      case FILE_REQUIRED -> HttpStatus.BAD_REQUEST;
      case FILE_TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE;
      case UNSUPPORTED_MEDIA_TYPE -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
      case FILE_UNREADABLE -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }
}
