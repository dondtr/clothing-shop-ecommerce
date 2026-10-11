package com.clothingshop.entity;

/**
 * Status of a payment.
 */
public enum PaymentStatus {
    UNPAID("Chưa thanh toán"),
    PAID("Đã thanh toán"),
    FAILED("Không thành công"),
    REFUND("Đã hoàn tiền");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    /** Vietnamese name shown on the pages. */
    public String getLabel() {
        return label;
    }
}
