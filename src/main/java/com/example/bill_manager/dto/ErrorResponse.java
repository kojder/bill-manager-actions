package com.example.bill_manager.dto;

import java.time.Instant;

public record ErrorResponse(String code, String message, Instant timestamp) {}
