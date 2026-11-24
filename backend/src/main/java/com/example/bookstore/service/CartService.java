package com.example.bookstore.service;

import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Cart;
import com.example.bookstore.entity.CartItem;
import com.example.bookstore.exception.ConcurrentRequestException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.model.AddToCartResult;
import com.example.bookstore.model.CartItemResponse;
import com.example.bookstore.model.CartResponse;
import com.example.bookstore.model.StockStatus;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.CartRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Stock is checked but never reserved; it only decreases at checkout. */
@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final BookRepository bookRepository;
    private final InventoryService inventoryService;

    public CartService(
            CartRepository cartRepository, BookRepository bookRepository, InventoryService inventoryService) {
        this.cartRepository = cartRepository;
        this.bookRepository = bookRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        return cartRepository
                .findByUserIdWithItems(userId)
                .map(this::toResponse)
                .orElseGet(CartService::emptyCart);
    }

    public AddToCartResult addItem(Long userId, Long bookId, int quantity) {
        Book book = bookRepository
                .findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book %d not found".formatted(bookId)));
        Cart cart = cartRepository.findByUserIdWithItems(userId).orElseGet(() -> new Cart(userId));
        boolean newLine = cart.quantityOf(bookId) == 0;

        inventoryService.requireAvailable(book, cart.quantityOf(bookId) + quantity);
        cart.addItem(book, quantity);
        return new AddToCartResult(toResponse(saveNow(cart)), newLine);
    }

    public CartResponse changeQuantity(Long userId, Long bookId, int quantity) {
        Cart cart = requireCartContaining(userId, bookId);
        inventoryService.requireAvailable(cart.findItem(bookId).orElseThrow().getBook(), quantity);
        cart.changeQuantity(bookId, quantity);
        return toResponse(cart);
    }

    public void removeItem(Long userId, Long bookId) {
        requireCartContaining(userId, bookId).removeItem(bookId);
    }

    /** Idempotent: clearing an empty or missing cart succeeds. */
    public void clear(Long userId) {
        cartRepository.findByUserIdWithItems(userId).ifPresent(Cart::clear);
    }

    // Flush now: a unique-key violation means a concurrent request created the same cart or line first (409, not 500).
    private Cart saveNow(Cart cart) {
        try {
            return cartRepository.saveAndFlush(cart);
        } catch (DataIntegrityViolationException concurrentInsert) {
            throw new ConcurrentRequestException();
        }
    }

    private Cart requireCartContaining(Long userId, Long bookId) {
        return cartRepository
                .findByUserIdWithItems(userId)
                .filter(cart -> cart.findItem(bookId).isPresent())
                .orElseThrow(() -> new ResourceNotFoundException("Book %d is not in the cart".formatted(bookId)));
    }

    private CartResponse toResponse(Cart cart) {
        List<Long> bookIds =
                cart.getItems().stream().map(item -> item.getBook().getId()).toList();
        Map<Long, Integer> available = inventoryService.availableQuantities(bookIds);
        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> toItemResponse(
                        item, available.getOrDefault(item.getBook().getId(), 0)))
                .toList();
        boolean checkoutAllowed = !items.isEmpty() && items.stream().noneMatch(CartItemResponse::exceedsAvailable);
        return new CartResponse(items, cart.totalQuantity(), cart.total(), checkoutAllowed);
    }

    private static CartItemResponse toItemResponse(CartItem item, int available) {
        Book book = item.getBook();
        return new CartItemResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPrice(),
                item.getQuantity(),
                item.lineTotal(),
                available,
                StockStatus.forQuantity(available),
                item.getQuantity() > available);
    }

    private static CartResponse emptyCart() {
        return new CartResponse(List.of(), 0, BigDecimal.ZERO, false);
    }
}
