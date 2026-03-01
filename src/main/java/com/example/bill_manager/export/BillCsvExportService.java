package com.example.bill_manager.export;

import com.example.bill_manager.dto.BillAnalysisResponse;

public interface BillCsvExportService {

  String exportToCsv(BillAnalysisResponse response);
}
