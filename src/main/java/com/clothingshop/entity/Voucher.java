package com.clothingshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Voucher (table {@code Voucher}) with its usage policy (table {@code VoucherPolicy}).
 *
 * <p>The policy has no life cycle of its own (1 - 1 composition sharing the key {@code voucherCode}),
 * so it is mapped as an embeddable stored in the secondary table {@code VoucherPolicy} (Phase 1, D3).
 */
@Entity
@Table(name = "Voucher")
@SecondaryTable(name = "VoucherPolicy", pkJoinColumns = @PrimaryKeyJoinColumn(name = "voucherCode"))
public class Voucher {

    /** Voucher code entered by customers; assigned, not generated. */
    @Id
    @Column(length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    /** Remaining number of uses. */
    @Column(nullable = false)
    private int quantity;

    @Embedded
    private VoucherPolicy policy;

    protected Voucher() {
    }

    public Voucher(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public VoucherPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(VoucherPolicy policy) {
        this.policy = policy;
    }
}
