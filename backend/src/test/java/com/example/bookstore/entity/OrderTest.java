package com.example.bookstore.entity;

import static com.example.bookstore.entity.CartTest.book;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.example.bookstore.exception.EmptyCartException;
import com.example.bookstore.model.OrderStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OrderTest {

    private static final long USER_ID = 7L;

    @Test
    void placeFrom_copiesEachLineWithTitleAndPrice_andTheTotal() {
        Cart cart = new Cart(USER_ID);
        cart.addItem(book(1L, "Clean Code", "32.99"), 2);
        cart.addItem(book(5L, "The Pragmatic Programmer", "39.99"), 1);

        Order order = Order.placeFrom(cart);

        assertThat(order.getUserId()).isEqualTo(USER_ID);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.getItems())
                .extracting(OrderItem::getBookId, OrderItem::getTitle, OrderItem::getUnitPrice, OrderItem::getQuantity)
                .containsExactly(
                        tuple(1L, "Clean Code", new BigDecimal("32.99"), 2),
                        tuple(5L, "The Pragmatic Programmer", new BigDecimal("39.99"), 1));
        assertThat(order.getItems().get(0).lineTotal()).isEqualByComparingTo("65.98");
        assertThat(order.totalQuantity()).isEqualTo(3);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("105.97");
    }

    @Test
    void placeFrom_keepsCopiedPrices_whenTheBookPriceChangesLater() {
        Book cleanCode = book(1L, "Clean Code", "32.99");
        Cart cart = new Cart(USER_ID);
        cart.addItem(cleanCode, 1);
        Order order = Order.placeFrom(cart);

        ReflectionTestUtils.setField(cleanCode, "price", new BigDecimal("99.00")); // price rise afterwards

        assertThat(order.getItems().get(0).getUnitPrice()).isEqualByComparingTo("32.99");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("32.99");
    }

    @Test
    void placeFrom_emptyCart_isRejected() {
        assertThatThrownBy(() -> Order.placeFrom(new Cart(USER_ID)))
                .isInstanceOf(EmptyCartException.class)
                .hasMessage("Your cart is empty");
    }

    @Test
    void getItems_cannotBeModifiedFromOutside() {
        Cart cart = new Cart(USER_ID);
        cart.addItem(book(1L, "Clean Code", "32.99"), 1);

        assertThatThrownBy(() -> Order.placeFrom(cart).getItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
