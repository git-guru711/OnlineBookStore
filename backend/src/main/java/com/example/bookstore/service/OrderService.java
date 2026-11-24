package com.example.bookstore.service;

import com.example.bookstore.entity.Cart;
import com.example.bookstore.entity.Order;
import com.example.bookstore.entity.OrderItem;
import com.example.bookstore.exception.EmptyCartException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.model.OrderItemResponse;
import com.example.bookstore.model.OrderResponse;
import com.example.bookstore.model.OrderSummaryResponse;
import com.example.bookstore.model.PageResponse;
import com.example.bookstore.model.StockRequest;
import com.example.bookstore.repository.CartRepository;
import com.example.bookstore.repository.OrderRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    public OrderService(
            CartRepository cartRepository, OrderRepository orderRepository, InventoryService inventoryService) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
    }

    /** One transaction: decrease stock, save the order with copied prices, empty the cart. */
    @Transactional
    public OrderResponse checkout(Long userId) {
        return checkout(userId, null);
    }

    /** The unique (user_id, idempotency_key) constraint makes a second order with the same key fail. */
    @Transactional
    public OrderResponse checkout(Long userId, String idempotencyKey) {
        Cart cart = cartRepository.findByUserIdWithItems(userId).orElseThrow(EmptyCartException::new);
        Order order = Order.placeFrom(cart, idempotencyKey);

        inventoryService.decreaseStock(order.getItems().stream()
                .map(item -> new StockRequest(item.getBookId(), item.getTitle(), item.getQuantity()))
                .toList());
        Order saved = orderRepository.saveAndFlush(order);
        cart.clear();
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Optional<OrderResponse> findByIdempotencyKey(Long userId, String idempotencyKey) {
        return orderRepository
                .findByUserIdAndIdempotencyKeyWithItems(userId, idempotencyKey)
                .map(OrderService::toResponse);
    }

    public OrderResponse getOrder(Long userId, Long orderId) {
        return orderRepository
                .findByIdAndUserIdWithItems(orderId, userId)
                .map(OrderService::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Order %d not found".formatted(orderId)));
    }

    public PageResponse<OrderSummaryResponse> listOrders(Long userId, Pageable pageable) {
        return PageResponse.from(orderRepository.findSummariesByUserId(userId, pageable), summary -> summary);
    }

    private static OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items =
                order.getItems().stream().map(OrderService::toItemResponse).toList();
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                items,
                order.totalQuantity(),
                order.getTotalAmount(),
                order.getCreatedAt());
    }

    private static OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getBookId(), item.getTitle(), item.getUnitPrice(), item.getQuantity(), item.lineTotal());
    }
}
