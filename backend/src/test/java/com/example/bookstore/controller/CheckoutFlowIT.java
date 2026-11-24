package com.example.bookstore.controller;

import static com.example.bookstore.controller.AuthFlowIT.basic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Inventory;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.InventoryRepository;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Uses its own books, so it never changes the stock of the shared demo catalog. */
@SpringBootTest
@AutoConfigureMockMvc
class CheckoutFlowIT {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final String PASSWORD = "secret123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void checkout_createsOrder_decreasesStock_andEmptiesCart() throws Exception {
        long bookA = createBook("Checkout Book A", "20.00", 5);
        long bookB = createBook("Checkout Book B", "15.50", 3);
        String user = register("checkout.flow@example.com");
        addToCart(user, bookA, 2);
        addToCart(user, bookB, 1);

        String location = mockMvc.perform(as(user, post("/api/v1/orders")))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, matchesPattern("/api/v1/orders/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].bookId").value(bookA))
                .andExpect(jsonPath("$.items[0].title").value("Checkout Book A"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(20.00))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].lineTotal").value(40.00))
                .andExpect(jsonPath("$.totalQuantity").value(3))
                .andExpect(jsonPath("$.totalAmount").value(55.50))
                .andExpect(jsonPath("$.createdAt").isString())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.LOCATION);
        assertThat(location).isNotNull();
        long orderId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

        mockMvc.perform(get("/api/v1/books/" + bookA))
                .andExpect(jsonPath("$.availableQuantity").value(3));
        mockMvc.perform(get("/api/v1/books/" + bookB))
                .andExpect(jsonPath("$.availableQuantity").value(2));
        mockMvc.perform(as(user, get("/api/v1/cart")))
                .andExpect(jsonPath("$.items.length()").value(0));

        mockMvc.perform(as(user, get(location)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.totalAmount").value(55.50));
        mockMvc.perform(as(user, get("/api/v1/orders")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(orderId))
                .andExpect(jsonPath("$.content[0].status").value("PLACED"))
                .andExpect(jsonPath("$.content[0].totalQuantity").value(3))
                .andExpect(jsonPath("$.content[0].totalAmount").value(55.50))
                .andExpect(jsonPath("$.totalElements").value(1));

        String otherUser = register("checkout.other@example.com");
        mockMvc.perform(as(otherUser, get(location))).andExpect(status().isNotFound());
    }

    @Test
    void checkout_retriedWithTheSameIdempotencyKey_returnsTheOriginalOrder_andOrdersOnlyOnce() throws Exception {
        long book = createBook("Idempotent Book", "12.00", 5);
        String user = register("idempotent.retry@example.com");
        addToCart(user, book, 2);
        String key = "7f9c2ba4-e88f-4f7c-9b3a-0d9b1a2c3e4f";

        long orderId = readId(mockMvc.perform(as(user, post("/api/v1/orders")).header("Idempotency-Key", key))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());

        // the response was lost: the client sends the same request again
        mockMvc.perform(as(user, post("/api/v1/orders")).header("Idempotency-Key", key))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId)) // the original order
                .andExpect(jsonPath("$.totalAmount").value(24.00));

        mockMvc.perform(get("/api/v1/books/" + book))
                .andExpect(jsonPath("$.availableQuantity").value(3)); // decreased once
        mockMvc.perform(as(user, get("/api/v1/orders")))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void checkout_retriedAfterTheCartWasFilledAgain_stillReturnsTheOriginalOrder_andLeavesTheNewCartAlone()
            throws Exception {
        long book = createBook("Idempotent Refill", "10.00", 10);
        String user = register("idempotent.refill@example.com");
        addToCart(user, book, 1);
        String key = "a1b2c3d4-0000-4000-8000-000000000001";
        long orderId = readId(mockMvc.perform(as(user, post("/api/v1/orders")).header("Idempotency-Key", key))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
        addToCart(user, book, 3);

        mockMvc.perform(as(user, post("/api/v1/orders")).header("Idempotency-Key", key))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.totalQuantity").value(1));

        mockMvc.perform(as(user, get("/api/v1/cart")))
                .andExpect(jsonPath("$.totalQuantity").value(3)); // not ordered
        mockMvc.perform(get("/api/v1/books/" + book))
                .andExpect(jsonPath("$.availableQuantity").value(9));
    }

    @Test
    void idempotencyKeys_belongToOneUser_anotherUsersSameKeyIsANewPurchase() throws Exception {
        long book = createBook("Idempotent Shared Key", "8.00", 10);
        String alice = register("idempotent.alice@example.com");
        String bob = register("idempotent.bob@example.com");
        addToCart(alice, book, 1);
        addToCart(bob, book, 2);
        String sameKey = "b2c3d4e5-1111-4111-8111-111111111111";

        long aliceOrder =
                readId(mockMvc.perform(as(alice, post("/api/v1/orders")).header("Idempotency-Key", sameKey))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        long bobOrder = readId(mockMvc.perform(as(bob, post("/api/v1/orders")).header("Idempotency-Key", sameKey))
                .andExpect(status().isCreated()) // not Alice's order
                .andExpect(jsonPath("$.totalQuantity").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString());

        assertThat(bobOrder).isNotEqualTo(aliceOrder);
    }

    @Test
    void checkout_withAMalformedIdempotencyKey_returns400_andOrdersNothing() throws Exception {
        long book = createBook("Idempotent Bad Key", "9.00", 4);
        String user = register("idempotent.badkey@example.com");
        addToCart(user, book, 1);

        mockMvc.perform(as(user, post("/api/v1/orders")).header("Idempotency-Key", "not a key!"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/books/" + book))
                .andExpect(jsonPath("$.availableQuantity").value(4));
    }

    @Test
    void checkout_withEmptyCart_returns400() throws Exception {
        String user = register("checkout.empty@example.com");

        mockMvc.perform(as(user, post("/api/v1/orders")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://bookstore.example/problems/cart-empty"));
    }

    @Test
    void orders_requireAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/orders")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/orders")).andExpect(status().isUnauthorized());
    }

    private static long readId(String orderJson) {
        return Long.parseLong(orderJson.replaceAll("(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }

    private long createBook(String title, String price, int stock) {
        String isbn = "97899%08d".formatted(SEQUENCE.incrementAndGet());
        Book book = bookRepository.save(new Book(isbn, title, "Test Author", new BigDecimal(price)));
        inventoryRepository.save(new Inventory(book.getId(), stock));
        return book.getId();
    }

    private String register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s", "fullName": "Checkout Tester"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
        return basic(email, PASSWORD);
    }

    private void addToCart(String user, long bookId, int quantity) throws Exception {
        mockMvc.perform(as(user, post("/api/v1/cart/items"))
                        .content("{\"bookId\": %d, \"quantity\": %d}".formatted(bookId, quantity)))
                .andExpect(status().isCreated());
    }

    private static MockHttpServletRequestBuilder as(String authorization, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, authorization).contentType(MediaType.APPLICATION_JSON);
    }
}
