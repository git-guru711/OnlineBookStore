package com.example.bookstore.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        OrderStatus status,
        List<OrderItemResponse> items,
        int totalQuantity,
        BigDecimal totalAmount,
        Instant createdAt) {}
