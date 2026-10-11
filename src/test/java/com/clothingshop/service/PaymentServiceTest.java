package com.clothingshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clothingshop.TestDatabase;
import com.clothingshop.dao.OrderDao;
import com.clothingshop.dto.AddressForm;
import com.clothingshop.entity.Customer;
import com.clothingshop.entity.CustomerLevel;
import com.clothingshop.entity.DiscountType;
import com.clothingshop.entity.EligibleCustomer;
import com.clothingshop.entity.Order;
import com.clothingshop.entity.Payment;
import com.clothingshop.entity.PaymentMethod;
import com.clothingshop.entity.PaymentStatus;
import com.clothingshop.entity.Product;
import com.clothingshop.entity.Voucher;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.util.JpaUtil;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** FR-13 VNPAY: waiting order, IPN, Return URL, J7 reconciliation, discarding, refund. */
class PaymentServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /**
     * Accepts parameters signed "ok"; query and refund answers are set by each test. Queries about other tests' orders
     * (the database is shared) get no answer, so they are left alone.
     */
    private static final class FakeGateway implements VnpayGateway {
        QueryResult answer = new QueryResult(QueryStatus.UNKNOWN, null, null, "no answer");
        int answerFor;
        boolean refundAccepted = true;
        int refunds;

        @Override
        public String paymentUrl(int orderId, BigDecimal amount, LocalDateTime createdAt, LocalDateTime expireAt, String ip, String ret) {
            return "https://pay.example/?vnp_TxnRef=" + orderId + "&vnp_Amount=" + VnpayClient.vnpAmount(amount);
        }

        @Override
        public boolean isSignatureValid(Map<String, String> params) {
            return "ok".equals(params.get("vnp_SecureHash"));
        }

        @Override
        public QueryResult query(int orderId, LocalDateTime createdAt) {
            return orderId == answerFor ? answer : new QueryResult(QueryStatus.UNKNOWN, null, null, "not this test");
        }

        @Override
        public boolean refund(int orderId, BigDecimal amount, String transactionNo, LocalDateTime createdAt, String createdBy) {
            refunds++;
            return refundAccepted;
        }
    }

    private final FakeGateway vnpay = new FakeGateway();
    private MutableClock clock;
    private CartService cart;
    private CheckoutService checkout;
    private PaymentService payments;
    private int customerId;
    private Product product;
    private String voucher;

    @BeforeAll
    static void database() {
        TestDatabase.start();
    }

    @BeforeEach
    void setUp() {
        clock = new MutableClock(TestData.NOW.atZone(ZONE).toInstant(), ZONE);
        cart = new CartService(new InMemoryCartStore(clock), clock);
        checkout = new CheckoutService(cart, new AddressService(), clock, true);
        payments = new PaymentService(vnpay, cart, clock, false);
        customerId = TestData.customer("matkhau-123").getId();
        product = TestData.product(TestData.unique("Áo VNPAY "), "Đen", "M", 10, "2500000");
        voucher = TestData.voucher(DiscountType.FIXED_AMOUNT, "20000", 3, EligibleCustomer.ALL, "0", null);
    }

    /** A waiting VNPAY order of 2 items: 5.000.000 − 20.000 + 20.000 = 5.000.000. */
    private int waitingOrder() {
        cart.addItem(customerId, product.getId(), 2);
        vnpay.answerFor = checkout.place(customerId, new CheckoutService.Request(Set.of(product.getId()), null,
                new AddressForm("TP. HCM", "Thủ Đức", "1 Võ Văn Ngân"), null, voucher, PaymentMethod.VNPAY, null));
        return vnpay.answerFor;
    }

    private Map<String, String> ipn(int orderId, String amount, String responseCode) {
        Map<String, String> p = new HashMap<>();
        p.put("vnp_TxnRef", String.valueOf(orderId));
        p.put("vnp_Amount", amount);
        p.put("vnp_ResponseCode", responseCode);
        p.put("vnp_TransactionStatus", responseCode);
        p.put("vnp_TransactionNo", "14123456");
        p.put("vnp_SecureHash", "ok");
        return p;
    }

    private Payment payment(int orderId) {
        return JpaUtil.read(em -> em.createQuery("select p from Payment p where p.order.id = :o", Payment.class)
                .setParameter("o", orderId).getResultStream().findFirst().orElse(null));
    }

    private boolean visible(int orderId) {
        return JpaUtil.<Boolean>read(em -> new OrderDao(em).isVisible(orderId));
    }

    private int stock() {
        return JpaUtil.read(em -> em.find(Product.class, product.getId()).getStockQuantity());
    }

    private int voucherLeft() {
        return JpaUtil.read(em -> em.find(Voucher.class, voucher).getQuantity());
    }

    @Test
    void aWaitingOrderHoldsStockAndVoucherAndIsHiddenUntilPaid() {
        int orderId = waitingOrder();
        assertEquals(PaymentStatus.UNPAID, payment(orderId).getStatus());
        assertEquals(8, stock());
        assertEquals(2, voucherLeft());
        assertTrue(cart.lines(customerId).containsKey(product.getId()), "the cart is kept until the payment succeeds");
        assertFalse(visible(orderId), "BR-26");
        assertTrue(payments.paymentUrl(customerId, orderId, "127.0.0.1", "http://x/return").contains("vnp_Amount=500000000"));
        assertThrows(BusinessException.class, () -> payments.paymentUrl(customerId + 1, orderId, "127.0.0.1", "http://x"));
    }

    @Test
    void aSuccessfulIpnPaysTheOrderOnceEmptiesTheCartAndRaisesTheLevel() {
        int orderId = waitingOrder();
        assertEquals("00", payments.handleIpn(ipn(orderId, "500000000", "00")).rspCode());
        Payment p = payment(orderId);
        assertEquals(PaymentStatus.PAID, p.getStatus());
        assertEquals("14123456", p.getTransactionNo());
        assertEquals(TestData.NOW, p.getPaidAt());
        assertTrue(cart.lines(customerId).isEmpty());
        assertEquals(CustomerLevel.REGULAR, JpaUtil.read(em -> em.find(Customer.class, customerId)).getCustomerLevel(),
                "5.000.000 paid (FR-27)");
        assertTrue(visible(orderId));
        assertEquals("02", payments.handleIpn(ipn(orderId, "500000000", "00")).rspCode(), "a repeated IPN changes nothing");
        assertEquals(PaymentService.ReturnStatus.PAID, payments.returnStatus(ipn(orderId, "500000000", "00")));
    }

    @Test
    void aFailedIpnDiscardsTheOrderAndGivesStockAndVoucherBack() {
        int orderId = waitingOrder();
        assertEquals("00", payments.handleIpn(ipn(orderId, "500000000", "24")).rspCode());
        assertNull(payment(orderId));
        assertNull(JpaUtil.read(em -> em.find(Order.class, orderId)));
        assertEquals(10, stock());
        assertEquals(3, voucherLeft());
        assertTrue(cart.lines(customerId).containsKey(product.getId()), "the cart is kept");
        assertEquals(PaymentService.ReturnStatus.FAILED, payments.returnStatus(ipn(orderId, "500000000", "24")));
        assertEquals("01", payments.handleIpn(ipn(orderId, "500000000", "00")).rspCode());
    }

    @Test
    void ipnChecksSignatureAndAmount() {
        int orderId = waitingOrder();
        Map<String, String> unsigned = ipn(orderId, "500000000", "00");
        unsigned.put("vnp_SecureHash", "forged");
        assertEquals("97", payments.handleIpn(unsigned).rspCode());
        assertEquals("04", payments.handleIpn(ipn(orderId, "100", "00")).rspCode());
        assertEquals("01", payments.handleIpn(ipn(999999, "100", "00")).rspCode());
        assertEquals(PaymentStatus.UNPAID, payment(orderId).getStatus());
        assertEquals(PaymentService.ReturnStatus.WAITING, payments.returnStatus(ipn(orderId, "500000000", "00")),
                "the Return URL only shows the state (FR-13 step 5)");
        assertEquals(PaymentService.ReturnStatus.INVALID, payments.returnStatus(unsigned));
    }

    @Test
    void j7AsksVnpayOnlyAfterThePaymentTime() {
        int orderId = waitingOrder();
        vnpay.answer = new VnpayGateway.QueryResult(VnpayGateway.QueryStatus.PAID, "999", new BigDecimal("5000000"), "ok");
        clock.advance(Duration.ofMinutes(14));
        assertEquals(0, payments.reconcilePending(), "still within the 15 minutes");
        clock.advance(Duration.ofMinutes(2));
        assertEquals(1, payments.reconcilePending());
        assertEquals(PaymentStatus.PAID, payment(orderId).getStatus());
        assertEquals("999", payment(orderId).getTransactionNo());
    }

    @Test
    void j7DiscardsUnpaidOrdersAndKeepsOnesWithoutAnswer() {
        int unknown = waitingOrder();
        clock.advance(Duration.ofMinutes(16));
        assertEquals(0, payments.reconcilePending());
        assertEquals(PaymentStatus.UNPAID, payment(unknown).getStatus(), "VNPAY may still have the money: keep");
        vnpay.answer = new VnpayGateway.QueryResult(VnpayGateway.QueryStatus.NOT_PAID, null, null, "91");
        assertEquals(1, payments.reconcilePending());
        assertNull(payment(unknown));
        assertEquals(10, stock());
    }

    @Test
    void refundsGoThroughVnpayAndAFailureChangesNothing() {
        int orderId = waitingOrder();
        payments.handleIpn(ipn(orderId, "500000000", "00"));
        vnpay.refundAccepted = false;
        assertThrows(BusinessException.class, () -> payments.refundVnpay(orderId, "nv01"));
        vnpay.refundAccepted = true;
        payments.refundVnpay(orderId, "nv01");
        assertEquals(2, vnpay.refunds);
        int cod = TestData.order(customerId, "100000", com.clothingshop.entity.OrderStatus.PENDING, PaymentMethod.COD, PaymentStatus.UNPAID);
        assertThrows(BusinessException.class, () -> payments.refundVnpay(cod, "nv01"));
    }
}
