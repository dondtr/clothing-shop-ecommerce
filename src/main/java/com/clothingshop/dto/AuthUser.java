package com.clothingshop.dto;

import java.io.Serializable;

/**
 * The signed-in user kept in the HTTP session under {@link #SESSION_KEY} (FR-02).
 * Holds only what the views and access checks need, never the password hash.
 */
public final class AuthUser implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SESSION_KEY = "auth";

    /** Role derived from the account type (BR-27): a customer, or an employee with role STAFF or ADMIN. */
    public enum Role { CUSTOMER, STAFF, ADMIN }

    private final Integer id;
    private final String userName;
    private final String fullName;
    private final Role role;

    public AuthUser(Integer id, String userName, String fullName, Role role) {
        this.id = id;
        this.userName = userName;
        this.fullName = fullName;
        this.role = role;
    }

    public Integer getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public boolean isCustomer() {
        return role == Role.CUSTOMER;
    }

    public boolean isEmployee() {
        return role != Role.CUSTOMER;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
