package com.example.bill_manager.upload;

import com.example.bill_manager.dto.BillAnalysisResponse;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class BillResultStore {

  private final Map<UUID, BillAnalysisResponse> store = new ConcurrentHashMap<>();

  public void save(final UUID id, final BillAnalysisResponse response) {
    store.put(id, response);
  }

  public Optional<BillAnalysisResponse> findById(final UUID id) {
    return Optional.ofNullable(store.get(id));
  }
}
