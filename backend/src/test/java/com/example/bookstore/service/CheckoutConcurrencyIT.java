package com.example.bookstore.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bookstore.entity.Book;
import com.example.bookstore.exception.InsufficientStockException;
import com.example.bookstore.support.TestCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Two buyers race for the last copy: exactly one wins, and stock never goes negative. Repeated many times. */
@SpringBootTest
@Import(TestCatalog.class)
class CheckoutConcurrencyIT {

    @Autowired
    private TestCatalog catalog;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @RepeatedTest(5)
    void twoBuyersForTheLastCopy_exactlyOneSucceeds() throws Exception {
        Book lastCopy = catalog.book("Contested Copy", "25.00", 1);
        long alice = catalog.user("race.alice");
        long bob = catalog.user("race.bob");
        cartService.addItem(alice, lastCopy.getId(), 1);
        cartService.addItem(bob, lastCopy.getId(), 1);

        List<String> outcomes = checkoutAtTheSameMoment(alice, bob);

        assertThat(outcomes).containsExactlyInAnyOrder("ORDER_PLACED", "INSUFFICIENT_STOCK");
        assertThat(catalog.stockOf(lastCopy)).isZero();
    }

    private List<String> checkoutAtTheSameMoment(long... userIds) throws Exception {
        CountDownLatch startTogether = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(userIds.length);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (long userId : userIds) {
                Callable<String> checkout = () -> {
                    startTogether.await();
                    try {
                        orderService.checkout(userId);
                        return "ORDER_PLACED";
                    } catch (InsufficientStockException expected) {
                        return "INSUFFICIENT_STOCK";
                    } catch (RuntimeException unexpected) {
                        return "UNEXPECTED: " + unexpected;
                    }
                };
                futures.add(pool.submit(checkout));
            }
            startTogether.countDown();
            List<String> outcomes = new ArrayList<>();
            for (Future<String> future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }
}
