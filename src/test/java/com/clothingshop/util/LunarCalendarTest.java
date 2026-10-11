package com.clothingshop.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Solar dates of Tết and Giỗ Tổ checked against the published Vietnamese calendar. */
class LunarCalendarTest {

    @Test
    void tet() {
        assertEquals(LocalDate.of(2023, 1, 22), LunarCalendar.toSolar(1, 1, 2023));
        assertEquals(LocalDate.of(2024, 2, 10), LunarCalendar.toSolar(1, 1, 2024));
        assertEquals(LocalDate.of(2025, 1, 29), LunarCalendar.toSolar(1, 1, 2025));
        assertEquals(LocalDate.of(2026, 2, 17), LunarCalendar.toSolar(1, 1, 2026));
        assertEquals(LocalDate.of(2027, 2, 6), LunarCalendar.toSolar(1, 1, 2027));
    }

    @Test
    void gioToHungVuongIncludingYearsWithALeapMonth() {
        assertEquals(LocalDate.of(2023, 4, 29), LunarCalendar.toSolar(10, 3, 2023), "2023 has a leap 2nd month");
        assertEquals(LocalDate.of(2024, 4, 18), LunarCalendar.toSolar(10, 3, 2024));
        assertEquals(LocalDate.of(2025, 4, 7), LunarCalendar.toSolar(10, 3, 2025), "2025 has a leap 6th month");
        assertEquals(LocalDate.of(2026, 4, 26), LunarCalendar.toSolar(10, 3, 2026));
    }

    @Test
    void julianDaysRoundTrip() {
        LocalDate d = LocalDate.of(2026, 10, 10);
        assertEquals(d, LunarCalendar.fromJulianDay(LunarCalendar.julianDay(10, 10, 2026)));
    }
}
