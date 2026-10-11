package com.clothingshop.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    private static BigDecimal vnd(String value) {
        return new BigDecimal(value);
    }

    @Test
    void roundsToTheNearestThousandHalfUp() {
        assertEquals(vnd("124000"), Money.roundToThousand(vnd("123546")));   // BR-21 example
        assertEquals(vnd("123000"), Money.roundToThousand(vnd("123499.99")));
        assertEquals(vnd("124000"), Money.roundToThousand(vnd("123500")));   // from 500 VND rounds up (Q46)
        assertEquals(vnd("0"), Money.roundToThousand(vnd("0")));
    }

    @Test
    void subtractsAPercentageThenRounds() {
        assertEquals(vnd("128000"), Money.minusPercent(vnd("150000.00"), vnd("15.00")));  // 127.500 → 128.000
        assertEquals(vnd("159000"), Money.minusPercent(vnd("199000"), vnd("20")));        // 159.200 → 159.000
        assertEquals(vnd("150000"), Money.minusPercent(vnd("150000.00"), BigDecimal.ZERO));
        assertEquals(vnd("0"), Money.minusPercent(vnd("150000"), vnd("100")));
        assertEquals(vnd("133000"), Money.minusPercent(vnd("199000"), vnd("33.33")));      // 132.673,3 → 133.000
    }
}
