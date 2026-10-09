package com.clothingshop.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer-specific data (table {@code Customer}, primary key {@code userId} shared with {@link User}).
 */
@Entity
@Table(name = "Customer")
@PrimaryKeyJoinColumn(name = "userId")
public class Customer extends User {

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerLevel customerLevel;

    /** TRUE: the system may downgrade the level automatically (ERD default TRUE). */
    @Column(nullable = false)
    private boolean autoDowngrade = true;

    /** Saved delivery addresses; owned by the customer, removed together with it. */
    @OneToMany(mappedBy = "customer", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

    protected Customer() {
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public CustomerLevel getCustomerLevel() {
        return customerLevel;
    }

    public void setCustomerLevel(CustomerLevel customerLevel) {
        this.customerLevel = customerLevel;
    }

    public boolean isAutoDowngrade() {
        return autoDowngrade;
    }

    public void setAutoDowngrade(boolean autoDowngrade) {
        this.autoDowngrade = autoDowngrade;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    /** Adds an address and keeps both sides of the association in sync. */
    public void addAddress(Address address) {
        addresses.add(address);
        address.setCustomer(this);
    }

    /** Removes an address; orphan removal deletes it on flush. */
    public void removeAddress(Address address) {
        addresses.remove(address);
        address.setCustomer(null);
    }
}
