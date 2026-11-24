package com.example.bookstore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bookstore.entity.Book;
import com.example.bookstore.exception.InsufficientStockException;
import com.example.bookstore.model.StockShortage;
import com.example.bookstore.support.TestCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

@SpringBootTest
@Import(TestCatalog.class)
class CheckoutRollbackIT {

    @Autowired
    private TestCatalog catalog;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Test
    void oneShortLine_rollsBackTheWholeCheckout() {
        Book plenty = catalog.book("Plenty In Stock", "20.00", 5);
        Book lastCopy = catalog.book("Last Copy", "15.00", 1);
        long buyer = catalog.user("rollback.buyer");
        long rival = catalog.user("rollback.rival");
        cartService.addItem(buyer, plenty.getId(), 2);
        cartService.addItem(buyer, lastCopy.getId(), 1);
        cartService.addItem(rival, lastCopy.getId(), 1);

        orderService.checkout(rival); // takes the last copy first

        assertThatThrownBy(() -> orderService.checkout(buyer))
                .isInstanceOfSatisfying(
                        InsufficientStockException.class,
                        ex -> assertThat(ex.getShortages())
                                .containsExactly(new StockShortage(lastCopy.getId(), "Last Copy", 1, 0)));

        // the decrease for "Plenty In Stock" happened in the failed transaction and was rolled back
        assertThat(catalog.stockOf(plenty)).isEqualTo(5);
        assertThat(catalog.stockOf(lastCopy)).isZero();
        assertThat(cartService.getCart(buyer).items()).hasSize(2);
        assertThat(orderService.listOrders(buyer, PageRequest.of(0, 20)).totalElements())
                .isZero();
    }
}
