package com.clothingshop.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ViewFunctionsTest {

    @Test
    void formatsVietnameseAmountsAndNumbers() {
        assertEquals("1.250.000 ₫", ViewFunctions.vnd(new BigDecimal("1250000.00")));
        assertEquals("0 ₫", ViewFunctions.vnd(BigDecimal.ZERO));
        assertEquals("20", ViewFunctions.number(new BigDecimal("20.00")));
        assertEquals("4,5", ViewFunctions.number(new BigDecimal("4.5")));
        assertEquals("★★★★☆", ViewFunctions.stars(4.4));
        assertEquals("★★★★★", ViewFunctions.stars(4.5));
        assertEquals("10/10/2026 08:05", ViewFunctions.dateTime(LocalDateTime.of(2026, 10, 10, 8, 5)));
    }

    @Test
    void formatsShortAmountsForChartAxesAndDates() {
        assertEquals("0", ViewFunctions.vndShort(BigDecimal.ZERO));
        assertEquals("500k", ViewFunctions.vndShort(new BigDecimal("500000")));
        assertEquals("1,5tr", ViewFunctions.vndShort(new BigDecimal("1500000")));
        assertEquals("2tr", ViewFunctions.vndShort(new BigDecimal("2000000")));
        assertEquals("2,5 tỷ", ViewFunctions.vndShort(new BigDecimal("2500000000")));
        assertEquals("01/10/2026", ViewFunctions.date(LocalDate.of(2026, 10, 1)));
    }

    @Test
    void resolvesImagePathsInsideTheApplication() {
        assertEquals("/shop/assets/images/a.jpg", ViewFunctions.imageUrl("/shop", "assets/images/a.jpg"));
        assertEquals("/shop/assets/images/a.jpg", ViewFunctions.imageUrl("/shop", "/assets/images/a.jpg"));
        assertEquals("https://cdn.example.com/a.jpg", ViewFunctions.imageUrl("/shop", "https://cdn.example.com/a.jpg"));
    }
}
