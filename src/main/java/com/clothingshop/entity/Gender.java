package com.clothingshop.entity;

/**
 * Gender of a user.
 */
public enum Gender {
    FEMALE("Nữ"),
    MALE("Nam"),
    OTHER("Khác");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    /** Vietnamese name shown on the pages. */
    public String getLabel() {
        return label;
    }
}
