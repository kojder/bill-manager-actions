package com.example.bill_manager.ai;

import lombok.Getter;

@Getter
public class BillAnalysisException extends RuntimeException {

  public enum ErrorCode {
    PROMPT_TOO_LARGE,
    ANALYSIS_FAILED,
    INVALID_RESPONSE,
    SERVICE_UNAVAILABLE
  }

  private final ErrorCode errorCode;

  public BillAnalysisException(final ErrorCode errorCode, final String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public BillAnalysisException(
      final ErrorCode errorCode, final String message, final Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }
}
