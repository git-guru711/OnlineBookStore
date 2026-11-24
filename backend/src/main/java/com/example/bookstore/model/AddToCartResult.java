package com.example.bookstore.model;

/** newLine: 201 (new line) or 200 (quantity merged). */
public record AddToCartResult(CartResponse cart, boolean newLine) {}
