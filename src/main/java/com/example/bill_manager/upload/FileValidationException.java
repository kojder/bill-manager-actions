package com.example.bill_manager.upload;

import lombok.Getter;

@Getter
public class FileValidationException extends RuntimeException {

  public enum ErrorCode {
    FILE_REQUIRED,
    FILE_TOO_LARGE,
    UNSUPPORTED_MEDIA_TYPE,
    FILE_UNREADABLE
  }

  private final ErrorCode errorCode;

  public FileValidationException(final ErrorCode errorCode, final String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public FileValidationException(final ErrorCode errorCode, final String message,
      final Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }
}
