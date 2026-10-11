package com.clothingshop.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * The cart page (FR-09 step 3): each line at the current price after discount, and the total of the lines that can be
 * ordered now. Lines that cannot be ordered stay in the cart with their reason (FR-09 rules).
 *
 * @param total          Σ subtotal of the orderable lines, before vouchers and the shipping fee
 * @param orderableCount number of orderable lines
 * @param hasProblems    at least one line cannot be ordered as it is
 */
public record CartView(List<Line> lines, BigDecimal total, int orderableCount, boolean hasProblems) {

    public static final CartView EMPTY = new CartView(List.of(), BigDecimal.ZERO, 0, false);

    /** Why a line can or cannot be ordered. */
    public enum Status {
        OK,
        /** The row was set to not on sale (FR-32); it stays in the cart but cannot be ordered. */
        NOT_ON_SALE,
        OUT_OF_STOCK,
        /** The quantity is above the stock left. */
        NOT_ENOUGH_STOCK
    }

    /**
     * One row of the cart ("CartItem").
     *
     * @param unitPrice price after the discount in effect now
     * @param subtotal  unitPrice × quantity (CartItem.calcSubtotal())
     */
    public record Line(int productId, String name, String color, String size, String imgUrl, BigDecimal basePrice,
                       BigDecimal unitPrice, boolean onSale, int quantity, BigDecimal subtotal, int stockQuantity,
                       Status status, boolean orderable) {
    }
}
