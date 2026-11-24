package com.example.bookstore.controller;

import com.example.bookstore.model.BookResponse;
import com.example.bookstore.model.PageResponse;
import com.example.bookstore.model.SortParameter;
import com.example.bookstore.service.BookService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private static final Set<String> SORTABLE_FIELDS = Set.of("title", "author", "price");

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public PageResponse<BookResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "title,asc") String sort,
            @RequestParam(required = false) @Size(max = 100) String q) {
        PageRequest pageable = PageRequest.of(page, size, SortParameter.parse(sort, SORTABLE_FIELDS));
        if (q == null || q.isBlank()) {
            return bookService.list(pageable);
        }
        return bookService.search(q.strip(), pageable);
    }

    @GetMapping("/{id}")
    public BookResponse getById(@PathVariable Long id) {
        return bookService.findById(id);
    }
}
