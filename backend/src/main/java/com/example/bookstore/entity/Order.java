package com.example.bookstore.entity;

import com.example.bookstore.exception.EmptyCartException;
import com.example.bookstore.model.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;

/** Titles and prices are copied at checkout, so an order never changes with the catalog. */
@Entity
@Table(name = "orders") // "order" is a reserved word in SQL
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal totalAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Null when the client sent no Idempotency-Key. */
    @Column(name = "idempotency_key", length = 64, updatable = false)
    private String idempotencyKey;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {}

    private Order(Long userId) {
        this.userId = userId;
        this.status = OrderStatus.PLACED;
    }

    public static Order placeFrom(Cart cart) {
        return placeFrom(cart, null);
    }

    public static Order placeFrom(Cart cart, String idempotencyKey) {
        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException();
        }
        Order order = new Order(cart.getUserId());
        for (CartItem line : cart.getItems()) {
            Book book = line.getBook();
            order.items.add(new OrderItem(order, book.getId(), book.getTitle(), book.getPrice(), line.getQuantity()));
        }
        order.totalAmount = cart.total();
        order.idempotencyKey = idempotencyKey;
        return order;
    }

    public int totalQuantity() {
        return items.stream().mapToInt(OrderItem::getQuantity).sum();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
