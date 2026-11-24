package com.example.bookstore.controller;

import static com.example.bookstore.controller.AuthFlowIT.basic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bookstore.entity.Book;
import com.example.bookstore.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class CartFlowIT {

    private static final String PASSWORD = "secret123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    private long cleanCode; // 32.99, 15 in stock
    private long pragmatic; // 39.99, 20 in stock
    private long refactoring; // sold out
    private long domainDriven; // 1 left

    @BeforeEach
    void findSeededBooks() {
        cleanCode = bookId("9780132350884");
        pragmatic = bookId("9780135957059");
        refactoring = bookId("9780134757599");
        domainDriven = bookId("9780321125217");
    }

    @Test
    void addMergeChangeAndRemove_keepsTotalsRight_andNeverTouchesStock() throws Exception {
        String user = register("cart.flow@example.com");

        mockMvc.perform(as(user, post("/api/v1/cart/items")).content(item(cleanCode, 2)))
                .andExpect(status().isCreated());
        mockMvc.perform(as(user, post("/api/v1/cart/items")).content(item(cleanCode, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(3));
        mockMvc.perform(as(user, post("/api/v1/cart/items")).content(item(pragmatic, 1)))
                .andExpect(status().isCreated());
        mockMvc.perform(as(user, patch("/api/v1/cart/items/" + pragmatic)).content("{\"quantity\": 4}"))
                .andExpect(status().isOk());
        mockMvc.perform(as(user, delete("/api/v1/cart/items/" + cleanCode))).andExpect(status().isNoContent());

        mockMvc.perform(as(user, get("/api/v1/cart")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].bookId").value(pragmatic))
                .andExpect(jsonPath("$.items[0].title").value("The Pragmatic Programmer"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(39.99))
                .andExpect(jsonPath("$.items[0].quantity").value(4))
                .andExpect(jsonPath("$.items[0].lineTotal").value(159.96))
                .andExpect(jsonPath("$.items[0].availableQuantity").value(20))
                .andExpect(jsonPath("$.items[0].exceedsAvailable").value(false))
                .andExpect(jsonPath("$.totalQuantity").value(4))
                .andExpect(jsonPath("$.subtotal").value(159.96))
                .andExpect(jsonPath("$.checkoutAllowed").value(true));

        // the cart does not reserve stock
        mockMvc.perform(get("/api/v1/books/" + pragmatic))
                .andExpect(jsonPath("$.availableQuantity").value(20));

        mockMvc.perform(as(user, delete("/api/v1/cart"))).andExpect(status().isNoContent());
        mockMvc.perform(as(user, get("/api/v1/cart")))
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.subtotal").value(0))
                .andExpect(jsonPath("$.checkoutAllowed").value(false));
    }

    @Test
    void addingMoreThanAvailable_returns409WithStockDetails() throws Exception {
        String user = register("cart.stock@example.com");

        mockMvc.perform(as(user, post("/api/v1/cart/items")).content(item(refactoring, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://bookstore.example/problems/insufficient-stock"))
                .andExpect(jsonPath("$.errors[0].bookId").value(refactoring))
                .andExpect(jsonPath("$.errors[0].title").value("Refactoring"))
                .andExpect(jsonPath("$.errors[0].requested").value(1))
                .andExpect(jsonPath("$.errors[0].available").value(0));

        mockMvc.perform(as(user, post("/api/v1/cart/items")).content(item(domainDriven, 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].available").value(1));
    }

    @Test
    void cart_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
    }

    private String register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s", "fullName": "Cart Tester"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
        return basic(email, PASSWORD);
    }

    private static MockHttpServletRequestBuilder as(String authorization, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, authorization).contentType(MediaType.APPLICATION_JSON);
    }

    private static String item(long bookId, int quantity) {
        return "{\"bookId\": %d, \"quantity\": %d}".formatted(bookId, quantity);
    }

    private long bookId(String isbn) {
        return bookRepository.findAll().stream()
                .filter(book -> book.getIsbn().equals(isbn))
                .map(Book::getId)
                .findFirst()
                .orElseThrow();
    }
}
