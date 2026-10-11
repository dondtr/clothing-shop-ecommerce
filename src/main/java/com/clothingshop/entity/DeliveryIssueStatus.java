package com.clothingshop.entity;

/**
 * Status of a delivery issue reported by a customer.
 */
public enum DeliveryIssueStatus {
    OPEN("Đang chờ xử lý"),
    RESOLVED("Đã xử lý");

    private final String label;

    DeliveryIssueStatus(String label) {
        this.label = label;
    }

    /** Vietnamese name shown on the pages. */
    public String getLabel() {
        return label;
    }
}
