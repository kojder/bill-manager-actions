package com.example.bill_manager.upload;

import lombok.Getter;

@Getter
public class PdfConversionException extends RuntimeException {

  public enum ErrorCode {
    PDF_READ_FAILED,
    PDF_ENCRYPTED,
    PDF_EMPTY,
    PDF_TOO_MANY_PAGES,
    CONVERSION_FAILED
  }

  private final ErrorCode errorCode;

  public PdfConversionException(final ErrorCode errorCode, final String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public PdfConversionException(
      final ErrorCode errorCode, final String message, final Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }
}
