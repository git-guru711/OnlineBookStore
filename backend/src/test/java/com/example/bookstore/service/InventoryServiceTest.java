package com.example.bookstore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Inventory;
import com.example.bookstore.exception.InsufficientStockException;
import com.example.bookstore.model.StockRequest;
import com.example.bookstore.model.StockShortage;
import com.example.bookstore.repository.InventoryRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void availableQuantities_returnsStockPerBook_missingRowCountsAsZero() {
        given(inventoryRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(new Inventory(1L, 15)));

        assertThat(inventoryService.availableQuantities(List.of(1L, 2L)))
                .containsEntry(1L, 15)
                .containsEntry(2L, 0);
    }

    @Test
    void requireAvailable_enoughStock_passes() {
        given(inventoryRepository.findById(1L)).willReturn(Optional.of(new Inventory(1L, 5)));

        assertThatCode(() -> inventoryService.requireAvailable(book(1L, "Clean Code"), 5))
                .doesNotThrowAnyException();
    }

    @Test
    void requireAvailable_tooFew_throwsWithShortageDetails() {
        given(inventoryRepository.findById(4L)).willReturn(Optional.of(new Inventory(4L, 1)));

        assertThatThrownBy(() -> inventoryService.requireAvailable(book(4L, "Domain-Driven Design"), 2))
                .isInstanceOfSatisfying(
                        InsufficientStockException.class,
                        ex -> assertThat(ex.getShortages())
                                .containsExactly(new StockShortage(4L, "Domain-Driven Design", 2, 1)));
    }

    @Test
    void requireAvailable_noInventoryRow_isTreatedAsSoldOut() {
        given(inventoryRepository.findById(9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.requireAvailable(book(9L, "Unstocked"), 1))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void decreaseStock_enoughForEveryLine_decreasesEachOne() {
        given(inventoryRepository.decrementIfAvailable(1L, 2)).willReturn(1);
        given(inventoryRepository.decrementIfAvailable(5L, 1)).willReturn(1);

        inventoryService.decreaseStock(
                List.of(new StockRequest(1L, "Clean Code", 2), new StockRequest(5L, "The Pragmatic Programmer", 1)));

        verify(inventoryRepository).decrementIfAvailable(1L, 2);
        verify(inventoryRepository).decrementIfAvailable(5L, 1);
    }

    @Test
    void decreaseStock_locksBooksInAscendingIdOrder_whateverTheCartOrder() {
        given(inventoryRepository.decrementIfAvailable(anyLong(), anyInt())).willReturn(1);

        inventoryService.decreaseStock(List.of(
                new StockRequest(9L, "Nine", 1), new StockRequest(2L, "Two", 1), new StockRequest(5L, "Five", 1)));

        // a consistent lock order means two checkouts can never wait on each other in a cycle
        InOrder lockOrder = inOrder(inventoryRepository);
        lockOrder.verify(inventoryRepository).decrementIfAvailable(2L, 1);
        lockOrder.verify(inventoryRepository).decrementIfAvailable(5L, 1);
        lockOrder.verify(inventoryRepository).decrementIfAvailable(9L, 1);
    }

    @Test
    void decreaseStock_anyShortLine_throwsListingEveryShortLineWithCurrentStock() {
        given(inventoryRepository.decrementIfAvailable(1L, 2)).willReturn(1);
        given(inventoryRepository.decrementIfAvailable(4L, 2)).willReturn(0);
        given(inventoryRepository.decrementIfAvailable(3L, 1)).willReturn(0);
        given(inventoryRepository.findById(4L)).willReturn(Optional.of(new Inventory(4L, 1)));
        given(inventoryRepository.findById(3L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.decreaseStock(List.of(
                        new StockRequest(1L, "Clean Code", 2),
                        new StockRequest(4L, "Domain-Driven Design", 2),
                        new StockRequest(3L, "Refactoring", 1))))
                .isInstanceOfSatisfying(
                        InsufficientStockException.class,
                        ex -> assertThat(ex.getShortages())
                                .containsExactlyInAnyOrder(
                                        new StockShortage(4L, "Domain-Driven Design", 2, 1),
                                        new StockShortage(3L, "Refactoring", 1, 0)));
        // undone by the caller's transaction rolling back
    }

    static Book book(long id, String title) {
        Book book = new Book("isbn-" + id, title, "Author", new BigDecimal("10.00"));
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }
}
