package com.example.bookstore.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Inventory;
import com.example.bookstore.model.BookView;
import com.example.bookstore.support.RepositoryTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@RepositoryTest
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndLoadsBook() {
        Book saved = bookRepository.saveAndFlush(
                new Book("9780000000001", "Test Book", "Test Author", new BigDecimal("19.99")));
        entityManager.clear(); // force a real read from the database, not the JPA cache

        Book loaded = bookRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getId()).isNotNull();
        assertThat(loaded.getIsbn()).isEqualTo("9780000000001");
        assertThat(loaded.getTitle()).isEqualTo("Test Book");
        assertThat(loaded.getAuthor()).isEqualTo("Test Author");
        assertThat(loaded.getPrice()).isEqualByComparingTo("19.99");
    }

    @Test
    void duplicateIsbn_isRejected() {
        bookRepository.saveAndFlush(new Book("9780000000002", "First", "Author A", new BigDecimal("10.00")));

        assertThatThrownBy(() -> bookRepository.saveAndFlush(
                        new Book("9780000000002", "Second", "Author B", new BigDecimal("12.00"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void negativePrice_isRejected() {
        assertThatThrownBy(() -> bookRepository.saveAndFlush(
                        new Book("9780000000003", "Bad Price", "Author", new BigDecimal("-0.01"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findAllWithStock_returnsPageOfBooksWithQuantity_sortedByTitle() {
        saveBookWithStock("9780000000203", "Gamma", "15.00", 9);
        Book alpha = saveBookWithStock("9780000000201", "Alpha", "10.00", 3);
        saveBookWithStock("9780000000202", "Beta", "12.50", 0);
        entityManager.clear();

        Page<BookView> page = bookRepository.findAllWithStock(PageRequest.of(0, 2, Sort.by("title")));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent())
                .extracting(BookView::title, BookView::availableQuantity)
                .containsExactly(tuple("Alpha", 3), tuple("Beta", 0));
        BookView first = page.getContent().get(0);
        assertThat(first.id()).isEqualTo(alpha.getId());
        assertThat(first.isbn()).isEqualTo("9780000000201");
        assertThat(first.author()).isEqualTo("Author");
        assertThat(first.price()).isEqualByComparingTo("10.00");
    }

    @Test
    void findAllWithStock_bookWithoutInventoryRow_hasZeroQuantity() {
        bookRepository.saveAndFlush(new Book("9780000000204", "No Stock Row", "Author", new BigDecimal("9.00")));

        Page<BookView> page = bookRepository.findAllWithStock(PageRequest.of(0, 20));

        assertThat(page.getContent())
                .extracting(BookView::title, BookView::availableQuantity)
                .contains(tuple("No Stock Row", 0));
    }

    @Test
    void findWithStockById_returnsBookWithQuantity_orEmptyWhenUnknown() {
        Book book = saveBookWithStock("9780000000205", "Single", "11.00", 4);
        entityManager.clear();

        assertThat(bookRepository.findWithStockById(book.getId())).hasValueSatisfying(view -> {
            assertThat(view.title()).isEqualTo("Single");
            assertThat(view.availableQuantity()).isEqualTo(4);
        });
        assertThat(bookRepository.findWithStockById(999_999L)).isEmpty();
    }

    private Book saveBookWithStock(String isbn, String title, String price, int quantity) {
        Book book = bookRepository.saveAndFlush(new Book(isbn, title, "Author", new BigDecimal(price)));
        inventoryRepository.saveAndFlush(new Inventory(book.getId(), quantity));
        return book;
    }

    private void searchCatalog() {
        for (String[] row : new String[][] {
            {"9780000000501", "Clean Code", "Robert C. Martin", "32.99"},
            {"9780000000502", "Clean Architecture", "Robert C. Martin", "29.99"},
            {"9780000000503", "Refactoring", "Martin Fowler", "42.00"},
            {"9780000000504", "Domain-Driven Design", "Eric Evans", "49.95"}
        }) {
            Book book = bookRepository.save(new Book(row[0], row[1], row[2], new BigDecimal(row[3])));
            inventoryRepository.save(new Inventory(book.getId(), 5));
        }
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void search_findsBooksWhoseTitleContainsTheText_ignoringCase() {
        searchCatalog();

        Page<BookView> page = bookRepository.searchWithStock("clean", PageRequest.of(0, 20, Sort.by("title")));

        assertThat(page.getContent()).extracting(BookView::title).containsExactly("Clean Architecture", "Clean Code");
        assertThat(page.getContent())
                .allSatisfy(book -> assertThat(book.availableQuantity()).isEqualTo(5));
    }

    @Test
    void search_findsBooksWhoseAuthorContainsTheText() {
        searchCatalog();

        Page<BookView> page = bookRepository.searchWithStock("FOWLER", PageRequest.of(0, 20, Sort.by("title")));

        assertThat(page.getContent()).extracting(BookView::title).containsExactly("Refactoring");
    }

    @Test
    void search_matchesTitleOrAuthor_andPagesTheResults() {
        searchCatalog();

        // "martin" is in two authors' names (Robert C. Martin twice, Martin Fowler once)
        Page<BookView> first = bookRepository.searchWithStock("martin", PageRequest.of(0, 2, Sort.by("title")));

        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.getContent()).extracting(BookView::title).containsExactly("Clean Architecture", "Clean Code");
    }

    @Test
    void search_treatsLikeWildcardsAsPlainText_soPercentDoesNotMatchEverything() {
        searchCatalog();

        assertThat(bookRepository.searchWithStock("%", PageRequest.of(0, 20)).getTotalElements())
                .isZero();
        assertThat(bookRepository.searchWithStock("_", PageRequest.of(0, 20)).getTotalElements())
                .isZero();
    }
}
