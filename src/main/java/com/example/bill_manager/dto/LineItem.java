package com.example.bill_manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record LineItem(
    @NotBlank
    String name,

    @NotNull
    @Positive
    BigDecimal quantity,

    @NotNull
    @Positive
    BigDecimal unitPrice,

    @NotNull
    @Positive
    BigDecimal totalPrice
) {}
