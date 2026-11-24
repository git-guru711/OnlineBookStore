package com.example.bookstore.repository;

import com.example.bookstore.entity.Order;
import com.example.bookstore.model.OrderSummaryResponse;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Scoped to the owner: another user's order id simply is not found. */
    @Query("""
            SELECT o FROM Order o
              LEFT JOIN FETCH o.items
             WHERE o.id = :id AND o.userId = :userId
            """)
    Optional<Order> findByIdAndUserIdWithItems(@Param("id") Long id, @Param("userId") Long userId);

    @Query("""
            SELECT o FROM Order o
              LEFT JOIN FETCH o.items
             WHERE o.userId = :userId AND o.idempotencyKey = :key
            """)
    Optional<Order> findByUserIdAndIdempotencyKeyWithItems(@Param("userId") Long userId, @Param("key") String key);

    @Query(value = """
            SELECT new com.example.bookstore.model.OrderSummaryResponse(
                       o.id, o.status, CAST(SUM(i.quantity) AS Integer), o.totalAmount, o.createdAt)
              FROM Order o JOIN o.items i
             WHERE o.userId = :userId
             GROUP BY o.id, o.status, o.totalAmount, o.createdAt
             ORDER BY o.createdAt DESC, o.id DESC
            """, countQuery = "SELECT COUNT(o) FROM Order o WHERE o.userId = :userId")
    Page<OrderSummaryResponse> findSummariesByUserId(@Param("userId") Long userId, Pageable pageable);
}
