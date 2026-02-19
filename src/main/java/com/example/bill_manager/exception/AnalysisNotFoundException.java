package com.example.bill_manager.exception;

import java.util.UUID;
import lombok.Getter;

@Getter
public class AnalysisNotFoundException extends RuntimeException {

  private final UUID analysisId;

  public AnalysisNotFoundException(final UUID analysisId) {
    super("Analysis not found with ID: " + analysisId);
    this.analysisId = analysisId;
  }
}
