package com.clothingshop.entity;

/**
 * Fixed product categories (no category management, Q08).
 */
public enum ProductCategory {
    ACCESSORY("Phụ kiện"),
    HAT("Mũ nón"),
    SHIRT("Áo"),
    PANTS("Quần"),
    DRESS("Váy đầm"),
    SHOES("Giày dép");

    private final String label;

    ProductCategory(String label) {
        this.label = label;
    }

    /** Vietnamese name shown in the shop. */
    public String getLabel() {
        return label;
    }
}
