package com.example.bill_manager.upload;

import com.example.bill_manager.dto.BillAnalysisResponse;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryResultStore implements BillResultStore {

  private final Map<UUID, BillAnalysisResponse> store = new ConcurrentHashMap<>();

  @Override
  public void save(final UUID id, final BillAnalysisResponse response) {
    Objects.requireNonNull(id, "ID must not be null");
    Objects.requireNonNull(response, "Response must not be null");
    store.put(id, response);
  }

  @Override
  public Optional<BillAnalysisResponse> findById(final UUID id) {
    Objects.requireNonNull(id, "ID must not be null");
    return Optional.ofNullable(store.get(id));
  }
}
