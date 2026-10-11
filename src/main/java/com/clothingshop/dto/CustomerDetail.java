package com.clothingshop.dto;

import com.clothingshop.entity.CustomerLevel;
import com.clothingshop.entity.Gender;
import com.clothingshop.entity.ReturnRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One customer as staff see it (FR-42 step 2).
 *
 * @param paidTotal Σ totalAmount of the orders paid (PAID), the basis of the level (FR-27)
 * @param version   current version of the account, sent back with the manager's level forms (section 3.1)
 */
public record CustomerDetail(int id, String userName, String fullName, String email, String phone, Gender gender,
                             LocalDate dob, LocalDateTime createdAt, CustomerLevel level, boolean autoDowngrade,
                             long version, BigDecimal paidTotal, List<OrderRow> orders, List<ReturnItem> returns) {

    /** A return request of the customer. */
    public record ReturnItem(int id, int orderId, LocalDateTime requestedAt, String reason, ReturnRequestStatus status,
                             LocalDateTime handledAt) {
    }
}
