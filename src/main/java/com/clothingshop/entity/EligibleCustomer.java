package com.clothingshop.entity;

/**
 * Customers allowed to use a voucher: one membership level or ALL.
 */
public enum EligibleCustomer {
    CASUAL("Hạng Vãng lai"),
    REGULAR("Hạng Thường xuyên"),
    LOYAL("Hạng Trung thành"),
    ALL("Mọi khách hàng");

    private final String label;

    EligibleCustomer(String label) {
        this.label = label;
    }

    /** Vietnamese name shown on the pages. */
    public String getLabel() {
        return label;
    }
}
