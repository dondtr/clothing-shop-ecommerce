package com.clothingshop.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * The VNPAY payment gateway (API version 2.1.0) as the shop uses it (FR-13, section 3.6). The implementation is
 * {@link VnpayClient}; tests use fakes.
 */
public interface VnpayGateway {

    /** Result of asking VNPAY about a transaction (querydr). */
    enum QueryStatus {
        /** Paid: the order is confirmed as paid. */
        PAID,
        /** Not paid, failed or never started: the waiting order can be discarded. */
        NOT_PAID,
        /** No answer or an answer that cannot be trusted: keep the order and ask again later. */
        UNKNOWN
    }

    /**
     * @param transactionNo VNPAY's transaction number when paid
     * @param amount        amount paid in VND when paid
     */
    record QueryResult(QueryStatus status, String transactionNo, BigDecimal amount, String message) {
    }

    /**
     * The address of VNPAY's payment page for an order, signed with the shop's secret.
     *
     * @param createdAt the transaction time; querydr and refund send it back as vnp_TransactionDate
     * @param expireAt  after this the customer can no longer pay (orderDate + 15 minutes, Q37)
     */
    String paymentUrl(int orderId, BigDecimal amount, LocalDateTime createdAt, LocalDateTime expireAt, String clientIp,
                      String returnUrl);

    /** Whether the vnp_* parameters sent back by VNPAY (IPN or Return URL) carry a valid signature. */
    boolean isSignatureValid(Map<String, String> params);

    /** Asks VNPAY for the result of the transaction of {@code orderId} created at {@code createdAt} (querydr). */
    QueryResult query(int orderId, LocalDateTime createdAt);

    /**
     * Refunds the whole amount of a paid transaction (Payment.refund(), FR-17, FR-23, FR-26).
     *
     * @return {@code true} when VNPAY accepted the refund
     */
    boolean refund(int orderId, BigDecimal amount, String transactionNo, LocalDateTime createdAt, String createdBy);
}
