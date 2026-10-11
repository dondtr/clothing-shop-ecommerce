package com.clothingshop.entity;

/**
 * How staff resolved a delivery issue.
 */
public enum DeliveryResolution {
    RESHIP("Giao lại hàng"),
    RESOLVED("Đã giải quyết");

    private final String label;

    DeliveryResolution(String label) {
        this.label = label;
    }

    /** Vietnamese name shown on the pages. */
    public String getLabel() {
        return label;
    }
}
