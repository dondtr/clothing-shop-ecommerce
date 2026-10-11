package com.clothingshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clothingshop.TestDatabase;
import com.clothingshop.dao.OrderDao;
import com.clothingshop.dto.AddressForm;
import com.clothingshop.dto.OrderDetailView;
import com.clothingshop.entity.Customer;
import com.clothingshop.entity.CustomerLevel;
import com.clothingshop.entity.DeliveryIssue;
import com.clothingshop.entity.DeliveryIssueStatus;
import com.clothingshop.entity.DiscountType;
import com.clothingshop.entity.EligibleCustomer;
import com.clothingshop.entity.Employee;
import com.clothingshop.entity.EmployeeRole;
import com.clothingshop.entity.Order;
import com.clothingshop.entity.OrderStatus;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Order life cycle: FR-16 to FR-24, tasks J1 – J3, delivery issues, email links. */
class OrderServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private record Mail(String to, String subject, String body) {
    }

    /** Refunds accepted or refused on demand; everything else unused. */
    private static final class RefundGateway implements VnpayGateway {
        boolean accept = true;
        int refunds;

        public String paymentUrl(int o, BigDecimal a, LocalDateTime c, LocalDateTime e, String ip, String r) {
            return "https://pay.example/";
        }

        public boolean isSignatureValid(Map<String, String> params) {
            return "ok".equals(params.get("vnp_SecureHash"));
        }

        public QueryResult query(int orderId, LocalDateTime createdAt) {
            return new QueryResult(QueryStatus.UNKNOWN, null, null, "");
        }

        public boolean refund(int orderId, BigDecimal amount, String transactionNo, LocalDateTime createdAt, String by) {
            refunds++;
            return accept;
        }
    }

    private final List<Mail> outbox = new ArrayList<>();
    private final RefundGateway vnpay = new RefundGateway();
    private MutableClock clock;
    private CartService cart;
    private CheckoutService checkout;
    private PaymentService payments;
    private OrderService orders;
    private DeliveryService delivery;
    private DeliveryLinks links;
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
        EmailService mail = (to, subject, body) -> outbox.add(new Mail(to, subject, body));
        cart = new CartService(new InMemoryCartStore(clock), clock);
        checkout = new CheckoutService(cart, new AddressService(), clock, true);
        payments = new PaymentService(vnpay, cart, clock, false);
        links = new DeliveryLinks("http://localhost/shop");
        orders = new OrderService(payments, mail, links, clock);
        delivery = new DeliveryService(mail, links, clock);
        customerId = TestData.customer("matkhau-123").getId();
        product = TestData.product(TestData.unique("Áo vòng đời "), "Xanh", "L", 10, "1500000");
        voucher = TestData.voucher(DiscountType.FIXED_AMOUNT, "20000", 5, EligibleCustomer.ALL, "0", null);
    }

    /** 2 × 1.500.000 − 20.000 + 20.000 = 3.000.000. */
    private int place(PaymentMethod method) {
        cart.addItem(customerId, product.getId(), 2);
        return checkout.place(customerId, new CheckoutService.Request(Set.of(product.getId()), null,
                new AddressForm("TP. HCM", "Thủ Đức", "1 Võ Văn Ngân"), null, voucher, method, null));
    }

    private int shipped(PaymentMethod method) {
        int id = place(method);
        if (method == PaymentMethod.VNPAY) {
            payments.markPaid(id, "777");
        }
        orders.confirm(id);
        orders.ship(id);
        return id;
    }

    private Order order(int id) {
        return JpaUtil.read(em -> em.find(Order.class, id));
    }

    private Payment payment(int id) {
        return JpaUtil.read(em -> new OrderDao(em).paymentOf(id));
    }

    private int stock() {
        return JpaUtil.read(em -> em.find(Product.class, product.getId()).getStockQuantity());
    }

    private int voucherLeft() {
        return JpaUtil.read(em -> em.find(Voucher.class, voucher).getQuantity());
    }

    private DeliveryIssue issueOf(int orderId) {
        return JpaUtil.read(em -> em.createQuery("select i from DeliveryIssue i left join fetch i.resolvedBy where i.order.id = :o",
                DeliveryIssue.class).setParameter("o", orderId).getResultStream().findFirst().orElse(null));
    }

    private CustomerLevel level() {
        return JpaUtil.read(em -> em.find(Customer.class, customerId)).getCustomerLevel();
    }

    @Test
    void staffConfirmThenShipAndTheCustomerGetsE1WithASignedButton() {
        int id = place(PaymentMethod.COD);
        assertThrows(BusinessException.class, () -> orders.ship(id), "CONFIRMED first (table 6.1)");
        orders.confirm(id);
        assertThrows(BusinessException.class, () -> orders.confirm(id));
        orders.ship(id);
        assertEquals(OrderStatus.SHIPPING, order(id).getStatus());
        assertEquals(TestData.NOW, order(id).getShippedAt());
        Mail e1 = outbox.get(outbox.size() - 1);
        assertTrue(e1.subject().contains("#" + id));
        String token = e1.body().substring(e1.body().indexOf("token=") + 6).split("\\s")[0];
        assertEquals(new DeliveryLinks.Link(id, DeliveryLinks.Action.RECEIVED), links.verify(token).orElseThrow());
        assertTrue(links.verify(token + "x").isEmpty());
    }

    @Test
    void receivingACodOrderPaysItRaisesTheLevelAndClosesAnOpenIssue() {
        int id = shipped(PaymentMethod.COD);
        assertEquals(DeliveryService.ReportResult.CREATED, delivery.reportNotReceived(id));
        assertEquals(DeliveryService.ReportResult.ALREADY_REPORTED, delivery.reportNotReceived(id));
        clock.advance(Duration.ofDays(1));
        OrderService.ReceivedResult r = orders.confirmReceived(id);
        assertTrue(r.completedNow());
        assertEquals(OrderStatus.COMPLETED, order(id).getStatus());
        assertEquals(PaymentStatus.PAID, payment(id).getStatus());
        assertEquals(CustomerLevel.REGULAR, level(), "3.000.000 paid (FR-27)");
        DeliveryIssue issue = issueOf(id);
        assertEquals(DeliveryIssueStatus.RESOLVED, issue.getStatus());
        assertNull(issue.getResolution());
        assertNull(issue.getResolvedBy());
        assertFalse(orders.confirmReceived(id).completedNow(), "a second click changes nothing (Q24)");
        assertEquals(DeliveryService.ReportResult.NOT_SHIPPING, delivery.reportNotReceived(id));
    }

    @Test
    void theCustomerCancelsAShippingCodOrderAndEverythingIsGivenBack() {
        int id = shipped(PaymentMethod.COD);
        delivery.reportNotReceived(id);
        assertEquals(8, stock());
        assertEquals(4, voucherLeft());
        assertThrows(BusinessException.class, () -> orders.cancelByCustomer(customerId + 1, "khac", id));
        orders.cancelByCustomer(customerId, "khach", id);
        assertEquals(OrderStatus.CANCELLED, order(id).getStatus());
        assertEquals(PaymentStatus.FAILED, payment(id).getStatus());
        assertEquals(10, stock(), "stock back even while shipping (Q28)");
        assertEquals(5, voucherLeft());
        assertEquals(DeliveryIssueStatus.RESOLVED, issueOf(id).getStatus());
        assertTrue(outbox.get(outbox.size() - 1).body().contains("Khách hàng hủy đơn"), "E6");
        assertThrows(BusinessException.class, () -> orders.cancelByCustomer(customerId, "khach", id));
    }

    @Test
    void aPaidVnpayOrderIsRefundedBeforeItIsCancelled() {
        int id = shipped(PaymentMethod.VNPAY);
        assertEquals(CustomerLevel.REGULAR, level());
        vnpay.accept = false;
        assertThrows(BusinessException.class, () -> orders.cancelByCustomer(customerId, "khach", id));
        assertEquals(OrderStatus.SHIPPING, order(id).getStatus(), "refund failed: nothing changes (Q65)");
        assertEquals(PaymentStatus.PAID, payment(id).getStatus());
        vnpay.accept = true;
        orders.cancelByCustomer(customerId, "khach", id);
        assertEquals(PaymentStatus.REFUND, payment(id).getStatus());
        assertEquals(CustomerLevel.CASUAL, level(), "the refund lowers the paid total (autoDowngrade on)");
        assertEquals(2, vnpay.refunds);
        assertTrue(outbox.get(outbox.size() - 1).body().contains("hoàn qua VNPAY"));
    }

    @Test
    void completedOrdersCannotBeCancelledAndFailedDeliveryOnlyWhileShipping() {
        int id = shipped(PaymentMethod.COD);
        Employee staff = TestData.employee(EmployeeRole.STAFF, "matkhau-123");
        int pending = place(PaymentMethod.COD);
        assertThrows(BusinessException.class, () -> orders.cancelFailedDelivery(pending, staff.getId(), staff.getUserName()));
        delivery.reportNotReceived(id);
        orders.cancelFailedDelivery(id, staff.getId(), staff.getUserName());
        assertEquals(OrderStatus.CANCELLED, order(id).getStatus());
        assertEquals(staff.getId(), issueOf(id).getResolvedBy().getId(), "closed by the employee who cancelled (FR-23)");

        orders.confirm(pending);
        orders.ship(pending);
        orders.confirmReceived(pending);
        assertThrows(BusinessException.class, () -> orders.cancelByCustomer(customerId, "khach", pending), "BR-10");
    }

    @Test
    void j2CompletesSevenDaysAfterShippingButNotWithAnOpenIssue() {
        int quiet = shipped(PaymentMethod.COD);
        int reported = shipped(PaymentMethod.COD);
        delivery.reportNotReceived(reported);
        clock.advance(Duration.ofDays(7).minusMinutes(1));
        orders.autoComplete();
        assertEquals(OrderStatus.SHIPPING, order(quiet).getStatus());
        clock.advance(Duration.ofMinutes(1));
        orders.autoComplete();
        assertEquals(OrderStatus.COMPLETED, order(quiet).getStatus());
        assertEquals(OrderStatus.SHIPPING, order(reported).getStatus(), "BR-13: not while an issue is open");

        Employee staff = TestData.employee(EmployeeRole.STAFF, "matkhau-123");
        int issueId = issueOf(reported).getId();
        delivery.resolve(issueId, staff.getId(), false, "Đơn vị vận chuyển giao chậm", "Đã nhắc giao trong ngày");
        clock.advance(Duration.ofDays(6));
        orders.autoComplete();
        assertEquals(OrderStatus.SHIPPING, order(reported).getStatus(), "7 days counted again from resolvedAt (Q21)");
        clock.advance(Duration.ofDays(1));
        orders.autoComplete();
        assertEquals(OrderStatus.COMPLETED, order(reported).getStatus());
    }

    @Test
    void j3CancelsCodOrdersNotShippedWithin15Days() {
        int cod = place(PaymentMethod.COD);
        int confirmed = place(PaymentMethod.COD);
        orders.confirm(confirmed);
        int vnpayPaid = place(PaymentMethod.VNPAY);
        payments.markPaid(vnpayPaid, "1");
        clock.advance(Duration.ofDays(15));
        orders.autoCancelCod();
        assertEquals(OrderStatus.PENDING, order(cod).getStatus(), "exactly 15 days is not more than 15 days");
        clock.advance(Duration.ofSeconds(1));
        orders.autoCancelCod();
        assertEquals(OrderStatus.CANCELLED, order(cod).getStatus());
        assertEquals(OrderStatus.CANCELLED, order(confirmed).getStatus());
        assertEquals(PaymentStatus.FAILED, payment(cod).getStatus());
        assertEquals(OrderStatus.PENDING, order(vnpayPaid).getStatus(), "only COD orders (BR-12)");
    }

    @Test
    void j1RemindsOrdersShippedThreeDaysBeforeExactlyOnce() {
        int id = shipped(PaymentMethod.COD);   // shipped at 10/10 08:00
        clock.advance(Duration.ofDays(3));     // 13/10: window [09/10 00:00, 10/10 00:00) does not include it
        outbox.clear();
        delivery.sendReminders();
        assertTrue(outbox.stream().noneMatch(m -> m.subject().contains("#" + id + " ")));
        clock.advance(Duration.ofDays(1));     // 14/10: window [10/10, 11/10)
        delivery.sendReminders();
        Mail e2 = outbox.stream().filter(m -> m.subject().contains("#" + id + " ")).findFirst().orElseThrow();
        assertTrue(e2.body().contains("Chưa thấy hàng đâu"));
        clock.advance(Duration.ofDays(1));
        outbox.clear();
        delivery.sendReminders();
        assertTrue(outbox.stream().noneMatch(m -> m.subject().contains("#" + id + " ")), "once only (Q04)");
    }

    @Test
    void reshippingTakesStockAgainAndNeedsEnoughForEveryLine() {
        int id = shipped(PaymentMethod.COD);
        delivery.reportNotReceived(id);
        Employee staff = TestData.employee(EmployeeRole.STAFF, "matkhau-123");
        int issueId = issueOf(id).getId();
        assertThrows(com.clothingshop.exception.ValidationException.class,
                () -> delivery.resolve(issueId, staff.getId(), true, "", ""));
        JpaUtil.inTransaction(em -> {
            em.find(Product.class, product.getId()).setStockQuantity(1);
            return null;
        });
        assertThrows(BusinessException.class, () -> delivery.resolve(issueId, staff.getId(), true, "Mất hàng", "Giao lại"));
        assertEquals(1, stock());
        JpaUtil.inTransaction(em -> {
            em.find(Product.class, product.getId()).setStockQuantity(5);
            return null;
        });
        delivery.resolve(issueId, staff.getId(), true, "Mất hàng", "Giao lại trong 2 ngày");
        assertEquals(3, stock(), "taken once more for the reship (Q21)");
        assertEquals(com.clothingshop.entity.DeliveryResolution.RESHIP, issueOf(id).getResolution());
        assertTrue(outbox.get(outbox.size() - 1).body().contains("Giao lại trong 2 ngày"), "E3 carries the typed text");
        assertThrows(BusinessException.class, () -> delivery.resolve(issueId, staff.getId(), false, "a", "b"));
        assertTrue(delivery.openIssues().stream().noneMatch(i -> i.orderId() == id));
    }

    @Test
    void theOrderPageShowsAmountsAndTheButtonsThatApply() {
        int id = place(PaymentMethod.COD);
        OrderDetailView v = new OrderQueries(clock).forCustomer(customerId, id);
        assertEquals(new BigDecimal("3000000"), v.productAmount());
        assertEquals(new BigDecimal("20000"), v.discount());
        assertEquals(new BigDecimal("1500000"), v.lines().get(0).unitPrice());
        assertTrue(v.canCancel());
        assertFalse(v.canConfirm());
        assertThrows(BusinessException.class, () -> new OrderQueries(clock).forCustomer(customerId + 1, id));
        int waiting = place(PaymentMethod.VNPAY);
        assertThrows(BusinessException.class, () -> new OrderQueries(clock).forCustomer(customerId, waiting), "BR-26");
        assertTrue(new OrderQueries(clock).forCustomer(customerId).stream().noneMatch(r -> r.id() == waiting));
    }
}
