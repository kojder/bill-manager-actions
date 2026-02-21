package com.example.bill_manager.upload;

import com.example.bill_manager.dto.BillAnalysisResponse;
import java.util.Optional;
import java.util.UUID;

public interface BillResultStore {

  void save(UUID id, BillAnalysisResponse response);

  Optional<BillAnalysisResponse> findById(UUID id);
}
