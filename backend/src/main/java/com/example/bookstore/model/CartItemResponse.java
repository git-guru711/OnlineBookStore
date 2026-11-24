package com.example.bookstore.model;

import java.math.BigDecimal;

public record CartItemResponse(
        Long bookId,
        String title,
        String author,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        int availableQuantity,
        StockStatus stockStatus,
        boolean exceedsAvailable) {}
