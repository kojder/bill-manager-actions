package com.example.bill_manager.upload;

import lombok.Getter;

@Getter
public class FileValidationException extends RuntimeException {

  public enum ErrorCode {
    FILE_REQUIRED,
    FILE_TOO_LARGE,
    UNSUPPORTED_MEDIA_TYPE
  }

  private final ErrorCode errorCode;

  public FileValidationException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }
}
