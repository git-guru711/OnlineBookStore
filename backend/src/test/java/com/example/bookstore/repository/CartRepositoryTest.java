package com.example.bookstore.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Cart;
import com.example.bookstore.entity.CartItem;
import com.example.bookstore.entity.User;
import com.example.bookstore.support.RepositoryTest;
import java.math.BigDecimal;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@RepositoryTest
class CartRepositoryTest {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User user;
    private Book cleanCode;
    private Book pragmatic;

    @BeforeEach
    void setUp() {
        user = entityManager.persist(new User("cart.owner@example.com", "hash", "Cart Owner"));
        cleanCode = entityManager.persist(new Book("9780000000301", "Clean Code", "Author", new BigDecimal("32.99")));
        pragmatic = entityManager.persist(new Book("9780000000302", "Pragmatic", "Author", new BigDecimal("39.99")));
        entityManager.flush();
    }

    @Test
    void savesCartWithItems_andLoadsThemWithBooksInOneQuery() {
        Cart cart = new Cart(user.getId());
        cart.addItem(cleanCode, 2);
        cart.addItem(pragmatic, 1);
        cartRepository.saveAndFlush(cart);
        entityManager.clear();

        Cart loaded = cartRepository.findByUserIdWithItems(user.getId()).orElseThrow();

        assertThat(Hibernate.isInitialized(loaded.getItems())).isTrue();
        assertThat(loaded.getItems())
                .allSatisfy(item ->
                        assertThat(Hibernate.isInitialized(item.getBook())).isTrue());
        assertThat(loaded.getItems()).extracting(CartItem::getQuantity).containsExactly(2, 1);
        assertThat(loaded.total()).isEqualByComparingTo("105.97");
        assertThat(cartRepository.findByUserIdWithItems(999_999L)).isEmpty();
    }

    @Test
    void removedItem_isDeletedFromDatabase() {
        Cart cart = new Cart(user.getId());
        cart.addItem(cleanCode, 2);
        cart.addItem(pragmatic, 1);
        cartRepository.saveAndFlush(cart);
        entityManager.clear();

        Cart loaded = cartRepository.findByUserIdWithItems(user.getId()).orElseThrow();
        loaded.removeItem(cleanCode.getId());
        cartRepository.saveAndFlush(loaded);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cart_items", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void changingItems_incrementsTheCartVersion() {
        Cart cart = cartRepository.saveAndFlush(new Cart(user.getId()));
        long before = cart.getVersion();

        cart.addItem(cleanCode, 1);
        cartRepository.saveAndFlush(cart);

        assertThat(cart.getVersion()).isGreaterThan(before);
    }

    @Test
    void secondCartForSameUser_isRejected() {
        cartRepository.saveAndFlush(new Cart(user.getId()));

        assertThatThrownBy(() -> cartRepository.saveAndFlush(new Cart(user.getId())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void database_rejectsInvalidQuantityAndDuplicateBookLines() {
        Cart cart = new Cart(user.getId());
        cart.addItem(cleanCode, 1);
        Long cartId = cartRepository.saveAndFlush(cart).getId();

        // the domain already prevents these; the constraints are the last line of defence
        assertThatThrownBy(() -> jdbcTemplate.update(
                        "INSERT INTO cart_items (cart_id, book_id, quantity) VALUES (?, ?, 0)",
                        cartId,
                        pragmatic.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                        "INSERT INTO cart_items (cart_id, book_id, quantity) VALUES (?, ?, 1)",
                        cartId,
                        cleanCode.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
