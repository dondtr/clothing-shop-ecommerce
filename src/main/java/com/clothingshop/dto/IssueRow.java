package com.clothingshop.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * An open "Chưa thấy hàng đâu" report for staff (FR-42 step 3, FR-21).
 *
 * @param reshipPossible every line of the order still has enough stock to send it again (Q54)
 */
public record IssueRow(int issueId, int orderId, int customerId, String customerName, String customerPhone,
                       String customerEmail, LocalDateTime reportedAt, LocalDateTime shippedAt, List<Line> lines,
                       boolean reshipPossible) {

    /** A line of the order with the stock left. */
    public record Line(String name, String color, String size, int quantity, int stockQuantity) {
    }
}
