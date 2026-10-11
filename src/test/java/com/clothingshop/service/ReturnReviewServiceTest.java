package com.clothingshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clothingshop.TestDatabase;
import com.clothingshop.dao.OrderDao;
import com.clothingshop.dto.AddressForm;
import com.clothingshop.entity.Customer;
import com.clothingshop.entity.CustomerLevel;
import com.clothingshop.entity.DiscountType;
import com.clothingshop.entity.EligibleCustomer;
import com.clothingshop.entity.Employee;
import com.clothingshop.entity.EmployeeRole;
import com.clothingshop.entity.Order;
import com.clothingshop.entity.OrderStatus;
import com.clothingshop.entity.PaymentMethod;
import com.clothingshop.entity.PaymentStatus;
import com.clothingshop.entity.Product;
import com.clothingshop.entity.ReturnRequest;
import com.clothingshop.entity.ReturnRequestStatus;
import com.clothingshop.entity.Voucher;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
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

/** FR-25, FR-26 returns and FR-29, FR-30 reviews. */
class ReturnReviewServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final class RefundGateway implements VnpayGateway {
        boolean accept = true;

        public String paymentUrl(int o, BigDecimal a, LocalDateTime c, LocalDateTime e, String ip, String r) {
            return "";
        }

        public boolean isSignatureValid(Map<String, String> p) {
            return false;
        }

        public QueryResult query(int orderId, LocalDateTime createdAt) {
            return new QueryResult(QueryStatus.UNKNOWN, null, null, "");
        }

        public boolean refund(int orderId, BigDecimal amount, String no, LocalDateTime createdAt, String by) {
            return accept;
        }
    }

    private final List<String> subjects = new ArrayList<>();
    private final RefundGateway vnpay = new RefundGateway();
    private MutableClock clock;
    private CartService cart;
    private CheckoutService checkout;
    private PaymentService payments;
    private OrderService orders;
    private ReturnService returns;
    private ReviewService reviews;
    private int customerId;
    private Product white;
    private Product black;
    private String voucher;
    private Employee staff;

    @BeforeAll
    static void database() {
        TestDatabase.start();
    }

    @BeforeEach
    void setUp() {
        clock = new MutableClock(TestData.NOW.atZone(ZONE).toInstant(), ZONE);
        EmailService mail = (to, subject, body) -> subjects.add(subject);
        cart = new CartService(new InMemoryCartStore(clock), clock);
        checkout = new CheckoutService(cart, new AddressService(), clock, true);
        payments = new PaymentService(vnpay, cart, clock, false);
        orders = new OrderService(payments, mail, new DeliveryLinks("http://localhost/shop"), clock);
        returns = new ReturnService(payments, mail, clock);
        reviews = new ReviewService(clock);
        customerId = TestData.customer("matkhau-123").getId();
        String name = TestData.unique("Áo trả hàng ");
        white = TestData.product(name, "Trắng", "M", 10, "1200000");
        black = TestData.product(name, "Đen", "M", 10, "1200000");
        voucher = TestData.voucher(DiscountType.FIXED_AMOUNT, "20000", 5, EligibleCustomer.ALL, "0", null);
        staff = TestData.employee(EmployeeRole.STAFF, "matkhau-123");
    }

    /** A delivered order of 2 × 1.200.000 (2.400.000 paid: REGULAR). */
    private int delivered(PaymentMethod method) {
        cart.addItem(customerId, white.getId(), 2);
        int id = checkout.place(customerId, new CheckoutService.Request(Set.of(white.getId()), null,
                new AddressForm("TP. HCM", "Thủ Đức", "1 Võ Văn Ngân"), null, voucher, method, null));
        if (method == PaymentMethod.VNPAY) {
            payments.markPaid(id, "555");
        }
        orders.confirm(id);
        orders.ship(id);
        orders.confirmReceived(id);
        return id;
    }

    private ReturnRequest requestOf(int orderId) {
        return JpaUtil.read(em -> em.createQuery("select r from ReturnRequest r where r.order.id = :o", ReturnRequest.class)
                .setParameter("o", orderId).getSingleResult());
    }

    private int stock() {
        return JpaUtil.read(em -> em.find(Product.class, white.getId()).getStockQuantity());
    }

    @Test
    void aReturnCanBeRequestedOnceWithin15DaysOfDelivery() {
        int id = delivered(PaymentMethod.COD);
        assertThrows(ValidationException.class, () -> returns.submit(customerId, id, " "));
        assertThrows(BusinessException.class, () -> returns.submit(customerId + 1, id, "Không vừa"));
        clock.advance(Duration.ofDays(15));
        returns.submit(customerId, id, "Không vừa size");
        assertEquals(ReturnRequestStatus.PENDING, requestOf(id).getStatus());
        assertThrows(BusinessException.class, () -> returns.submit(customerId, id, "Lần hai"), "one request per order");

        int late = delivered(PaymentMethod.COD);
        clock.advance(Duration.ofDays(15).plusSeconds(1));
        assertThrows(BusinessException.class, () -> returns.submit(customerId, late, "Quá hạn"), "BR-11: 15 days");
    }

    @Test
    void approvingACodReturnCancelsTheOrderAndGivesEverythingBack() {
        int id = delivered(PaymentMethod.COD);
        assertEquals(CustomerLevel.REGULAR, JpaUtil.read(em -> em.find(Customer.class, customerId)).getCustomerLevel());
        returns.submit(customerId, id, "Vải không như mô tả");
        int requestId = requestOf(id).getId();
        returns.approve(requestId, staff.getId(), staff.getUserName());
        ReturnRequest r = requestOf(id);
        assertEquals(ReturnRequestStatus.APPROVED, r.getStatus());
        assertEquals(TestData.NOW, r.getHandledAt());
        assertEquals(OrderStatus.CANCELLED, JpaUtil.read(em -> em.find(Order.class, id)).getStatus());
        assertEquals(PaymentStatus.REFUND, JpaUtil.read(em -> new OrderDao(em).paymentOf(id)).getStatus());
        assertEquals(10, stock());
        assertEquals(5, JpaUtil.read(em -> em.find(Voucher.class, voucher)).getQuantity(), "Q29");
        assertEquals(CustomerLevel.CASUAL, JpaUtil.read(em -> em.find(Customer.class, customerId)).getCustomerLevel());
        assertTrue(subjects.get(subjects.size() - 1).contains("trả hàng"), "E7");
        assertThrows(BusinessException.class, () -> returns.reject(requestId, staff.getId()), "already handled");
    }

    @Test
    void aVnpayReturnIsRefundedFirstAndStaysPendingIfTheRefundFails() {
        int id = delivered(PaymentMethod.VNPAY);
        returns.submit(customerId, id, "Đổi ý");
        int requestId = requestOf(id).getId();
        vnpay.accept = false;
        assertThrows(BusinessException.class, () -> returns.approve(requestId, staff.getId(), staff.getUserName()));
        assertEquals(ReturnRequestStatus.PENDING, requestOf(id).getStatus(), "Q65");
        assertEquals(OrderStatus.COMPLETED, JpaUtil.read(em -> em.find(Order.class, id)).getStatus());
        vnpay.accept = true;
        returns.approve(requestId, staff.getId(), staff.getUserName());
        assertEquals(ReturnRequestStatus.APPROVED, requestOf(id).getStatus());
    }

    @Test
    void rejectingKeepsTheOrderCompleted() {
        int id = delivered(PaymentMethod.COD);
        returns.submit(customerId, id, "Không thích");
        returns.reject(requestOf(id).getId(), staff.getId());
        assertEquals(ReturnRequestStatus.REJECTED, requestOf(id).getStatus());
        assertEquals(OrderStatus.COMPLETED, JpaUtil.read(em -> em.find(Order.class, id)).getStatus());
        assertEquals(8, stock());
    }

    @Test
    void onlyBoughtProductsAreReviewedOncePerProduct() {
        assertThrows(BusinessException.class, () -> reviews.create(customerId, white.getId(), "5", "Đẹp"), "not bought yet");
        delivered(PaymentMethod.COD);
        assertThrows(ValidationException.class, () -> reviews.create(customerId, white.getId(), "6", null));
        assertThrows(ValidationException.class, () -> reviews.create(customerId, white.getId(), "abc", null));
        int reviewId = reviews.create(customerId, white.getId(), "4", "Vải đẹp");
        assertThrows(BusinessException.class, () -> reviews.create(customerId, white.getId(), "5", "Lần hai"));
        assertThrows(BusinessException.class, () -> reviews.create(customerId, black.getId(), "5", "Dòng khác cùng mặt hàng"),
                "one review per product whatever the row (Q44)");
        int other = TestData.customer("matkhau-123").getId();
        assertThrows(BusinessException.class, () -> reviews.delete(other, reviewId), "only the author");
        reviews.delete(customerId, reviewId);
        assertEquals(0L, (long) JpaUtil.<Long>read(em -> em.createQuery("select count(r) from Review r where r.id = :id", Long.class)
                .setParameter("id", reviewId).getSingleResult()));
        reviews.create(customerId, white.getId(), "5", null);
    }
}
