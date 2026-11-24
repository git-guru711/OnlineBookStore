package com.example.bookstore.controller;

import static com.example.bookstore.controller.AuthFlowIT.basic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bookstore.entity.Book;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.support.TestCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Concurrent checkouts over real HTTP: every outcome is 201/400/409, never 500, and stock always adds up. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestCatalog.class)
class CheckoutContentionIT {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final String PASSWORD = "secret123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestCatalog catalog;

    @Autowired
    private OrderRepository orderRepository;

    /** Opposite line order in two carts: without a consistent lock order this deadlocks. */
    @RepeatedTest(10)
    void twoCheckoutsSharingBooksInOppositeOrder_bothSucceed() throws Exception {
        Book bookA = catalog.book("Contended A", "10.00", 10);
        Book bookB = catalog.book("Contended B", "12.00", 10);
        String alice = register("contention.alice");
        String bob = register("contention.bob");
        addToCart(alice, bookA, 1);
        addToCart(alice, bookB, 1);
        addToCart(bob, bookB, 1); // opposite order
        addToCart(bob, bookA, 1);

        List<Integer> statuses = checkoutAtTheSameMoment(alice, bob);

        assertThat(statuses).containsExactly(201, 201);
        assertThat(catalog.stockOf(bookA)).isEqualTo(8);
        assertThat(catalog.stockOf(bookB)).isEqualTo(8);
    }

    /** A double click on "Place order": one order, stock decreased once; the other request gets 409 or 400. */
    @RepeatedTest(10)
    void sameUserCheckingOutTwiceAtOnce_createsExactlyOneOrder() throws Exception {
        Book book = catalog.book("Double Submit", "20.00", 10);
        String carol = register("contention.carol");
        addToCart(carol, book, 2);
        long ordersBefore = orderRepository.count();

        List<Integer> statuses = checkoutAtTheSameMoment(carol, carol);

        assertThat(statuses).containsOnlyOnce(201);
        assertThat(statuses).hasSize(2).allMatch(code -> code == 201 || code == 409 || code == 400);
        assertThat(orderRepository.count()).isEqualTo(ordersBefore + 1);
        assertThat(catalog.stockOf(book)).isEqualTo(8);
    }

    /** The same Idempotency-Key twice at once: one order, and both requests get it back (201 and 200). */
    @RepeatedTest(10)
    void sameIdempotencyKeyTwiceAtOnce_createsOneOrder_andBothRequestsGetIt() throws Exception {
        Book book = catalog.book("Idempotent Race", "15.00", 10);
        String dave = register("contention.dave");
        addToCart(dave, book, 3);
        long ordersBefore = orderRepository.count();
        String key = "race-%d-0000-4000-8000-000000000000".formatted(SEQUENCE.incrementAndGet());

        List<String[]> responses = checkoutAtTheSameMomentWithKey(key, dave, dave);

        assertThat(responses).extracting(response -> response[0]).containsExactlyInAnyOrder("201", "200");
        assertThat(responses)
                .extracting(response -> idOf(response[1]))
                .containsOnly(idOf(responses.get(0)[1]));
        assertThat(orderRepository.count()).isEqualTo(ordersBefore + 1);
        assertThat(catalog.stockOf(book)).isEqualTo(7);
    }

    private List<String[]> checkoutAtTheSameMomentWithKey(String key, String... authorizations) throws Exception {
        CountDownLatch startTogether = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(authorizations.length);
        try {
            List<Future<String[]>> futures = new ArrayList<>();
            for (String authorization : authorizations) {
                Callable<String[]> checkout = () -> {
                    startTogether.await();
                    var response = mockMvc.perform(post("/api/v1/orders")
                                    .header(HttpHeaders.AUTHORIZATION, authorization)
                                    .header("Idempotency-Key", key))
                            .andReturn()
                            .getResponse();
                    return new String[] {String.valueOf(response.getStatus()), response.getContentAsString()};
                };
                futures.add(pool.submit(checkout));
            }
            startTogether.countDown();
            List<String[]> responses = new ArrayList<>();
            for (Future<String[]> future : futures) {
                responses.add(future.get(30, TimeUnit.SECONDS));
            }
            return responses;
        } finally {
            pool.shutdownNow();
        }
    }

    private static String idOf(String orderJson) {
        return orderJson.replaceAll("(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1");
    }

    private List<Integer> checkoutAtTheSameMoment(String... authorizations) throws Exception {
        CountDownLatch startTogether = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(authorizations.length);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (String authorization : authorizations) {
                Callable<Integer> checkout = () -> {
                    startTogether.await();
                    return mockMvc.perform(post("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, authorization))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                };
                futures.add(pool.submit(checkout));
            }
            startTogether.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(30, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }

    private String register(String name) throws Exception {
        String email = "%s.%d@example.com".formatted(name, SEQUENCE.incrementAndGet());
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s", "fullName": "Contention Tester"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
        return basic(email, PASSWORD);
    }

    private void addToCart(String authorization, Book book, int quantity) throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\": %d, \"quantity\": %d}".formatted(book.getId(), quantity)))
                .andExpect(status().isCreated());
    }
}
