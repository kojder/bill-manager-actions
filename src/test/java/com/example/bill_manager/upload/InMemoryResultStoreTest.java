package com.example.bill_manager.upload;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bill_manager.dto.BillAnalysisResponse;
import com.example.bill_manager.dto.BillAnalysisResult;
import com.example.bill_manager.dto.LineItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InMemoryResultStoreTest {

  private InMemoryResultStore store;

  @BeforeEach
  void setUp() {
    store = new InMemoryResultStore();
  }

  private BillAnalysisResponse createResponse(final UUID id) {
    final BillAnalysisResult analysis =
        new BillAnalysisResult(
            "Store",
            List.of(new LineItem("Item", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN)),
            BigDecimal.TEN,
            "PLN",
            null);
    return new BillAnalysisResponse(id, "receipt.jpg", analysis, Instant.now());
  }

  @Nested
  class SaveAndRetrieve {

    @Test
    void shouldSaveAndRetrieveResponse() {
      final UUID id = UUID.randomUUID();
      final BillAnalysisResponse response = createResponse(id);

      store.save(id, response);
      final Optional<BillAnalysisResponse> result = store.findById(id);

      assertThat(result).isPresent();
      assertThat(result.get().id()).isEqualTo(id);
      assertThat(result.get().originalFileName()).isEqualTo("receipt.jpg");
    }

    @Test
    void shouldReturnEmptyForUnknownId() {
      final Optional<BillAnalysisResponse> result = store.findById(UUID.randomUUID());

      assertThat(result).isEmpty();
    }

    @Test
    void shouldOverwriteExistingEntry() {
      final UUID id = UUID.randomUUID();
      final BillAnalysisResponse first = createResponse(id);
      store.save(id, first);

      final BillAnalysisResponse updated =
          new BillAnalysisResponse(id, "updated.jpg", first.analysis(), Instant.now());
      store.save(id, updated);

      final Optional<BillAnalysisResponse> result = store.findById(id);
      assertThat(result).isPresent();
      assertThat(result.get().originalFileName()).isEqualTo("updated.jpg");
    }
  }
}
