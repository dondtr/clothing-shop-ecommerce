package com.clothingshop.entity;

/**
 * Status of a return request.
 */
public enum ReturnRequestStatus {
    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    REJECTED("Bị từ chối");

    private final String label;

    ReturnRequestStatus(String label) {
        this.label = label;
    }

    /** Vietnamese name shown on the pages. */
    public String getLabel() {
        return label;
    }
}
