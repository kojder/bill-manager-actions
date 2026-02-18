package com.example.bill_manager.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record BillAnalysisResponse(
    @NotNull
    UUID id,

    @NotBlank
    String originalFileName,

    @NotNull
    @Valid
    BillAnalysisResult analysis,

    @NotNull
    LocalDateTime analyzedAt
) {}
