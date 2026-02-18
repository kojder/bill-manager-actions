package com.example.bill_manager.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
    String code,
    String message,
    LocalDateTime timestamp
) {}
