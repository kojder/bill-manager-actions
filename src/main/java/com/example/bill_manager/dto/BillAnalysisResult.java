package com.example.bill_manager.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record BillAnalysisResult(
    @NotBlank
    String merchantName,

    @NotEmpty
    @Valid
    List<LineItem> items,

    @NotNull
    BigDecimal totalAmount,

    @NotBlank
    String currency,

    List<String> categoryTags
) {}
