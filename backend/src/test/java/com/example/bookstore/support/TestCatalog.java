package com.example.bookstore.support;

import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Inventory;
import com.example.bookstore.model.RegisterRequest;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.InventoryRepository;
import com.example.bookstore.service.AuthService;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/** Books and users with unique ISBNs and emails, so tests that change stock never touch the shared demo catalog. */
@Component
public class TestCatalog {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private final BookRepository bookRepository;
    private final InventoryRepository inventoryRepository;
    private final AuthService authService;

    public TestCatalog(
            BookRepository bookRepository, InventoryRepository inventoryRepository, AuthService authService) {
        this.bookRepository = bookRepository;
        this.inventoryRepository = inventoryRepository;
        this.authService = authService;
    }

    public Book book(String title, String price, int stock) {
        int n = SEQUENCE.incrementAndGet();
        Book book =
                bookRepository.save(new Book("97898%08d".formatted(n), title, "Test Author", new BigDecimal(price)));
        inventoryRepository.save(new Inventory(book.getId(), stock));
        return book;
    }

    public long user(String name) {
        int n = SEQUENCE.incrementAndGet();
        return authService
                .register(new RegisterRequest("%s.%d@example.com".formatted(name, n), "secret123", name))
                .id();
    }

    public int stockOf(Book book) {
        return inventoryRepository.findById(book.getId()).orElseThrow().getAvailableQuantity();
    }
}
