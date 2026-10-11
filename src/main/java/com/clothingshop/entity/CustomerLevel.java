package com.clothingshop.entity;

/**
 * Membership level of a customer.
 */
public enum CustomerLevel {
    CASUAL("Vãng lai"),
    REGULAR("Thường xuyên"),
    LOYAL("Trung thành");

    private final String label;

    CustomerLevel(String label) {
        this.label = label;
    }

    /** Vietnamese name shown on the pages. */
    public String getLabel() {
        return label;
    }
}
