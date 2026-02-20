package com.example.bill_manager.upload;

import lombok.Getter;

@Getter
public class ImagePreprocessingException extends RuntimeException {

  public enum ErrorCode {
    IMAGE_READ_FAILED,
    PREPROCESSING_FAILED
  }

  private final ErrorCode errorCode;

  public ImagePreprocessingException(final ErrorCode errorCode, final String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public ImagePreprocessingException(
      final ErrorCode errorCode, final String message, final Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }
}
