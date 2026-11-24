package com.example.bookstore.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.example.bookstore.exception.InvalidQuantityException;
import com.example.bookstore.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class CartTest {

    private static final long USER_ID = 7L;

    private final Book cleanCode = book(1L, "Clean Code", "32.99");
    private final Book pragmatic = book(5L, "The Pragmatic Programmer", "39.99");

    @Test
    void newCart_isEmpty_withZeroTotals() {
        Cart cart = new Cart(USER_ID);

        assertThat(cart.getUserId()).isEqualTo(USER_ID);
        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.totalQuantity()).isZero();
        assertThat(cart.total()).isEqualByComparingTo("0.00");
    }

    @Test
    void addItem_newBook_addsLine() {
        Cart cart = new Cart(USER_ID);

        cart.addItem(cleanCode, 2);

        assertThat(cart.getItems())
                .extracting(item -> item.getBook().getTitle(), CartItem::getQuantity)
                .containsExactly(tuple("Clean Code", 2));
    }

    @Test
    void addItem_sameBookAgain_mergesQuantityIntoOneLine() {
        Cart cart = new Cart(USER_ID);

        cart.addItem(cleanCode, 2);
        cart.addItem(cleanCode, 3);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.quantityOf(1L)).isEqualTo(5);
    }

    @Test
    void totals_sumPriceTimesQuantity_acrossLines() {
        Cart cart = new Cart(USER_ID);

        cart.addItem(cleanCode, 2); // 65.98
        cart.addItem(pragmatic, 1); // 39.99

        assertThat(cart.totalQuantity()).isEqualTo(3);
        assertThat(cart.total()).isEqualByComparingTo("105.97");
        assertThat(cart.getItems().get(0).lineTotal()).isEqualByComparingTo("65.98");
    }

    @ParameterizedTest(name = "quantity {0} is rejected")
    @ValueSource(ints = {0, -1, 100})
    void addItem_quantityOutside1To99_isRejected(int quantity) {
        Cart cart = new Cart(USER_ID);

        assertThatThrownBy(() -> cart.addItem(cleanCode, quantity))
                .isInstanceOf(InvalidQuantityException.class)
                .hasMessage("Quantity must be between 1 and 99");
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void addItem_mergedQuantityAbove99_isRejected_andLineUnchanged() {
        Cart cart = new Cart(USER_ID);
        cart.addItem(cleanCode, 60);

        assertThatThrownBy(() -> cart.addItem(cleanCode, 40))
                .isInstanceOf(InvalidQuantityException.class)
                .hasMessage("Quantity must be between 1 and 99 (this would make 100)");
        assertThat(cart.quantityOf(1L)).isEqualTo(60);
    }

    @Test
    void changeQuantity_updatesLine() {
        Cart cart = new Cart(USER_ID);
        cart.addItem(cleanCode, 2);

        cart.changeQuantity(1L, 7);

        assertThat(cart.quantityOf(1L)).isEqualTo(7);
    }

    @Test
    void changeQuantity_outside1To99_isRejected() {
        Cart cart = new Cart(USER_ID);
        cart.addItem(cleanCode, 2);

        assertThatThrownBy(() -> cart.changeQuantity(1L, 0)).isInstanceOf(InvalidQuantityException.class);
        assertThat(cart.quantityOf(1L)).isEqualTo(2);
    }

    @Test
    void changeQuantity_bookNotInCart_throwsNotFound() {
        Cart cart = new Cart(USER_ID);

        assertThatThrownBy(() -> cart.changeQuantity(1L, 2))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Book 1 is not in the cart");
    }

    @Test
    void removeItem_removesLine() {
        Cart cart = new Cart(USER_ID);
        cart.addItem(cleanCode, 2);
        cart.addItem(pragmatic, 1);

        cart.removeItem(1L);

        assertThat(cart.getItems()).extracting(item -> item.getBook().getId()).containsExactly(5L);
    }

    @Test
    void removeItem_bookNotInCart_throwsNotFound() {
        Cart cart = new Cart(USER_ID);

        assertThatThrownBy(() -> cart.removeItem(1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void clear_removesAllLines() {
        Cart cart = new Cart(USER_ID);
        cart.addItem(cleanCode, 2);
        cart.addItem(pragmatic, 1);

        cart.clear();

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void getItems_cannotBeModifiedFromOutside() {
        Cart cart = new Cart(USER_ID);

        assertThatThrownBy(() -> cart.getItems().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    static Book book(long id, String title, String price) {
        Book book = new Book("isbn-" + id, title, "Author", new BigDecimal(price));
        ReflectionTestUtils.setField(book, "id", id); // ids normally come from the database
        return book;
    }
}
