package com.clothingshop.service;

import java.util.Map;
import java.util.function.Function;

/**
 * Where carts are kept, one per customer (userId), shared by every browser the customer is signed in on (section 3.4).
 * The application uses {@link InMemoryCartStore} (Q47); a Redis store can replace it later without changing the
 * business code (Q59).
 */
public interface CartStore {

    /** A copy of the customer's cart lines (product row id → quantity, in the order added); empty when there is none. */
    Map<Integer, Integer> lines(int userId);

    /**
     * Runs {@code change} on the customer's cart while no other change to that cart can run, so two browsers changing the
     * cart at the same moment cannot corrupt it; the later change wins on the same line. A cart is created when needed and
     * dropped when it becomes empty. If {@code change} throws, the cart is left as it was before the call.
     */
    <T> T update(int userId, Function<Cart, T> change);
}
