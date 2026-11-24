package com.example.bookstore.model;

import java.math.BigDecimal;

public record BookResponse(
        Long id,
        String isbn,
        String title,
        String author,
        BigDecimal price,
        int availableQuantity,
        StockStatus stockStatus) {}
