package com.clothingshop.service;

import com.clothingshop.util.LunarCalendar;
import java.time.LocalDate;

/**
 * The four holiday campaigns of FR-38 (Q26). The constant name is the code prefix (BR-19). Tết and Giỗ Tổ follow the
 * Vietnamese lunar calendar.
 */
public enum Holiday {
    TET("Tết Nguyên đán (mùng 1 tháng Giêng âm lịch)"),
    GIOTO("Giỗ Tổ Hùng Vương (10/3 âm lịch)"),
    GPMN("Ngày Giải phóng miền Nam (30/04)"),
    QK("Quốc khánh (02/09)");

    private final String label;

    Holiday(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Solar date of the holiday in {@code year}. */
    public LocalDate dateIn(int year) {
        return switch (this) {
            case TET -> LunarCalendar.toSolar(1, 1, year);
            case GIOTO -> LunarCalendar.toSolar(10, 3, year);
            case GPMN -> LocalDate.of(year, 4, 30);
            case QK -> LocalDate.of(year, 9, 2);
        };
    }
}
