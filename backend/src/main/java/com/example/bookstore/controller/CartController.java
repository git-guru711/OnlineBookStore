package com.example.bookstore.controller;

import com.example.bookstore.config.AuthenticatedUser;
import com.example.bookstore.model.AddCartItemRequest;
import com.example.bookstore.model.AddToCartResult;
import com.example.bookstore.model.CartResponse;
import com.example.bookstore.model.UpdateCartItemRequest;
import com.example.bookstore.service.CartService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** No cart id in the URL: users can only reach their own cart. */
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal AuthenticatedUser user) {
        return cartService.getCart(user.id());
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody AddCartItemRequest request) {
        AddToCartResult result = cartService.addItem(user.id(), request.bookId(), request.quantity());
        return result.newLine()
                ? ResponseEntity.created(URI.create("/api/v1/cart")).body(result.cart())
                : ResponseEntity.ok(result.cart());
    }

    @PatchMapping("/items/{bookId}")
    public CartResponse changeQuantity(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long bookId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.changeQuantity(user.id(), bookId, request.quantity());
    }

    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<Void> removeItem(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long bookId) {
        cartService.removeItem(user.id(), bookId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(@AuthenticationPrincipal AuthenticatedUser user) {
        cartService.clear(user.id());
        return ResponseEntity.noContent().build();
    }
}
