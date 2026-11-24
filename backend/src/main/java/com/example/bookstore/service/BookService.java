package com.example.bookstore.service;

import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.model.BookResponse;
import com.example.bookstore.model.BookView;
import com.example.bookstore.model.PageResponse;
import com.example.bookstore.model.StockStatus;
import com.example.bookstore.repository.BookRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public PageResponse<BookResponse> list(Pageable pageable) {
        return PageResponse.from(bookRepository.findAllWithStock(pageable), BookService::toResponse);
    }

    public PageResponse<BookResponse> search(String text, Pageable pageable) {
        return PageResponse.from(bookRepository.searchWithStock(text, pageable), BookService::toResponse);
    }

    public BookResponse findById(Long id) {
        return bookRepository
                .findWithStockById(id)
                .map(BookService::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Book %d not found".formatted(id)));
    }

    private static BookResponse toResponse(BookView view) {
        return new BookResponse(
                view.id(),
                view.isbn(),
                view.title(),
                view.author(),
                view.price(),
                view.availableQuantity(),
                StockStatus.forQuantity(view.availableQuantity()));
    }
}
