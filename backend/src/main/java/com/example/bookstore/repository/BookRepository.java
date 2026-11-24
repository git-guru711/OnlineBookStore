package com.example.bookstore.repository;

import com.example.bookstore.entity.Book;
import com.example.bookstore.model.BookView;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    /** Book joined with its stock in one query (no N+1). A missing inventory row counts as 0. */
    String SELECT_BOOK_VIEW = """
            SELECT new com.example.bookstore.model.BookView(
                       b.id, b.isbn, b.title, b.author, b.price, COALESCE(i.availableQuantity, 0))
              FROM Book b
              LEFT JOIN Inventory i ON i.bookId = b.id
            """;

    String MATCHES_PATTERN = """
             WHERE LOWER(b.title) LIKE :pattern ESCAPE '\\'
                OR LOWER(b.author) LIKE :pattern ESCAPE '\\'
            """;

    @Query(value = SELECT_BOOK_VIEW, countQuery = "SELECT COUNT(b) FROM Book b")
    Page<BookView> findAllWithStock(Pageable pageable);

    @Query(SELECT_BOOK_VIEW + " WHERE b.id = :id")
    Optional<BookView> findWithStockById(@Param("id") Long id);

    @Query(value = SELECT_BOOK_VIEW + MATCHES_PATTERN, countQuery = "SELECT COUNT(b) FROM Book b" + MATCHES_PATTERN)
    Page<BookView> findWithStockMatching(@Param("pattern") String pattern, Pageable pageable);

    /** Case-insensitive title/author search. LIKE wildcards in the text are escaped, so it matches literally. */
    default Page<BookView> searchWithStock(String text, Pageable pageable) {
        String literal = text.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return findWithStockMatching("%" + literal + "%", pageable);
    }
}
