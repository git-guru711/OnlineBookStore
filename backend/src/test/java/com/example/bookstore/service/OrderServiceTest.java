package com.example.bookstore.service;

import static com.example.bookstore.service.CartServiceTest.book;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.bookstore.entity.Cart;
import com.example.bookstore.entity.Order;
import com.example.bookstore.exception.EmptyCartException;
import com.example.bookstore.exception.InsufficientStockException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.model.OrderItemResponse;
import com.example.bookstore.model.OrderResponse;
import com.example.bookstore.model.OrderStatus;
import com.example.bookstore.model.OrderSummaryResponse;
import com.example.bookstore.model.PageResponse;
import com.example.bookstore.model.StockRequest;
import com.example.bookstore.model.StockShortage;
import com.example.bookstore.repository.CartRepository;
import com.example.bookstore.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final long USER = 7L;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void checkout_decreasesStockForEveryLine_savesOrder_andEmptiesCart() {
        Cart cart = cartWith2Lines();
        given(cartRepository.findByUserIdWithItems(USER)).willReturn(Optional.of(cart));
        given(orderRepository.saveAndFlush(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        OrderResponse order = orderService.checkout(USER);

        InOrder inOrder = inOrder(inventoryService, orderRepository);
        inOrder.verify(inventoryService)
                .decreaseStock(List.of(
                        new StockRequest(1L, "Clean Code", 2), new StockRequest(5L, "The Pragmatic Programmer", 1)));
        inOrder.verify(orderRepository).saveAndFlush(any(Order.class));
        assertThat(cart.getItems()).isEmpty();
        assertThat(order.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.items())
                .containsExactly(
                        new OrderItemResponse(1L, "Clean Code", new BigDecimal("32.99"), 2, new BigDecimal("65.98")),
                        new OrderItemResponse(
                                5L, "The Pragmatic Programmer", new BigDecimal("39.99"), 1, new BigDecimal("39.99")));
        assertThat(order.totalQuantity()).isEqualTo(3);
        assertThat(order.totalAmount()).isEqualByComparingTo("105.97");
    }

    @Test
    void checkout_userWithoutCart_throwsEmptyCart() {
        given(cartRepository.findByUserIdWithItems(USER)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.checkout(USER)).isInstanceOf(EmptyCartException.class);
        verify(inventoryService, never()).decreaseStock(anyList());
    }

    @Test
    void checkout_emptyCart_throwsEmptyCart_andTouchesNoStock() {
        given(cartRepository.findByUserIdWithItems(USER)).willReturn(Optional.of(new Cart(USER)));

        assertThatThrownBy(() -> orderService.checkout(USER)).isInstanceOf(EmptyCartException.class);
        verify(inventoryService, never()).decreaseStock(anyList());
    }

    @Test
    void checkout_notEnoughStock_savesNoOrder_andKeepsCart() {
        Cart cart = cartWith2Lines();
        given(cartRepository.findByUserIdWithItems(USER)).willReturn(Optional.of(cart));
        willThrow(new InsufficientStockException(List.of(new StockShortage(1L, "Clean Code", 2, 1))))
                .given(inventoryService)
                .decreaseStock(anyList());

        assertThatThrownBy(() -> orderService.checkout(USER)).isInstanceOf(InsufficientStockException.class);
        verify(orderRepository, never()).saveAndFlush(any());
        assertThat(cart.getItems()).hasSize(2);
    }

    @Test
    void getOrder_ownOrder_returnsIt() {
        given(orderRepository.findByIdAndUserIdWithItems(42L, USER))
                .willReturn(Optional.of(Order.placeFrom(cartWith2Lines())));

        assertThat(orderService.getOrder(USER, 42L).totalAmount()).isEqualByComparingTo("105.97");
    }

    @Test
    void getOrder_missingOrSomeoneElses_throwsNotFound() {
        given(orderRepository.findByIdAndUserIdWithItems(42L, USER)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(USER, 42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order 42 not found");
    }

    @Test
    void listOrders_returnsPagedSummaries() {
        OrderSummaryResponse summary = new OrderSummaryResponse(
                42L, OrderStatus.PLACED, 3, new BigDecimal("105.97"), Instant.parse("2026-11-20T10:20:00Z"));
        PageRequest pageable = PageRequest.of(0, 20);
        given(orderRepository.findSummariesByUserId(USER, pageable))
                .willReturn(new PageImpl<>(List.of(summary), pageable, 1));

        PageResponse<OrderSummaryResponse> page = orderService.listOrders(USER, pageable);

        assertThat(page.content()).containsExactly(summary);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    private static Cart cartWith2Lines() {
        Cart cart = new Cart(USER);
        cart.addItem(book(1L, "Clean Code", "32.99"), 2);
        cart.addItem(book(5L, "The Pragmatic Programmer", "39.99"), 1);
        return cart;
    }
}
