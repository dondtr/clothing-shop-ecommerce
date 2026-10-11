package com.clothingshop.dto;

import com.clothingshop.entity.PaymentMethod;
import com.clothingshop.entity.ReturnRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A return request for staff (FR-26). */
public record ReturnRow(int id, int orderId, int customerId, String customerName, BigDecimal totalAmount,
                        PaymentMethod method, LocalDateTime deliveredAt, LocalDateTime requestedAt, String reason,
                        ReturnRequestStatus status, String handledBy, LocalDateTime handledAt) {
}
