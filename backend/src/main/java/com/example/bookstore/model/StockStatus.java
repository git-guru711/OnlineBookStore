package com.example.bookstore.model;

public enum StockStatus {
    IN_STOCK,
    OUT_OF_STOCK;

    public static StockStatus forQuantity(int availableQuantity) {
        return availableQuantity > 0 ? IN_STOCK : OUT_OF_STOCK;
    }
}
