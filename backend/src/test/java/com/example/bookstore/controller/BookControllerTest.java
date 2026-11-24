package com.example.bookstore.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bookstore.config.SecurityConfig;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.model.BookResponse;
import com.example.bookstore.model.PageResponse;
import com.example.bookstore.model.StockStatus;
import com.example.bookstore.service.BookService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookController.class)
@Import(SecurityConfig.class) // the real security rules
class BookControllerTest {

    private static final String PROBLEMS = "https://bookstore.example/problems/";

    private static final BookResponse CLEAN_CODE = new BookResponse(
            1L, "9780132350884", "Clean Code", "Robert C. Martin", new BigDecimal("32.99"), 15, StockStatus.IN_STOCK);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @Test
    void list_withoutParams_usesDefaultPagingAndReturns200() throws Exception {
        given(bookService.list(PageRequest.of(0, 20, Sort.by("title"))))
                .willReturn(new PageResponse<>(List.of(CLEAN_CODE), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].isbn").value("9780132350884"))
                .andExpect(jsonPath("$.content[0].title").value("Clean Code"))
                .andExpect(jsonPath("$.content[0].author").value("Robert C. Martin"))
                .andExpect(jsonPath("$.content[0].price").value(32.99))
                .andExpect(jsonPath("$.content[0].availableQuantity").value(15))
                .andExpect(jsonPath("$.content[0].stockStatus").value("IN_STOCK"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void list_withPagingAndSortParams_passesThemToService() throws Exception {
        given(bookService.list(PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "price"))))
                .willReturn(new PageResponse<>(List.of(), 1, 5, 6, 2));

        mockMvc.perform(get("/api/v1/books")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "price,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void list_sortWithoutDirection_defaultsToAscending() throws Exception {
        given(bookService.list(PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "author"))))
                .willReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/books").param("sort", "author")).andExpect(status().isOk());
    }

    @Test
    void list_withQ_searchesTitleAndAuthorWithTheTrimmedText_keepingPagingAndSort() throws Exception {
        given(bookService.search("clean", PageRequest.of(0, 12, Sort.by("title"))))
                .willReturn(new PageResponse<>(List.of(CLEAN_CODE), 0, 12, 1, 1));

        mockMvc.perform(get("/api/v1/books").param("q", "  clean ").param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Clean Code"));
    }

    @Test
    void list_withBlankQ_listsTheWholeCatalog() throws Exception {
        given(bookService.list(PageRequest.of(0, 20, Sort.by("title"))))
                .willReturn(new PageResponse<>(List.of(CLEAN_CODE), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/books").param("q", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void list_qLongerThan100Characters_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("q", "a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("q"));
        verifyNoInteractions(bookService);
    }

    @Test
    void list_sizeAbove100_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("size"));
        verifyNoInteractions(bookService);
    }

    @Test
    void list_sizeZero_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"));
    }

    @Test
    void list_negativePage_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("page"));
    }

    @Test
    void list_unsupportedSortField_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("sort", "isbn,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("sort"));
        verifyNoInteractions(bookService);
    }

    @Test
    void list_sortWithMoreThanFieldAndDirection_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("sort", "title,asc,extra"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("sort"));
        verifyNoInteractions(bookService);
    }

    @Test
    void list_invalidSortDirection_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("sort", "title,sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("sort"));
    }

    @Test
    void getById_existingBook_returns200() throws Exception {
        given(bookService.findById(1L)).willReturn(CLEAN_CODE);

        mockMvc.perform(get("/api/v1/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.stockStatus").value("IN_STOCK"));
    }

    @Test
    void getById_unknownBook_returns404Problem() throws Exception {
        given(bookService.findById(99L)).willThrow(new ResourceNotFoundException("Book 99 not found"));

        mockMvc.perform(get("/api/v1/books/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "not-found"))
                .andExpect(jsonPath("$.detail").value("Book 99 not found"));
    }

    @Test
    void getById_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/books/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "malformed-request"));
    }
}
