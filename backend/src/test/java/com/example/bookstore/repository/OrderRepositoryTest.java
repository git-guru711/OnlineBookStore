package com.example.bookstore.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Cart;
import com.example.bookstore.entity.Order;
import com.example.bookstore.entity.OrderItem;
import com.example.bookstore.entity.User;
import com.example.bookstore.model.OrderStatus;
import com.example.bookstore.model.OrderSummaryResponse;
import com.example.bookstore.support.RepositoryTest;
import java.math.BigDecimal;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@RepositoryTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User owner;
    private User otherUser;
    private Book cleanCode;
    private Book pragmatic;

    @BeforeEach
    void setUp() {
        owner = entityManager.persist(new User("order.owner@example.com", "hash", "Owner"));
        otherUser = entityManager.persist(new User("order.other@example.com", "hash", "Other"));
        cleanCode = entityManager.persist(new Book("9780000000401", "Clean Code", "Author", new BigDecimal("32.99")));
        pragmatic = entityManager.persist(new Book("9780000000402", "Pragmatic", "Author", new BigDecimal("39.99")));
        entityManager.flush();
    }

    @Test
    void savesOrderWithItems_andFindsItOnlyForItsOwner() {
        Order saved = orderRepository.saveAndFlush(order(owner, cleanCode, 2, pragmatic, 1));
        entityManager.clear();

        Order loaded = orderRepository
                .findByIdAndUserIdWithItems(saved.getId(), owner.getId())
                .orElseThrow();

        assertThat(loaded.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getTotalAmount()).isEqualByComparingTo("105.97");
        assertThat(Hibernate.isInitialized(loaded.getItems())).isTrue();
        assertThat(loaded.getItems()).extracting(OrderItem::getTitle).containsExactly("Clean Code", "Pragmatic");
        assertThat(orderRepository.findByIdAndUserIdWithItems(saved.getId(), otherUser.getId()))
                .isEmpty();
    }

    @Test
    void findSummaries_returnsOwnersOrdersNewestFirst_withTotalQuantity() {
        Order first = orderRepository.saveAndFlush(order(owner, cleanCode, 1, pragmatic, 1));
        Order second = orderRepository.saveAndFlush(order(owner, cleanCode, 3, pragmatic, 2));
        orderRepository.saveAndFlush(order(otherUser, cleanCode, 1, pragmatic, 1));
        entityManager.clear();

        Page<OrderSummaryResponse> page = orderRepository.findSummariesByUserId(owner.getId(), PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(OrderSummaryResponse::id)
                .containsExactly(second.getId(), first.getId());
        assertThat(page.getContent().get(0).totalQuantity()).isEqualTo(5);
        assertThat(page.getContent().get(0).totalAmount()).isEqualByComparingTo("178.95");
    }

    @Test
    void order_survivesItsBookBeingDeletedFromTheCatalog() {
        Order saved = orderRepository.saveAndFlush(order(owner, cleanCode, 1, pragmatic, 1));
        entityManager
                .getEntityManager()
                .createQuery("DELETE FROM Book b WHERE b.id = :id")
                .setParameter("id", cleanCode.getId())
                .executeUpdate();
        entityManager.clear();

        Order loaded = orderRepository
                .findByIdAndUserIdWithItems(saved.getId(), owner.getId())
                .orElseThrow();

        assertThat(loaded.getItems()).extracting(OrderItem::getTitle).contains("Clean Code");
    }

    @Test
    void idempotencyKey_isUniquePerUser_soTheDatabaseRejectsASecondOrderWithTheSameKey() {
        orderRepository.saveAndFlush(keyedOrder(owner, "key-0001-shared"));

        assertThatThrownBy(() -> orderRepository.saveAndFlush(keyedOrder(owner, "key-0001-shared")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void idempotencyKey_sameKeyForTwoUsers_andOrdersWithoutKey_areAllowed() {
        orderRepository.saveAndFlush(keyedOrder(owner, "key-0002-shared"));
        orderRepository.saveAndFlush(keyedOrder(otherUser, "key-0002-shared")); // another user's purchase
        orderRepository.saveAndFlush(keyedOrder(owner, null)); // no key: never clashes
        orderRepository.saveAndFlush(keyedOrder(owner, null));
        entityManager.clear();

        assertThat(orderRepository.findByUserIdAndIdempotencyKeyWithItems(owner.getId(), "key-0002-shared"))
                .get()
                .extracting(Order::getUserId)
                .isEqualTo(owner.getId());
        assertThat(orderRepository.findByUserIdAndIdempotencyKeyWithItems(owner.getId(), "key-unknown"))
                .isEmpty();
    }

    private Order keyedOrder(User user, String idempotencyKey) {
        Cart cart = new Cart(user.getId());
        cart.addItem(cleanCode, 1);
        return Order.placeFrom(cart, idempotencyKey);
    }

    private static Order order(User user, Book firstBook, int firstQuantity, Book secondBook, int secondQuantity) {
        Cart cart = new Cart(user.getId());
        cart.addItem(firstBook, firstQuantity);
        cart.addItem(secondBook, secondQuantity);
        return Order.placeFrom(cart);
    }
}
