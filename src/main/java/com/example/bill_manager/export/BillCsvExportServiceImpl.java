package com.example.bill_manager.export;

import com.example.bill_manager.dto.BillAnalysisResponse;
import com.example.bill_manager.dto.LineItem;
import com.example.bill_manager.dto.PurchaseCategory;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class BillCsvExportServiceImpl implements BillCsvExportService {

  private static final char UTF8_BOM = '\uFEFF';
  private static final String DELIMITER = ",";
  private static final String NEWLINE = "\r\n";

  @Override
  public String exportToCsv(final BillAnalysisResponse response) {
    if (response == null) {
      throw new IllegalArgumentException("Response must not be null");
    }
    final StringBuilder sb = new StringBuilder();
    sb.append(UTF8_BOM);
    appendHeaderSection(sb, response);
    sb.append(NEWLINE);
    appendItemsSection(sb, response.analysis().items());
    appendCategoriesSection(sb, response.analysis().categoryTags());
    return sb.toString();
  }

  private void appendHeaderSection(final StringBuilder sb, final BillAnalysisResponse response) {
    sb.append("Merchant")
        .append(DELIMITER)
        .append("File")
        .append(DELIMITER)
        .append("Date")
        .append(DELIMITER)
        .append("Currency")
        .append(DELIMITER)
        .append("Total")
        .append(NEWLINE);
    sb.append(quote(response.analysis().merchantName()))
        .append(DELIMITER)
        .append(quote(response.originalFileName()))
        .append(DELIMITER)
        .append(quote(Objects.toString(response.analyzedAt(), "")))
        .append(DELIMITER)
        .append(quote(response.analysis().currency()))
        .append(DELIMITER)
        .append(
            quote(
                response.analysis().totalAmount() != null
                    ? response.analysis().totalAmount().toPlainString()
                    : ""))
        .append(NEWLINE);
  }

  private void appendItemsSection(final StringBuilder sb, final List<LineItem> items) {
    sb.append("Item Name")
        .append(DELIMITER)
        .append("Quantity")
        .append(DELIMITER)
        .append("Unit Price")
        .append(DELIMITER)
        .append("Total Price")
        .append(NEWLINE);
    for (final LineItem item : items) {
      sb.append(quote(item.name()))
          .append(DELIMITER)
          .append(quote(item.quantity().toPlainString()))
          .append(DELIMITER)
          .append(quote(item.unitPrice().toPlainString()))
          .append(DELIMITER)
          .append(quote(item.totalPrice().toPlainString()))
          .append(NEWLINE);
    }
  }

  private void appendCategoriesSection(
      final StringBuilder sb, final List<PurchaseCategory> categories) {
    if (categories == null || categories.isEmpty()) {
      return;
    }
    sb.append(NEWLINE).append("Categories").append(NEWLINE);
    for (int i = 0; i < categories.size(); i++) {
      if (i > 0) {
        sb.append(DELIMITER);
      }
      sb.append(quote(categories.get(i).name()));
    }
    sb.append(NEWLINE);
  }

  private String quote(final String value) {
    if (value == null) {
      return "\"\"";
    }
    final String safe =
        (value.startsWith("=")
                || value.startsWith("+")
                || value.startsWith("-")
                || value.startsWith("@"))
            ? "'" + value
            : value;
    return "\"" + safe.replace("\"", "\"\"") + "\"";
  }
}
