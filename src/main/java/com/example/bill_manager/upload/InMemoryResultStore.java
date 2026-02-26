package com.example.bill_manager.upload;

import com.example.bill_manager.dto.BillAnalysisResponse;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InMemoryResultStore implements BillResultStore {

  private static final Logger LOG = LoggerFactory.getLogger(InMemoryResultStore.class);

  // TODO: add eviction policy (TTL or max-size) to prevent unbounded growth under sustained traffic
  private final Map<UUID, BillAnalysisResponse> store = new ConcurrentHashMap<>();

  @Override
  public void save(final UUID id, final BillAnalysisResponse response) {
    Objects.requireNonNull(id, "ID must not be null");
    Objects.requireNonNull(response, "Response must not be null");
    store.put(id, response);
    LOG.debug("Result stored: id={}, storeSize={}", id, store.size());
  }

  @Override
  public Optional<BillAnalysisResponse> findById(final UUID id) {
    Objects.requireNonNull(id, "ID must not be null");
    final Optional<BillAnalysisResponse> result = Optional.ofNullable(store.get(id));
    if (result.isEmpty()) {
      LOG.debug("Result not found: id={}", id);
    }
    return result;
  }
}
