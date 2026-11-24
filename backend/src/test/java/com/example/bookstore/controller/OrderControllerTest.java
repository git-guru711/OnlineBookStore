package com.example.bookstore.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bookstore.config.AuthenticatedUser;
import com.example.bookstore.config.SecurityConfig;
import com.example.bookstore.exception.EmptyCartException;
import com.example.bookstore.exception.InsufficientStockException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.model.CheckoutResult;
import com.example.bookstore.model.OrderItemResponse;
import com.example.bookstore.model.OrderResponse;
import com.example.bookstore.model.OrderStatus;
import com.example.bookstore.model.OrderSummaryResponse;
import com.example.bookstore.model.PageResponse;
import com.example.bookstore.model.Role;
import com.example.bookstore.model.StockShortage;
import com.example.bookstore.service.IdempotentCheckout;
import com.example.bookstore.service.OrderService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerTest {

    private static final String PROBLEMS = "https://bookstore.example/problems/";
    private static final AuthenticatedUser JANE = new AuthenticatedUser(7L, "jane@example.com", "hash", Role.USER);
    private static final Instant CREATED = Instant.parse("2026-11-20T10:20:00Z");
    private static final OrderResponse ORDER = new OrderResponse(
            42L,
            OrderStatus.PLACED,
            List.of(new OrderItemResponse(1L, "Clean Code", new BigDecimal("32.99"), 2, new BigDecimal("65.98"))),
            2,
            new BigDecimal("65.98"),
            CREATED);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private IdempotentCheckout idempotentCheckout;

    @Test
    void checkout_returns201WithLocationAndOrder() throws Exception {
        given(idempotentCheckout.checkout(7L, null)).willReturn(CheckoutResult.created(ORDER));

        mockMvc.perform(post("/api/v1/orders").with(user(JANE)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/orders/42"))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.items[0].bookId").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Clean Code"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(32.99))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].lineTotal").value(65.98))
                .andExpect(jsonPath("$.totalQuantity").value(2))
                .andExpect(jsonPath("$.totalAmount").value(65.98))
                .andExpect(jsonPath("$.createdAt").value("2026-11-20T10:20:00Z"));
    }

    @Test
    void checkout_repeatedWithTheSameIdempotencyKey_returns200WithTheOriginalOrder() throws Exception {
        given(idempotentCheckout.checkout(7L, "3f2a9c1e-0000-4000-8000-000000000042"))
                .willReturn(CheckoutResult.alreadyPlaced(ORDER));

        mockMvc.perform(post("/api/v1/orders")
                        .with(user(JANE))
                        .header("Idempotency-Key", "3f2a9c1e-0000-4000-8000-000000000042"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION)) // nothing new was created
                .andExpect(jsonPath("$.id").value(42));
    }

    @Test
    void checkout_withAMalformedIdempotencyKey_returns400_withoutCheckingOut() throws Exception {
        mockMvc.perform(post("/api/v1/orders").with(user(JANE)).header("Idempotency-Key", "short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "validation-error"));

        verifyNoInteractions(idempotentCheckout);
    }

    @Test
    void checkout_emptyCart_returns400() throws Exception {
        given(idempotentCheckout.checkout(7L, null)).willThrow(new EmptyCartException());

        mockMvc.perform(post("/api/v1/orders").with(user(JANE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "cart-empty"));
    }

    @Test
    void checkout_notEnoughStock_returns409WithShortages() throws Exception {
        given(idempotentCheckout.checkout(7L, null))
                .willThrow(
                        new InsufficientStockException(List.of(new StockShortage(4L, "Domain-Driven Design", 1, 0))));

        mockMvc.perform(post("/api/v1/orders").with(user(JANE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "insufficient-stock"))
                .andExpect(jsonPath("$.errors[0].bookId").value(4));
    }

    @Test
    void listOrders_returnsPagedHistory() throws Exception {
        given(orderService.listOrders(7L, PageRequest.of(0, 20)))
                .willReturn(new PageResponse<>(
                        List.of(new OrderSummaryResponse(42L, OrderStatus.PLACED, 2, new BigDecimal("65.98"), CREATED)),
                        0,
                        20,
                        1,
                        1));

        mockMvc.perform(get("/api/v1/orders").with(user(JANE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(42))
                .andExpect(jsonPath("$.content[0].status").value("PLACED"))
                .andExpect(jsonPath("$.content[0].totalQuantity").value(2))
                .andExpect(jsonPath("$.content[0].totalAmount").value(65.98))
                .andExpect(jsonPath("$.content[0].createdAt").value("2026-11-20T10:20:00Z"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listOrders_withPagingParams_passesThem() throws Exception {
        given(orderService.listOrders(7L, PageRequest.of(2, 5))).willReturn(new PageResponse<>(List.of(), 2, 5, 0, 0));

        mockMvc.perform(get("/api/v1/orders")
                        .param("page", "2")
                        .param("size", "5")
                        .with(user(JANE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2));
    }

    @Test
    void listOrders_sizeAbove100_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/orders").param("size", "101").with(user(JANE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"));
        verifyNoInteractions(orderService);
    }

    @Test
    void getOrder_ownOrder_returns200() throws Exception {
        given(orderService.getOrder(7L, 42L)).willReturn(ORDER);

        mockMvc.perform(get("/api/v1/orders/42").with(user(JANE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42));
    }

    @Test
    void getOrder_someoneElsesOrMissing_returns404() throws Exception {
        given(orderService.getOrder(7L, 43L)).willThrow(new ResourceNotFoundException("Order 43 not found"));

        mockMvc.perform(get("/api/v1/orders/43").with(user(JANE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Order 43 not found"));
    }

    @Test
    void getOrder_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/orders/abc").with(user(JANE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "malformed-request"));
    }

    @Test
    void orderEndpoints_withoutCredentials_return401() throws Exception {
        mockMvc.perform(post("/api/v1/orders")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/orders")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/orders/42")).andExpect(status().isUnauthorized());
        verifyNoInteractions(orderService);
    }
}
