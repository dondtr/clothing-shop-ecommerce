package com.clothingshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Saved delivery address of a customer (table {@code Address}).
 */
@Entity
@Table(name = "Address")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customerId", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 100)
    private String province;

    @Column(nullable = false, length = 100)
    private String ward;

    @Column(nullable = false, length = 255)
    private String street;

    /** Column {@code isDefault}; renamed in Java to follow bean naming conventions. */
    @Column(name = "isDefault", nullable = false)
    private boolean defaultAddress;

    /** Optimistic locking version (column {@code version}); incremented by Hibernate on every update. */
    @Version
    @Column(nullable = false)
    private Long version;

    protected Address() {
    }

    public Address(String province, String ward, String street, boolean defaultAddress) {
        this.province = province;
        this.ward = ward;
        this.street = street;
        this.defaultAddress = defaultAddress;
    }

    /** FR-06 step 2. */
    public void updateAddress(String province, String ward, String street) {
        this.province = province;
        this.ward = ward;
        this.street = street;
    }

    /** "street, ward, province": the text written to Order.address when an order is placed (Q06). */
    public String toAddressString() {
        return join(street, ward, province);
    }

    /** The order address text for the given parts, also used to check its length before saving. */
    public static String join(String street, String ward, String province) {
        return street + ", " + ward + ", " + province;
    }

    public Integer getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }

    public void setDefaultAddress(boolean defaultAddress) {
        this.defaultAddress = defaultAddress;
    }

    public Long getVersion() {
        return version;
    }
}
