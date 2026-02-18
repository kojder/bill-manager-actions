package com.example.bill_manager.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record BillAnalysisResponse(
    @NotBlank
    String id,

    @NotBlank
    String originalFileName,

    @NotNull
    @Valid
    BillAnalysisResult analysis,

    @NotNull
    LocalDateTime analyzedAt
) {}
