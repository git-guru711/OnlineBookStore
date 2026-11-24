package com.example.bookstore.model;

/** created is false when a repeated Idempotency-Key returned an existing order (200 instead of 201). */
public record CheckoutResult(OrderResponse order, boolean created) {

    public static CheckoutResult created(OrderResponse order) {
        return new CheckoutResult(order, true);
    }

    public static CheckoutResult alreadyPlaced(OrderResponse order) {
        return new CheckoutResult(order, false);
    }
}
