package com.example.bill_manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record LineItem(
    @NotBlank
    String name,

    @NotNull
    @Positive
    BigDecimal quantity,

    @NotNull
    @PositiveOrZero
    BigDecimal unitPrice,

    @NotNull
    @PositiveOrZero
    BigDecimal totalPrice
) {}
