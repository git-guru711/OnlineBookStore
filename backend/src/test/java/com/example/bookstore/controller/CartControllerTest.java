package com.example.bookstore.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bookstore.config.AuthenticatedUser;
import com.example.bookstore.config.SecurityConfig;
import com.example.bookstore.exception.InsufficientStockException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.model.AddToCartResult;
import com.example.bookstore.model.CartItemResponse;
import com.example.bookstore.model.CartResponse;
import com.example.bookstore.model.Role;
import com.example.bookstore.model.StockShortage;
import com.example.bookstore.model.StockStatus;
import com.example.bookstore.service.CartService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(CartController.class)
@Import(SecurityConfig.class)
class CartControllerTest {

    private static final String PROBLEMS = "https://bookstore.example/problems/";
    private static final AuthenticatedUser JANE = new AuthenticatedUser(7L, "jane@example.com", "hash", Role.USER);
    private static final CartResponse CART = new CartResponse(
            List.of(new CartItemResponse(
                    1L,
                    "Clean Code",
                    "Robert C. Martin",
                    new BigDecimal("32.99"),
                    2,
                    new BigDecimal("65.98"),
                    15,
                    StockStatus.IN_STOCK,
                    false)),
            2,
            new BigDecimal("65.98"),
            true);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @Test
    void getCart_returnsCurrentUsersCart() throws Exception {
        given(cartService.getCart(7L)).willReturn(CART);

        mockMvc.perform(asJane(get("/api/v1/cart")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].bookId").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Clean Code"))
                .andExpect(jsonPath("$.items[0].author").value("Robert C. Martin"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(32.99))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].lineTotal").value(65.98))
                .andExpect(jsonPath("$.items[0].availableQuantity").value(15))
                .andExpect(jsonPath("$.items[0].stockStatus").value("IN_STOCK"))
                .andExpect(jsonPath("$.items[0].exceedsAvailable").value(false))
                .andExpect(jsonPath("$.totalQuantity").value(2))
                .andExpect(jsonPath("$.subtotal").value(65.98))
                .andExpect(jsonPath("$.checkoutAllowed").value(true));
    }

    @Test
    void addItem_newLine_returns201WithLocation() throws Exception {
        given(cartService.addItem(7L, 1L, 2)).willReturn(new AddToCartResult(CART, true));

        mockMvc.perform(asJane(post("/api/v1/cart/items")).content("{\"bookId\": 1, \"quantity\": 2}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/cart"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void addItem_mergedIntoExistingLine_returns200() throws Exception {
        given(cartService.addItem(7L, 1L, 1)).willReturn(new AddToCartResult(CART, false));

        mockMvc.perform(asJane(post("/api/v1/cart/items")).content("{\"bookId\": 1, \"quantity\": 1}"))
                .andExpect(status().isOk());
    }

    @Test
    void addItem_quantityZero_returns400() throws Exception {
        mockMvc.perform(asJane(post("/api/v1/cart/items")).content("{\"bookId\": 1, \"quantity\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("quantity"));
        verifyNoInteractions(cartService);
    }

    @Test
    void addItem_missingBookIdAndQuantity_returns400ForBoth() throws Exception {
        mockMvc.perform(asJane(post("/api/v1/cart/items")).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("bookId"))
                .andExpect(jsonPath("$.errors[1].field").value("quantity"));
        verifyNoInteractions(cartService);
    }

    @Test
    void addItem_unknownBook_returns404() throws Exception {
        given(cartService.addItem(7L, 99L, 1)).willThrow(new ResourceNotFoundException("Book 99 not found"));

        mockMvc.perform(asJane(post("/api/v1/cart/items")).content("{\"bookId\": 99, \"quantity\": 1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Book 99 not found"));
    }

    @Test
    void addItem_notEnoughStock_returns409() throws Exception {
        given(cartService.addItem(7L, 4L, 2))
                .willThrow(
                        new InsufficientStockException(List.of(new StockShortage(4L, "Domain-Driven Design", 2, 1))));

        mockMvc.perform(asJane(post("/api/v1/cart/items")).content("{\"bookId\": 4, \"quantity\": 2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "insufficient-stock"))
                .andExpect(jsonPath("$.errors[0].available").value(1));
    }

    @Test
    void changeQuantity_returns200WithUpdatedCart() throws Exception {
        given(cartService.changeQuantity(7L, 1L, 2)).willReturn(CART);

        mockMvc.perform(asJane(patch("/api/v1/cart/items/1")).content("{\"quantity\": 2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void changeQuantity_above99_returns400() throws Exception {
        mockMvc.perform(asJane(patch("/api/v1/cart/items/1")).content("{\"quantity\": 100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("quantity"));
        verifyNoInteractions(cartService);
    }

    @Test
    void changeQuantity_bookNotInCart_returns404() throws Exception {
        given(cartService.changeQuantity(7L, 5L, 2))
                .willThrow(new ResourceNotFoundException("Book 5 is not in the cart"));

        mockMvc.perform(asJane(patch("/api/v1/cart/items/5")).content("{\"quantity\": 2}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void changeQuantity_nonNumericBookId_returns400() throws Exception {
        mockMvc.perform(asJane(patch("/api/v1/cart/items/abc")).content("{\"quantity\": 2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "malformed-request"));
    }

    @Test
    void removeItem_returns204() throws Exception {
        mockMvc.perform(asJane(delete("/api/v1/cart/items/1"))).andExpect(status().isNoContent());
        verify(cartService).removeItem(7L, 1L);
    }

    @Test
    void removeItem_bookNotInCart_returns404() throws Exception {
        willThrow(new ResourceNotFoundException("Book 5 is not in the cart"))
                .given(cartService)
                .removeItem(7L, 5L);

        mockMvc.perform(asJane(delete("/api/v1/cart/items/5"))).andExpect(status().isNotFound());
    }

    @Test
    void clearCart_returns204() throws Exception {
        mockMvc.perform(asJane(delete("/api/v1/cart"))).andExpect(status().isNoContent());
        verify(cartService).clear(7L);
    }

    @Test
    void cartEndpoints_withoutCredentials_return401() throws Exception {
        mockMvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\": 1, \"quantity\": 1}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(cartService);
    }

    private static MockHttpServletRequestBuilder asJane(MockHttpServletRequestBuilder request) {
        return request.with(user(JANE)).contentType(MediaType.APPLICATION_JSON);
    }
}
