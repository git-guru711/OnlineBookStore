package com.example.bookstore.repository;

import com.example.bookstore.entity.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, Long> {

    /** The cart with its items and their books in one query (no N+1 when building the response). */
    @Query("""
            SELECT c FROM Cart c
              LEFT JOIN FETCH c.items i
              LEFT JOIN FETCH i.book
             WHERE c.userId = :userId
            """)
    Optional<Cart> findByUserIdWithItems(@Param("userId") Long userId);
}
