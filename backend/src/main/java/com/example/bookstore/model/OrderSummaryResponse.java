package com.example.bookstore.model;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummaryResponse(
        Long id, OrderStatus status, int totalQuantity, BigDecimal totalAmount, Instant createdAt) {}
