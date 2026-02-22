package com.example.bill_manager.ai;

import com.example.bill_manager.dto.BillAnalysisResult;
import java.util.List;

public interface BillAnalysisService {

  BillAnalysisResult analyze(List<byte[]> images, String mimeType);
}
