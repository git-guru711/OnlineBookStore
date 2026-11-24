package com.example.bookstore.entity;

import com.example.bookstore.model.StockStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Shares the book's primary key (1:1). */
@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    /** Bumped by every stock change, including the conditional update in InventoryRepository. */
    @Version
    private Long version;

    protected Inventory() {}

    public Inventory(Long bookId, int availableQuantity) {
        this.bookId = bookId;
        this.availableQuantity = availableQuantity;
    }

    public Long getBookId() {
        return bookId;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isInStock() {
        return status() == StockStatus.IN_STOCK;
    }

    public boolean canFulfil(int requestedQuantity) {
        return requestedQuantity <= availableQuantity;
    }

    public StockStatus status() {
        return StockStatus.forQuantity(availableQuantity);
    }
}
