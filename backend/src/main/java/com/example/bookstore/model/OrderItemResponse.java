package com.example.bookstore.model;

import java.math.BigDecimal;

public record OrderItemResponse(Long bookId, String title, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {}
