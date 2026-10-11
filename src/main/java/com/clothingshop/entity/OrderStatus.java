package com.clothingshop.entity;

/**
 * Life cycle status of an order.
 */
public enum OrderStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    SHIPPING("Đang giao"),
    COMPLETED("Giao thành công"),
    CANCELLED("Đã hủy");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    /** Vietnamese name shown on the pages. */
    public String getLabel() {
        return label;
    }
}
