package com.clothingshop.entity;

/**
 * Payment method of an order.
 */
public enum PaymentMethod {
    COD("Thanh toán khi nhận hàng (COD)"),
    VNPAY("Thanh toán trực tuyến VNPAY");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    /** Vietnamese name shown on the pages. */
    public String getLabel() {
        return label;
    }
}
