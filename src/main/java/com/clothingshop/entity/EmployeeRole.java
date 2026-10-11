package com.clothingshop.entity;

/**
 * Role of an employee account: STAFF or ADMIN (manager).
 */
public enum EmployeeRole {
    STAFF("Nhân viên"),
    ADMIN("Quản lý");

    private final String label;

    EmployeeRole(String label) {
        this.label = label;
    }

    /** Vietnamese name shown on the pages. */
    public String getLabel() {
        return label;
    }
}
