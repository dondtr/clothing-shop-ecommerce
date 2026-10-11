package com.clothingshop.service;

import com.clothingshop.entity.CustomerLevel;
import com.clothingshop.entity.DiscountType;
import com.clothingshop.entity.EligibleCustomer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

/**
 * Settings of the automatic vouchers (section 3.5, Q48), kept in a JSON file: monthly vouchers per membership level
 * (FR-37) and holiday campaigns (FR-38). New values apply from the next issue only (Q64); {@code enabledAt} records when a
 * level or campaign was last turned on, so one turned on mid-period gets vouchers from the next period (Q69).
 */
public record VoucherSettings(Map<CustomerLevel, Monthly> monthly, Map<Holiday, Campaign> holidays) {

    /**
     * Monthly voucher of one level; code {@code <LEVEL>-<MMYYYY>}, eligible customers = the level.
     *
     * @param startDay first day of validity in the month (from 00:00)
     * @param endDay   last day of validity (until 23:59:59), {@code null} = last day of the month; a day beyond the month
     *                 means its last day
     */
    public record Monthly(boolean enabled, LocalDateTime enabledAt, DiscountType discountType, BigDecimal discountValue,
                          BigDecimal minOrderValue, BigDecimal maxDiscount, Integer quantity, Integer startDay, Integer endDay) {
    }

    /**
     * One holiday campaign; code = prefix + number of the campaign (2 digits).
     *
     * @param startOffsetDays days from the holiday to the start (0 = 00:00 on the holiday)
     * @param durationDays    validity in whole days of 24 hours
     */
    public record Campaign(boolean enabled, LocalDateTime enabledAt, DiscountType discountType, BigDecimal discountValue,
                           Integer quantity, BigDecimal minOrderValue, BigDecimal maxDiscount, EligibleCustomer eligibleCustomer,
                           Integer startOffsetDays, Integer durationDays) {
    }

    /**
     * The values of the specification for a new file (Q25, Q26, Q49, Q67): LOYAL 15% and REGULAR 50.000 VND each month,
     * 10.000 uses, valid all month; CASUAL off. Holiday campaigns off and without type or value, which the manager must
     * enter before turning one on; 20 uses, minimum 0, maximum 500.000, every customer, 5 days from the holiday.
     */
    public static VoucherSettings defaults(LocalDateTime now) {
        Map<CustomerLevel, Monthly> monthly = new EnumMap<>(CustomerLevel.class);
        monthly.put(CustomerLevel.CASUAL, new Monthly(false, null, null, null, BigDecimal.ZERO, null, 10000, 1, null));
        monthly.put(CustomerLevel.REGULAR, new Monthly(true, now, DiscountType.FIXED_AMOUNT, new BigDecimal("50000"),
                BigDecimal.ZERO, null, 10000, 1, null));
        monthly.put(CustomerLevel.LOYAL, new Monthly(true, now, DiscountType.PERCENT, new BigDecimal("15"),
                BigDecimal.ZERO, null, 10000, 1, null));
        Map<Holiday, Campaign> holidays = new EnumMap<>(Holiday.class);
        for (Holiday h : Holiday.values()) {
            holidays.put(h, new Campaign(false, null, null, null, 20, BigDecimal.ZERO, new BigDecimal("500000"),
                    EligibleCustomer.ALL, 0, 5));
        }
        return new VoucherSettings(monthly, holidays);
    }

    /** These settings with entries missing from an older or hand-edited file taken from {@code defaults}. */
    VoucherSettings completedWith(VoucherSettings defaults) {
        Map<CustomerLevel, Monthly> m = new EnumMap<>(defaults.monthly());
        if (monthly != null) {
            monthly.forEach((level, value) -> {
                if (level != null && value != null) {
                    m.put(level, value);
                }
            });
        }
        Map<Holiday, Campaign> h = new EnumMap<>(defaults.holidays());
        if (holidays != null) {
            holidays.forEach((holiday, value) -> {
                if (holiday != null && value != null) {
                    h.put(holiday, value);
                }
            });
        }
        return new VoucherSettings(m, h);
    }
}
