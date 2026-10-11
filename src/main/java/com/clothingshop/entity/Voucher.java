package com.clothingshop.entity;

import com.clothingshop.util.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

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

    /** Optimistic locking version (column {@code version}); incremented by Hibernate on every update. */
    @Version
    @Column(nullable = false)
    private Long version;

    protected Voucher() {
    }

    public Voucher(String code) {
        this.code = code;
    }

    /**
     * Voucher.isUsable() (FR-14 step 2): uses left, the policy allows this customer now (VoucherPolicy.isAvailable()), and
     * the product amount reaches the minimum order value (BR-07).
     */
    public boolean isUsable(LocalDateTime now, CustomerLevel level, BigDecimal productAmount) {
        return quantity > 0 && policy.isAvailable(now, level) && productAmount.compareTo(policy.getMinOrderValue()) >= 0;
    }

    /**
     * Voucher.calcDiscount() (BR-07): PERCENT gives the percentage of the product amount rounded to thousands (BR-21) and
     * capped by maxDiscount when set; FIXED_AMOUNT gives the voucher value. Never more than the product amount.
     */
    public BigDecimal calcDiscount(BigDecimal productAmount) {
        BigDecimal discount;
        if (discountType == DiscountType.PERCENT) {
            discount = Money.roundToThousand(productAmount.multiply(discountValue).divide(BigDecimal.valueOf(100)));
            if (policy.getMaxDiscount() != null && discount.compareTo(policy.getMaxDiscount()) > 0) {
                discount = policy.getMaxDiscount();
            }
        } else {
            discount = discountValue;
        }
        return discount.min(productAmount).setScale(0, RoundingMode.HALF_UP);
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

    public Long getVersion() {
        return version;
    }
}
