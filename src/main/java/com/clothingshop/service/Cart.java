package com.clothingshop.service;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shopping cart of one customer (FR-09), kept outside the database (Q18, Q47): product row id → quantity, in the order
 * the rows were first added. Not thread-safe; a {@link CartStore} gives each change exclusive access to the cart.
 * The service checks stock and sale status before changing the cart.
 */
public final class Cart {

    private final Map<Integer, Integer> lines = new LinkedHashMap<>();
    private Instant lastUsed;

    Cart(Instant now) {
        this.lastUsed = now;
    }

    /** Adds {@code quantity} of row {@code productId}; a row already in the cart gets the quantities added (FR-09 step 1). */
    public void addItem(int productId, int quantity) {
        lines.merge(productId, quantity, Integer::sum);
    }

    /** Sets the quantity of a row already in the cart (FR-09 step 2). */
    public void updateQuantity(int productId, int quantity) {
        lines.replace(productId, quantity);
    }

    public void removeItem(int productId) {
        lines.remove(productId);
    }

    public void clear() {
        lines.clear();
    }

    public boolean contains(int productId) {
        return lines.containsKey(productId);
    }

    /** Quantity of the row in the cart, 0 when absent. */
    public int quantityOf(int productId) {
        return lines.getOrDefault(productId, 0);
    }

    public int size() {
        return lines.size();
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    /** Read-only view of the lines, in the order they were added. */
    public Map<Integer, Integer> lines() {
        return Collections.unmodifiableMap(lines);
    }

    /** An independent copy, so a store can apply a change and keep the original if the change fails. */
    Cart copy() {
        Cart copy = new Cart(lastUsed);
        copy.lines.putAll(lines);
        return copy;
    }

    Instant lastUsed() {
        return lastUsed;
    }

    void touch(Instant now) {
        lastUsed = now;
    }
}
