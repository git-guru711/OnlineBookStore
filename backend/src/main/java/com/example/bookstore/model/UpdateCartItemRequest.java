package com.example.bookstore.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** To remove a line, use DELETE rather than quantity 0. */
public record UpdateCartItemRequest(
        @NotNull @Min(1) @Max(99) Integer quantity) {}
