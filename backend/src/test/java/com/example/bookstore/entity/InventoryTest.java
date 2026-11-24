package com.example.bookstore.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bookstore.model.StockStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InventoryTest {

    @Test
    void zeroQuantity_isOutOfStock() {
        Inventory inventory = new Inventory(1L, 0);

        assertThat(inventory.isInStock()).isFalse();
        assertThat(inventory.status()).isEqualTo(StockStatus.OUT_OF_STOCK);
    }

    @Test
    void positiveQuantity_isInStock() {
        Inventory inventory = new Inventory(1L, 5);

        assertThat(inventory.isInStock()).isTrue();
        assertThat(inventory.status()).isEqualTo(StockStatus.IN_STOCK);
    }

    @ParameterizedTest(name = "available {0}, requested {1} -> {2}")
    @CsvSource({
        "5, 1, true", // plenty
        "5, 5, true", // exactly the last copies
        "5, 6, false", // one too many
        "0, 1, false" // sold out
    })
    void canFulfil_respectsAvailableQuantity(int available, int requested, boolean expected) {
        Inventory inventory = new Inventory(1L, available);

        assertThat(inventory.canFulfil(requested)).isEqualTo(expected);
    }
}
