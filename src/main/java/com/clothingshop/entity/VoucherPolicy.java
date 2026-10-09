package com.clothingshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Usage policy of a {@link Voucher}; columns are stored in the secondary table {@code VoucherPolicy}.
 */
@Embeddable
public class VoucherPolicy {

    @Enumerated(EnumType.STRING)
    @Column(table = "VoucherPolicy", nullable = false)
    private EligibleCustomer eligibleCustomer;

    /** Minimum product amount (after product discounts, before shipping) to use the voucher. */
    @Column(table = "VoucherPolicy", nullable = false, precision = 12, scale = 2)
    private BigDecimal minOrderValue;

    @Column(table = "VoucherPolicy", precision = 12, scale = 2)
    private BigDecimal maxDiscount;

    @Column(table = "VoucherPolicy", nullable = false)
    private LocalDateTime startDate;

    @Column(table = "VoucherPolicy", nullable = false)
    private LocalDateTime endDate;

    public VoucherPolicy() {
    }

    public EligibleCustomer getEligibleCustomer() {
        return eligibleCustomer;
    }

    public void setEligibleCustomer(EligibleCustomer eligibleCustomer) {
        this.eligibleCustomer = eligibleCustomer;
    }

    public BigDecimal getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(BigDecimal minOrderValue) {
        this.minOrderValue = minOrderValue;
    }

    public BigDecimal getMaxDiscount() {
        return maxDiscount;
    }

    public void setMaxDiscount(BigDecimal maxDiscount) {
        this.maxDiscount = maxDiscount;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
}
