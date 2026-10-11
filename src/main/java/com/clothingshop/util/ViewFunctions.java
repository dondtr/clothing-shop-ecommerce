package com.clothingshop.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatting functions for the JSP views, used with the prefix {@code f} (declared in {@code /WEB-INF/shop.tld}).
 * Numbers follow Vietnamese conventions: "." between thousands, "," before decimals.
 */
public final class ViewFunctions {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ViewFunctions() {
    }

    /** 1250000 → "1.250.000 ₫". */
    public static String vnd(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(amount) + " ₫";
    }

    /** Short amount for chart axes: 0, 500k, 1,5tr, 2 tỷ (rounded to one decimal). */
    public static String vndShort(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        BigDecimal thousand = BigDecimal.valueOf(1_000);
        BigDecimal million = BigDecimal.valueOf(1_000_000);
        BigDecimal billion = BigDecimal.valueOf(1_000_000_000);
        if (amount.abs().compareTo(thousand) < 0) {
            return number(amount.setScale(0, RoundingMode.HALF_UP));
        }
        if (amount.abs().compareTo(million) < 0) {
            return number(amount.divide(thousand, 1, RoundingMode.HALF_UP)) + "k";
        }
        if (amount.abs().compareTo(billion) < 0) {
            return number(amount.divide(million, 1, RoundingMode.HALF_UP)) + "tr";
        }
        return number(amount.divide(billion, 1, RoundingMode.HALF_UP)) + " tỷ";
    }

    /** A percentage or a rating without useless zeros: 20.00 → "20", 4.50 → "4,5". */
    public static String number(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    /** 4.4 → "★★★★☆" (rounded to whole stars, out of 5). */
    public static String stars(double rating) {
        int full = (int) Math.max(0, Math.min(5, Math.round(rating)));
        return "★".repeat(full) + "☆".repeat(5 - full);
    }

    /** "10/10/2026 08:30" (times are stored in Vietnam time, section 3.1). */
    public static String dateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME.format(value);
    }

    /** "10/10/2026". */
    public static String date(LocalDate value) {
        return value == null ? "" : DATE.format(value);
    }

    /**
     * Address of a product image: absolute http(s) URLs are used as they are, other values are paths inside the
     * application (for example {@code assets/images/cloth_1.jpg}). The view still escapes the result.
     */
    public static String imageUrl(String contextPath, String imgUrl) {
        if (imgUrl == null || imgUrl.isBlank()) {
            return "";
        }
        String lower = imgUrl.toLowerCase(Locale.ROOT);
        if (lower.startsWith("https://") || lower.startsWith("http://")) {
            return imgUrl;
        }
        return contextPath + "/" + (imgUrl.startsWith("/") ? imgUrl.substring(1) : imgUrl);
    }
}
