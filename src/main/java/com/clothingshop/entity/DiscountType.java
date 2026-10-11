package com.clothingshop.entity;

/**
 * How a voucher discount is calculated.
 */
public enum DiscountType {
    PERCENT("Phần trăm"),
    FIXED_AMOUNT("Số tiền cố định");

    private final String label;

    DiscountType(String label) {
        this.label = label;
    }

    /** Vietnamese name shown on the pages. */
    public String getLabel() {
        return label;
    }
}
