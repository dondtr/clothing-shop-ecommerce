package com.clothingshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Employee-specific data (table {@code Employee}, primary key {@code userId} shared with {@link User}).
 * Role ADMIN is the manager; role STAFF is a regular employee.
 */
@Entity
@Table(name = "Employee")
@PrimaryKeyJoinColumn(name = "userId")
public class Employee extends User {

    @Column(nullable = false)
    private LocalDate hireDate;

    /** FALSE: the account is locked and cannot log in. */
    @Column(nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeRole role;

    protected Employee() {
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public EmployeeRole getRole() {
        return role;
    }

    public void setRole(EmployeeRole role) {
        this.role = role;
    }
}
