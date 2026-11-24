package com.example.bookstore.repository;

import com.example.bookstore.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /** Atomic check-and-decrement, so two buyers can never both take the last copy. Returns 1 if decreased, 0 if not. */
    @Transactional
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE Inventory i
               SET i.availableQuantity = i.availableQuantity - :quantity,
                   i.version = i.version + 1
             WHERE i.bookId = :bookId
               AND i.availableQuantity >= :quantity
            """)
    int decrementIfAvailable(@Param("bookId") Long bookId, @Param("quantity") int quantity);
}
