package com.example.bookstore.entity;

import com.example.bookstore.exception.InvalidQuantityException;
import com.example.bookstore.exception.ResourceNotFoundException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** All changes go through these methods, so the quantity rules cannot be bypassed. */
@Entity
@Table(name = "carts")
public class Cart {

    public static final int MIN_QUANTITY = 1;
    public static final int MAX_QUANTITY = 99;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    /** Optimistic locking: two concurrent edits of the same cart cannot silently overwrite each other. */
    @Version
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<CartItem> items = new ArrayList<>();

    protected Cart() {}

    public Cart(Long userId) {
        this.userId = userId;
        this.updatedAt = Instant.now();
    }

    public void addItem(Book book, int quantity) {
        requireValidQuantity(quantity, "");
        Optional<CartItem> existing = findItem(book.getId());
        if (existing.isPresent()) {
            int merged = existing.get().getQuantity() + quantity;
            requireValidQuantity(merged, " (this would make %d)".formatted(merged));
            existing.get().setQuantity(merged);
        } else {
            items.add(new CartItem(this, book, quantity));
        }
        touch();
    }

    public void changeQuantity(Long bookId, int quantity) {
        CartItem item = requireItem(bookId);
        requireValidQuantity(quantity, "");
        item.setQuantity(quantity);
        touch();
    }

    public void removeItem(Long bookId) {
        items.remove(requireItem(bookId));
        touch();
    }

    public void clear() {
        items.clear();
        touch();
    }

    public int quantityOf(Long bookId) {
        return findItem(bookId).map(CartItem::getQuantity).orElse(0);
    }

    public int totalQuantity() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public BigDecimal total() {
        return items.stream().map(CartItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getVersion() {
        return version;
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    // Item changes alone do not dirty the cart row; touching the timestamp makes every change bump the version.
    private void touch() {
        updatedAt = Instant.now();
    }

    private CartItem requireItem(Long bookId) {
        return findItem(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book %d is not in the cart".formatted(bookId)));
    }

    public Optional<CartItem> findItem(Long bookId) {
        return items.stream()
                .filter(item -> item.getBook().getId().equals(bookId))
                .findFirst();
    }

    private static void requireValidQuantity(int quantity, String detail) {
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new InvalidQuantityException(
                    "Quantity must be between %d and %d%s".formatted(MIN_QUANTITY, MAX_QUANTITY, detail));
        }
    }
}
