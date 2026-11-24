package com.example.bookstore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.model.BookResponse;
import com.example.bookstore.model.BookView;
import com.example.bookstore.model.PageResponse;
import com.example.bookstore.model.StockStatus;
import com.example.bookstore.repository.BookRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    void list_mapsBooksToResponsesWithStockStatus() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("title"));
        given(bookRepository.findAllWithStock(pageable))
                .willReturn(new PageImpl<>(
                        List.of(
                                new BookView(
                                        1L,
                                        "9780132350884",
                                        "Clean Code",
                                        "Robert C. Martin",
                                        new BigDecimal("32.99"),
                                        15),
                                new BookView(
                                        3L,
                                        "9780134757599",
                                        "Refactoring",
                                        "Martin Fowler",
                                        new BigDecimal("42.00"),
                                        0)),
                        pageable,
                        2));

        PageResponse<BookResponse> result = bookService.list(pageable);

        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.content())
                .containsExactly(
                        new BookResponse(
                                1L,
                                "9780132350884",
                                "Clean Code",
                                "Robert C. Martin",
                                new BigDecimal("32.99"),
                                15,
                                StockStatus.IN_STOCK),
                        new BookResponse(
                                3L,
                                "9780134757599",
                                "Refactoring",
                                "Martin Fowler",
                                new BigDecimal("42.00"),
                                0,
                                StockStatus.OUT_OF_STOCK));
    }

    @Test
    void search_returnsTheMatchingBooksAsResponses() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("title"));
        given(bookRepository.searchWithStock("fowler", pageable))
                .willReturn(new PageImpl<>(
                        List.of(new BookView(
                                3L, "9780134757599", "Refactoring", "Martin Fowler", new BigDecimal("42.00"), 0)),
                        pageable,
                        1));

        PageResponse<BookResponse> result = bookService.search("fowler", pageable);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content())
                .containsExactly(new BookResponse(
                        3L,
                        "9780134757599",
                        "Refactoring",
                        "Martin Fowler",
                        new BigDecimal("42.00"),
                        0,
                        StockStatus.OUT_OF_STOCK));
    }

    @Test
    void findById_existingBook_returnsResponse() {
        given(bookRepository.findWithStockById(1L))
                .willReturn(Optional.of(new BookView(
                        1L, "9780132350884", "Clean Code", "Robert C. Martin", new BigDecimal("32.99"), 15)));

        BookResponse result = bookService.findById(1L);

        assertThat(result.title()).isEqualTo("Clean Code");
        assertThat(result.stockStatus()).isEqualTo(StockStatus.IN_STOCK);
    }

    @Test
    void findById_unknownId_throwsNotFound() {
        given(bookRepository.findWithStockById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Book 99 not found");
    }
}
