package com.clothingshop.dto;

import com.clothingshop.entity.DiscountType;
import com.clothingshop.entity.EligibleCustomer;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A voucher with its policy, as shown to customers (FR-39) and to the manager (FR-36).
 *
 * @param quantity    uses left
 * @param maxDiscount cap of a PERCENT voucher, {@code null} = no cap
 */
public record VoucherView(String code, DiscountType discountType, BigDecimal discountValue, int quantity,
                          EligibleCustomer eligibleCustomer, BigDecimal minOrderValue, BigDecimal maxDiscount,
                          LocalDateTime startDate, LocalDateTime endDate) {
}
