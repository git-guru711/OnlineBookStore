package com.example.bookstore.service;

import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Inventory;
import com.example.bookstore.exception.InsufficientStockException;
import com.example.bookstore.model.StockRequest;
import com.example.bookstore.model.StockShortage;
import com.example.bookstore.repository.InventoryRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The only place for stock rules; other code never uses the inventory repository directly. */
@Service
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public Map<Long, Integer> availableQuantities(Collection<Long> bookIds) {
        Map<Long, Integer> found = inventoryRepository.findAllById(bookIds).stream()
                .collect(Collectors.toMap(Inventory::getBookId, Inventory::getAvailableQuantity));
        return bookIds.stream()
                .distinct()
                .collect(Collectors.toMap(Function.identity(), id -> found.getOrDefault(id, 0)));
    }

    /**
     * Decreases stock atomically per line. Rows are locked in book-id order, so two checkouts cannot deadlock.
     * Any shortage throws, rolling back the caller's transaction (must run inside one).
     */
    @Transactional
    public void decreaseStock(List<StockRequest> lines) {
        List<StockShortage> shortages = new ArrayList<>();
        for (StockRequest line : inLockOrder(lines)) {
            if (inventoryRepository.decrementIfAvailable(line.bookId(), line.quantity()) == 0) {
                int available = inventoryRepository
                        .findById(line.bookId())
                        .map(Inventory::getAvailableQuantity)
                        .orElse(0);
                shortages.add(new StockShortage(line.bookId(), line.title(), line.quantity(), available));
            }
        }
        if (!shortages.isEmpty()) {
            throw new InsufficientStockException(shortages);
        }
    }

    private static List<StockRequest> inLockOrder(List<StockRequest> lines) {
        return lines.stream().sorted(Comparator.comparing(StockRequest::bookId)).toList();
    }

    public void requireAvailable(Book book, int requestedQuantity) {
        int available = inventoryRepository
                .findById(book.getId())
                .map(Inventory::getAvailableQuantity)
                .orElse(0);
        if (requestedQuantity > available) {
            throw new InsufficientStockException(
                    List.of(new StockShortage(book.getId(), book.getTitle(), requestedQuantity, available)));
        }
    }
}
