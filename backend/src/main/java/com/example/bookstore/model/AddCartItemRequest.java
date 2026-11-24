package com.example.bookstore.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddCartItemRequest(
        @NotNull @Positive Long bookId,
        @NotNull @Min(1) @Max(99) Integer quantity) {}
