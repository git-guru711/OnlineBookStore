package com.example.bookstore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Cart;
import com.example.bookstore.exception.ConcurrentRequestException;
import com.example.bookstore.exception.InsufficientStockException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.model.AddToCartResult;
import com.example.bookstore.model.CartItemResponse;
import com.example.bookstore.model.CartResponse;
import com.example.bookstore.model.StockShortage;
import com.example.bookstore.model.StockStatus;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.CartRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartServiceTest {

    private static final long USER = 7L;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private CartService cartService;

    private final Book cleanCode = book(1L, "Clean Code", "32.99");
    private final Book ddd = book(4L, "Domain-Driven Design", "49.95");

    @BeforeEach
    void setUp() {
        given(cartRepository.saveAndFlush(any(Cart.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(bookRepository.findById(1L)).willReturn(Optional.of(cleanCode));
        given(bookRepository.findById(4L)).willReturn(Optional.of(ddd));
        given(inventoryService.availableQuantities(anyCollection())).willReturn(Map.of(1L, 15, 4L, 1));
    }

    @Test
    void getCart_userWithoutCart_returnsEmptyCart_andCreatesNothing() {
        given(cartRepository.findByUserIdWithItems(USER)).willReturn(Optional.empty());

        CartResponse cart = cartService.getCart(USER);

        assertThat(cart.items()).isEmpty();
        assertThat(cart.totalQuantity()).isZero();
        assertThat(cart.subtotal()).isEqualByComparingTo("0");
        assertThat(cart.checkoutAllowed()).isFalse();
        verify(cartRepository, never()).saveAndFlush(any());
    }

    @Test
    void getCart_mapsLinesWithLivePriceAndAvailability() {
        Cart cart = new Cart(USER);
        cart.addItem(cleanCode, 2);
        cart.addItem(ddd, 2); // only 1 in stock
        given(cartRepository.findByUserIdWithItems(USER)).willReturn(Optional.of(cart));

        CartResponse response = cartService.getCart(USER);

        assertThat(response.items())
                .containsExactly(
                        new CartItemResponse(
                                1L,
                                "Clean Code",
                                "Author",
                                new BigDecimal("32.99"),
                                2,
                                new BigDecimal("65.98"),
                                15,
                                StockStatus.IN_STOCK,
                                false),
                        new CartItemResponse(
                                4L,
                                "Domain-Driven Design",
                                "Author",
                                new BigDecimal("49.95"),
                                2,
                                new BigDecimal("99.90"),
                                1,
                                StockStatus.IN_STOCK,
                                true));
        assertThat(response.totalQuantity()).isEqualTo(4);
        assertThat(response.subtotal()).isEqualByComparingTo("165.88");
        assertThat(response.checkoutAllowed()).isFalse();
    }

    @Test
    void addItem_firstBook_createsCart_andReportsNewLine() {
        given(cartRepository.findByUserIdWithItems(USER)).willReturn(Optional.empty());

        AddToCartResult result = cartService.addItem(USER, 1L, 2);

        assertThat(result.newLine()).isTrue();
        assertThat(result.cart().items()).extracting(CartItemResponse::quantity).containsExactly(2);
        verify(cartRepository).saveAndFlush(any(Cart.class));
    }

    @Test
    void addItem_bookAlreadyInCart_mergesAndChecksStockForTheMergedQuantity() {
        Cart cart = new Cart(USER);
        cart.addItem(cleanCode, 2);
        given(cartRepository.findByUserIdWithItems(USER)).willReturn(Optional.of(cart));

        AddToCartResult result = cartService.addItem(USER, 1L, 3);

        assertThat(result.newLine()).isFalse();
        assertThat(result.cart().items().get(0).quantity()).isEqualTo(5);
        verify(inventoryService).requireAvailable(cleanCode, 5);
    }

    @Test
    void addItem_unknownBook_throwsNotFound() {
        given(bookRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(USER, 99L, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Book 99 not found");
    }

    @Test
    void addItem_notEnoughStock_throws_andSavesNothing() {
        given(cartRepository.findByUserIdWithItems(USER)).willReturn(Optional.empty());
        willThrow(new InsufficientStockException(List.of(new StockShortage(4L, "Domain-Driven Design", 2, 1))))
                .given(inventoryService)
                .requireAvailable(ddd, 2);

        assertThatThrownBy(() -> cartService.addItem(USER, 4L, 2)).isInstanceOf(InsufficientStockException.class);
        verify(cartRepository, never()).saveAndFlush(any());
    }

    @Test
    void addItem_anotherRequestCreatedTheCartAtTheSameTime_throwsConcurrentRequest() {
        given(cartRepository.findByUserIdWithItems(USER)).willReturn(Optional.empty());
        given(cartRepository.saveAndFlush(any(Cart.class)))
                .willThrow(new DataIntegrityViolationException("uk_carts_user"));

        assertThatThrownBy(() -> cartService.addItem(USER, 1L, 1)).isInstanceOf(ConcurrentRequestException.class);
    }

    @Test
    void changeQuantity_checksStock_andUpdatesLine() {
        Cart cart = new Cart(USER);
        cart.addItem(cleanCode, 2);
        given(cartRepository.findByUserIdWithItems(USER)).willReturn(Optional.of(cart));

        CartResponse response = cartService.changeQuantity(USER, 1L, 7);

        verify(inventoryService).requireAvailable(cleanCode, 7);
        assertThat(response.items().get(0).quantity()).isEqualTo(7);
    }

    @Test
    void changeQuantity_userWithoutCart_throwsNotFound() {
        given(cartRepository.findByUserIdWithItems(USER)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.changeQuantity(USER, 1L, 2))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Book 1 is not in the cart");
    }

    @Test
    void removeItem_removesLine() {
        Cart cart = new Cart(USER);
        cart.addItem(cleanCode, 2);
        given(cartRepository.findByUserIdWithItems(USER)).willReturn(Optional.of(cart));

        cartService.removeItem(USER, 1L);

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void removeItem_userWithoutCart_throwsNotFound() {
        given(cartRepository.findByUserIdWithItems(USER)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItem(USER, 1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void clear_emptiesCart_andIsSafeWithoutCart() {
        Cart cart = new Cart(USER);
        cart.addItem(cleanCode, 2);
        given(cartRepository.findByUserIdWithItems(USER)).willReturn(Optional.of(cart));

        cartService.clear(USER);
        assertThat(cart.getItems()).isEmpty();

        given(cartRepository.findByUserIdWithItems(USER)).willReturn(Optional.empty());
        cartService.clear(USER); // no exception: clearing is idempotent
    }

    static Book book(long id, String title, String price) {
        Book book = new Book("isbn-" + id, title, "Author", new BigDecimal(price));
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }
}
