package com.clothingshop.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Carts in the server's memory, keyed by userId (section 3.4, option B chosen in Q47): every browser of a customer sees
 * the same cart; carts are lost when the server restarts (accepted in Q47) and only work with a single server.
 *
 * <p>{@link ConcurrentHashMap#compute} runs each change of one customer's cart alone. Each change is applied to a copy,
 * which replaces the cart only when the change succeeds.
 */
public class InMemoryCartStore implements CartStore {

    /** A cart not used for 30 days is dropped (section 3.4). */
    static final Duration MAX_IDLE = Duration.ofDays(30);
    private static final Duration SWEEP_EVERY = Duration.ofHours(1);

    private final ConcurrentHashMap<Integer, Cart> carts = new ConcurrentHashMap<>();
    private final Clock clock;
    private final AtomicReference<Instant> nextSweep;

    public InMemoryCartStore(Clock clock) {
        this.clock = clock;
        this.nextSweep = new AtomicReference<>(clock.instant().plus(SWEEP_EVERY));
    }

    @Override
    public Map<Integer, Integer> lines(int userId) {
        Instant now = clock.instant();
        sweepIfDue(now);
        Map<Integer, Integer> copy = new LinkedHashMap<>();
        carts.computeIfPresent(userId, (id, cart) -> {
            if (isIdle(cart, now)) {
                return null;
            }
            cart.touch(now);   // viewing the cart counts as using it
            copy.putAll(cart.lines());
            return cart;
        });
        return Collections.unmodifiableMap(copy);
    }

    @Override
    public <T> T update(int userId, Function<Cart, T> change) {
        Instant now = clock.instant();
        sweepIfDue(now);
        Object[] result = new Object[1];
        carts.compute(userId, (id, existing) -> {
            Cart working = existing == null || isIdle(existing, now) ? new Cart(now) : existing.copy();
            result[0] = change.apply(working);
            working.touch(now);
            return working.isEmpty() ? null : working;
        });
        @SuppressWarnings("unchecked")
        T value = (T) result[0];
        return value;
    }

    @Override
    public void removeProduct(int productId) {
        for (Integer userId : carts.keySet()) {
            carts.computeIfPresent(userId, (id, cart) -> {
                if (!cart.contains(productId)) {
                    return cart;
                }
                Cart copy = cart.copy();
                copy.removeItem(productId);
                return copy.isEmpty() ? null : copy;
            });
        }
    }

    /** Number of carts in memory. */
    int size() {
        return carts.size();
    }

    private static boolean isIdle(Cart cart, Instant now) {
        return cart.lastUsed().plus(MAX_IDLE).isBefore(now);
    }

    /** Drops idle carts; runs at most once an hour, on the request that finds it due. */
    private void sweepIfDue(Instant now) {
        Instant due = nextSweep.get();
        if (now.isBefore(due) || !nextSweep.compareAndSet(due, now.plus(SWEEP_EVERY))) {
            return;
        }
        for (Integer userId : carts.keySet()) {
            carts.computeIfPresent(userId, (id, cart) -> isIdle(cart, now) ? null : cart);
        }
    }
}
