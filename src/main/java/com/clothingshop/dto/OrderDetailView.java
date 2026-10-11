package com.clothingshop.dto;

import com.clothingshop.entity.DeliveryIssueStatus;
import com.clothingshop.entity.DeliveryResolution;
import com.clothingshop.entity.OrderStatus;
import com.clothingshop.entity.PaymentMethod;
import com.clothingshop.entity.PaymentStatus;
import com.clothingshop.entity.ReturnRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One order with everything the customer (FR-16) and staff (FR-18, FR-21, FR-23, FR-26) see.
 *
 * @param productAmount   Σ OrderDetail.subtotal
 * @param discount        voucher discount = product amount + shipping fee − total (not stored, BR-04)
 * @param canCancel       the customer may cancel (BR-10)
 * @param canConfirm      the customer may confirm receipt (FR-19)
 * @param canRequestReturn Order.canRequestReturn() (FR-25, BR-11)
 */
public record OrderDetailView(int id, int customerId, String customerName, String customerPhone, String customerEmail,
                              LocalDateTime orderDate, String address, String note, String voucherCode, OrderStatus status,
                              LocalDateTime shippedAt, LocalDateTime deliveredAt, PaymentMethod method,
                              PaymentStatus paymentStatus, LocalDateTime paidAt, String transactionNo, List<Line> lines,
                              BigDecimal productAmount, BigDecimal discount, BigDecimal shippingFee, BigDecimal totalAmount,
                              Issue issue, Return returnRequest, boolean canCancel, boolean canConfirm,
                              boolean canRequestReturn) {

    /**
     * A line: unit price = subtotal / quantity (Q02, Q45).
     *
     * @param reviewable the customer may review this product now (FR-29)
     * @param reviewId   the customer's review of this product, or {@code null}
     */
    public record Line(int productId, String name, String color, String size, String imgUrl, int quantity,
                       BigDecimal unitPrice, BigDecimal subtotal, int stockQuantity, boolean reviewable, Integer reviewId) {
    }

    /** The "Chưa thấy hàng đâu" report of the order, if any (FR-20, FR-21). */
    public record Issue(int id, LocalDateTime reportedAt, DeliveryIssueStatus status, DeliveryResolution resolution,
                        String resolvedBy, LocalDateTime resolvedAt) {
    }

    /** The return request of the order, if any (FR-25, FR-26). */
    public record Return(int id, LocalDateTime requestedAt, String reason, ReturnRequestStatus status, String handledBy,
                         LocalDateTime handledAt) {
    }
}
