package com.example.bookstore.service;

import com.example.bookstore.exception.ConcurrentRequestException;
import com.example.bookstore.exception.EmptyCartException;
import com.example.bookstore.model.CheckoutResult;
import java.util.Optional;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Checkout that is safe to repeat: a repeated Idempotency-Key returns the first order instead of ordering again.
 * Not @Transactional on purpose: a losing duplicate's transaction must roll back before the winner's order is read.
 */
@Service
public class IdempotentCheckout {

    private final OrderService orderService;

    public IdempotentCheckout(OrderService orderService) {
        this.orderService = orderService;
    }

    public CheckoutResult checkout(Long userId, String idempotencyKey) {
        if (idempotencyKey == null) {
            return CheckoutResult.created(orderService.checkout(userId));
        }
        Optional<CheckoutResult> earlier =
                orderService.findByIdempotencyKey(userId, idempotencyKey).map(CheckoutResult::alreadyPlaced);
        if (earlier.isPresent()) {
            return earlier.get();
        }
        try {
            return CheckoutResult.created(orderService.checkout(userId, idempotencyKey));
        } catch (DataIntegrityViolationException
                | ConcurrencyFailureException
                | ConcurrentRequestException
                | EmptyCartException lost) {
            // A request with the same key may have just placed the order; otherwise it is a real failure.
            return orderService
                    .findByIdempotencyKey(userId, idempotencyKey)
                    .map(CheckoutResult::alreadyPlaced)
                    .orElseThrow(() -> lost);
        }
    }
}
