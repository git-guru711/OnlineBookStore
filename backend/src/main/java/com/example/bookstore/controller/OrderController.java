package com.example.bookstore.controller;

import com.example.bookstore.config.AuthenticatedUser;
import com.example.bookstore.model.CheckoutResult;
import com.example.bookstore.model.OrderResponse;
import com.example.bookstore.model.OrderSummaryResponse;
import com.example.bookstore.model.PageResponse;
import com.example.bookstore.service.IdempotentCheckout;
import com.example.bookstore.service.OrderService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final IdempotentCheckout idempotentCheckout;

    public OrderController(OrderService orderService, IdempotentCheckout idempotentCheckout) {
        this.orderService = orderService;
        this.idempotentCheckout = idempotentCheckout;
    }

    /** Builds the order from the server-side cart. A repeated Idempotency-Key returns the original order (200). */
    @PostMapping
    public ResponseEntity<OrderResponse> checkout(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(name = "Idempotency-Key", required = false)
                    @Pattern(
                            regexp = "[A-Za-z0-9-]{8,64}",
                            message = "must be 8-64 letters, digits or hyphens (e.g. a UUID)")
                    String idempotencyKey) {
        CheckoutResult result = idempotentCheckout.checkout(user.id(), idempotencyKey);
        OrderResponse order = result.order();
        if (!result.created()) {
            return ResponseEntity.ok(order);
        }
        return ResponseEntity.created(URI.create("/api/v1/orders/" + order.id()))
                .body(order);
    }

    @GetMapping
    public PageResponse<OrderSummaryResponse> listOrders(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return orderService.listOrders(user.id(), PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return orderService.getOrder(user.id(), id);
    }
}
