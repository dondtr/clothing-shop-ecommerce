package com.clothingshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/** Carts kept in memory per customer (section 3.4). */
class InMemoryCartStoreTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-10T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private final InMemoryCartStore store = new InMemoryCartStore(clock);

    private void add(int userId, int productId, int quantity) {
        store.update(userId, cart -> {
            cart.addItem(productId, quantity);
            return null;
        });
    }

    @Test
    void keepsLinesInTheOrderAddedAndAddsQuantitiesOfTheSameRow() {
        add(1, 30, 1);
        add(1, 10, 2);
        add(1, 30, 3);
        assertEquals(List.of(30, 10), List.copyOf(store.lines(1).keySet()));
        assertEquals(Map.of(30, 4, 10, 2), store.lines(1));
        assertTrue(store.lines(2).isEmpty(), "each customer has an own cart");
    }

    @Test
    void aFailedChangeLeavesTheCartAsItWas() {
        add(1, 10, 2);
        assertThrows(IllegalStateException.class, () -> store.update(1, cart -> {
            cart.addItem(10, 5);
            cart.addItem(20, 1);
            throw new IllegalStateException("rejected");
        }));
        assertEquals(Map.of(10, 2), store.lines(1));
    }

    @Test
    void emptyCartsAreDropped() {
        add(1, 10, 2);
        store.update(1, cart -> {
            cart.removeItem(10);
            return null;
        });
        assertEquals(0, store.size());
    }

    @Test
    void cartsNotUsedFor30DaysAreDropped() {
        add(1, 10, 1);
        add(2, 10, 1);
        clock.advance(Duration.ofDays(29));
        store.lines(2);   // viewing counts as using
        clock.advance(Duration.ofDays(2));
        assertTrue(store.lines(1).isEmpty(), "unused for 31 days");
        assertEquals(Map.of(10, 1), store.lines(2), "used 2 days ago");

        add(3, 10, 1);
        clock.advance(Duration.ofDays(31));
        add(4, 10, 1);    // the hourly clean-up runs on this request
        assertEquals(1, store.size());
    }

    @Test
    void changesFromManyBrowsersAtOnceAreNotLost() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<?>> tasks = new ArrayList<>();
            for (int t = 0; t < 8; t++) {
                tasks.add(pool.submit(() -> {
                    for (int i = 0; i < 1000; i++) {
                        add(1, 10, 1);
                    }
                }));
            }
            for (Future<?> task : tasks) {
                task.get();
            }
        } finally {
            pool.shutdown();
        }
        assertEquals(Map.of(10, 8000), store.lines(1));
    }
}
