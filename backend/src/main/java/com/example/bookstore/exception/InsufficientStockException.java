package com.example.bookstore.exception;

import com.example.bookstore.model.StockShortage;
import java.util.List;

public class InsufficientStockException extends RuntimeException {

    private final List<StockShortage> shortages;

    public InsufficientStockException(List<StockShortage> shortages) {
        super("Some items are not available in the requested quantity");
        this.shortages = List.copyOf(shortages);
    }

    public List<StockShortage> getShortages() {
        return shortages;
    }
}
