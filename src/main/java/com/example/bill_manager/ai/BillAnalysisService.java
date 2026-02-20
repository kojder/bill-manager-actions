package com.example.bill_manager.ai;

import com.example.bill_manager.dto.BillAnalysisResult;

public interface BillAnalysisService {

  BillAnalysisResult analyze(byte[] imageData, String mimeType);
}
