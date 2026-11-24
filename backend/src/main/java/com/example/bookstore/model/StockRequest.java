package com.example.bookstore.model;

/** The title is only used in error messages. */
public record StockRequest(Long bookId, String title, int quantity) {}
