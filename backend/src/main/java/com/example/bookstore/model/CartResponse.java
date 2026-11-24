package com.example.bookstore.model;

import java.math.BigDecimal;
import java.util.List;

/** {@code checkoutAllowed} is false when the cart is empty or any line exceeds the available stock. */
public record CartResponse(
        List<CartItemResponse> items, int totalQuantity, BigDecimal subtotal, boolean checkoutAllowed) {}
