package com.clothingshop.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money rules shared by prices, vouchers and order totals: amounts are in VND and every computed amount is rounded to
 * the nearest thousand, half up (BR-21, Q34, Q46).
 */
public final class Money {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Money() {
    }

    /** 123.546 → 124.000, 123.499 → 123.000, 123.500 → 124.000. The result has scale 0. */
    public static BigDecimal roundToThousand(BigDecimal amount) {
        return amount.setScale(-3, RoundingMode.HALF_UP).setScale(0, RoundingMode.UNNECESSARY);
    }

    /**
     * {@code amount − amount × percent / 100}, rounded to thousands (section 3.2, "giá sau giảm").
     * Dividing by 100 is always exact, so no rounding happens before the final one.
     */
    public static BigDecimal minusPercent(BigDecimal amount, BigDecimal percent) {
        return roundToThousand(amount.subtract(amount.multiply(percent).divide(HUNDRED)));
    }
}
