package com.clothingshop.util;

import java.time.LocalDate;

/**
 * Vietnamese lunar calendar (time zone GMT+7), to find the solar date of Tết (1st day of the 1st lunar month) and Giỗ Tổ
 * Hùng Vương (10th day of the 3rd lunar month) for the holiday vouchers (FR-38, Q26). Astronomical algorithm by Hồ Ngọc
 * Đức (new moons and solar terms), as used by Vietnamese calendar software.
 */
public final class LunarCalendar {

    private static final double TIME_ZONE = 7.0;

    private LunarCalendar() {
    }

    /** Solar date of a day of a (non-leap) lunar month of {@code lunarYear}. */
    public static LocalDate toSolar(int lunarDay, int lunarMonth, int lunarYear) {
        int a11;
        int b11;
        if (lunarMonth < 11) {
            a11 = lunarMonth11(lunarYear - 1);
            b11 = lunarMonth11(lunarYear);
        } else {
            a11 = lunarMonth11(lunarYear);
            b11 = lunarMonth11(lunarYear + 1);
        }
        int k = (int) Math.floor(0.5 + (a11 - 2415021.076998695) / 29.530588853);
        int off = lunarMonth - 11;
        if (off < 0) {
            off += 12;
        }
        if (b11 - a11 > 365) {   // this lunar year has a leap month
            int leapOff = leapMonthOffset(a11);
            if (off >= leapOff) {
                off += 1;
            }
        }
        int monthStart = newMoonDay(k + off);
        return fromJulianDay(monthStart + lunarDay - 1);
    }

    static int julianDay(int dd, int mm, int yy) {
        int a = (14 - mm) / 12;
        int y = yy + 4800 - a;
        int m = mm + 12 * a - 3;
        int jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
        if (jd < 2299161) {
            jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083;
        }
        return jd;
    }

    static LocalDate fromJulianDay(int jd) {
        int b;
        int c;
        if (jd > 2299160) {
            int a = jd + 32044;
            b = (4 * a + 3) / 146097;
            c = a - (b * 146097) / 4;
        } else {
            b = 0;
            c = jd + 32082;
        }
        int d = (4 * c + 3) / 1461;
        int e = c - (1461 * d) / 4;
        int m = (5 * e + 2) / 153;
        int day = e - (153 * m + 2) / 5 + 1;
        int month = m + 3 - 12 * (m / 10);
        int year = b * 100 + d - 4800 + m / 10;
        return LocalDate.of(year, month, day);
    }

    /** Julian day number (local) of the k-th new moon after 1900-01-01. */
    private static int newMoonDay(int k) {
        return (int) Math.floor(newMoon(k) + 0.5 + TIME_ZONE / 24);
    }

    private static double newMoon(int k) {
        double t = k / 1236.85;
        double t2 = t * t;
        double t3 = t2 * t;
        double dr = Math.PI / 180;
        double jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3;
        jd1 += 0.00033 * Math.sin((166.56 + 132.87 * t - 0.009173 * t2) * dr);
        double m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3;
        double mpr = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3;
        double f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3;
        double c1 = (0.1734 - 0.000393 * t) * Math.sin(m * dr) + 0.0021 * Math.sin(2 * dr * m);
        c1 = c1 - 0.4068 * Math.sin(mpr * dr) + 0.0161 * Math.sin(dr * 2 * mpr);
        c1 = c1 - 0.0004 * Math.sin(dr * 3 * mpr);
        c1 = c1 + 0.0104 * Math.sin(dr * 2 * f) - 0.0051 * Math.sin(dr * (m + mpr));
        c1 = c1 - 0.0074 * Math.sin(dr * (m - mpr)) + 0.0004 * Math.sin(dr * (2 * f + m));
        c1 = c1 - 0.0004 * Math.sin(dr * (2 * f - m)) - 0.0006 * Math.sin(dr * (2 * f + mpr));
        c1 = c1 + 0.0010 * Math.sin(dr * (2 * f - mpr)) + 0.0005 * Math.sin(dr * (2 * mpr + m));
        double deltaT = t < -11
                ? 0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t * t3
                : -0.000278 + 0.000265 * t + 0.000262 * t2;
        return jd1 + c1 - deltaT;
    }

    /** Solar longitude, in 30° sectors (0 – 11), at local midnight of day {@code jdn}. */
    private static int sunLongitude(int jdn) {
        double t = (jdn - 0.5 - TIME_ZONE / 24 - 2451545.0) / 36525;
        double t2 = t * t;
        double dr = Math.PI / 180;
        double m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2;
        double l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2;
        double dl = (1.914600 - 0.004817 * t - 0.000014 * t2) * Math.sin(dr * m);
        dl = dl + (0.019993 - 0.000101 * t) * Math.sin(dr * 2 * m) + 0.000290 * Math.sin(dr * 3 * m);
        double l = (l0 + dl) * dr;
        l = l - Math.PI * 2 * Math.floor(l / (Math.PI * 2));
        return (int) Math.floor(l / Math.PI * 6);
    }

    /** Day of the start of lunar month 11 (the month containing the winter solstice) of year {@code yy}. */
    private static int lunarMonth11(int yy) {
        int off = julianDay(31, 12, yy) - 2415021;
        int k = (int) Math.floor(off / 29.530588853);
        int nm = newMoonDay(k);
        if (sunLongitude(nm) >= 9) {
            nm = newMoonDay(k - 1);
        }
        return nm;
    }

    /** Offset, from lunar month 11, of the leap month (the first month without a major solar term). */
    private static int leapMonthOffset(int a11) {
        int k = (int) Math.floor((a11 - 2415021.076998695) / 29.530588853 + 0.5);
        int i = 1;
        int arc = sunLongitude(newMoonDay(k + i));
        int last;
        do {
            last = arc;
            i++;
            arc = sunLongitude(newMoonDay(k + i));
        } while (arc != last && i < 14);
        return i - 1;
    }
}
