package com.example.bookstore.model;

import java.math.BigDecimal;

public record BookView(Long id, String isbn, String title, String author, BigDecimal price, int availableQuantity) {}
