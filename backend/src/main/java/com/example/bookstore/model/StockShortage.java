package com.example.bookstore.model;

public record StockShortage(Long bookId, String title, int requested, int available) {}
