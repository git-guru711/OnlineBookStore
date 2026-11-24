package com.example.bookstore.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Inventory;
import com.example.bookstore.model.StockStatus;
import com.example.bookstore.support.RepositoryTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@RepositoryTest
class InventoryRepositoryTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void loadsInventoryForBook() {
        Book book = bookRepository.saveAndFlush(
                new Book("9780000000101", "Stocked Book", "Author", new BigDecimal("10.00")));
        inventoryRepository.saveAndFlush(new Inventory(book.getId(), 7));
        entityManager.clear();

        Inventory loaded = inventoryRepository.findById(book.getId()).orElseThrow();

        assertThat(loaded.getBookId()).isEqualTo(book.getId());
        assertThat(loaded.getAvailableQuantity()).isEqualTo(7);
        assertThat(loaded.status()).isEqualTo(StockStatus.IN_STOCK);
    }

    @Test
    void inventoryForUnknownBook_isRejected() {
        assertThatThrownBy(() -> inventoryRepository.saveAndFlush(new Inventory(999_999L, 5)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void negativeQuantity_isRejected() {
        Book book = bookRepository.saveAndFlush(
                new Book("9780000000102", "Negative Stock", "Author", new BigDecimal("10.00")));

        assertThatThrownBy(() -> inventoryRepository.saveAndFlush(new Inventory(book.getId(), -1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void decrementIfAvailable_enoughStock_updatesOneRow_decreasesQuantity_andBumpsVersion() {
        Book book =
                bookRepository.saveAndFlush(new Book("9780000000103", "Decrement", "Author", new BigDecimal("10.00")));
        inventoryRepository.saveAndFlush(new Inventory(book.getId(), 5));
        entityManager.clear();

        int updated = inventoryRepository.decrementIfAvailable(book.getId(), 5); // exactly the last copies
        entityManager.clear();

        assertThat(updated).isEqualTo(1);
        Inventory after = inventoryRepository.findById(book.getId()).orElseThrow();
        assertThat(after.getAvailableQuantity()).isZero();
        assertThat(after.getVersion()).isEqualTo(1L);
    }

    @Test
    void decrementIfAvailable_notEnoughStock_updatesNothing() {
        Book book =
                bookRepository.saveAndFlush(new Book("9780000000104", "Too Few", "Author", new BigDecimal("10.00")));
        inventoryRepository.saveAndFlush(new Inventory(book.getId(), 2));
        entityManager.clear();

        int updated = inventoryRepository.decrementIfAvailable(book.getId(), 3);
        entityManager.clear();

        assertThat(updated).isZero();
        assertThat(inventoryRepository.findById(book.getId()).orElseThrow().getAvailableQuantity())
                .isEqualTo(2);
    }
}
