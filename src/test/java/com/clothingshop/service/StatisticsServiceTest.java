package com.clothingshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clothingshop.TestDatabase;
import com.clothingshop.dto.StatisticsReport;
import com.clothingshop.entity.Customer;
import com.clothingshop.entity.Order;
import com.clothingshop.entity.OrderDetail;
import com.clothingshop.entity.OrderStatus;
import com.clothingshop.entity.Payment;
import com.clothingshop.entity.PaymentMethod;
import com.clothingshop.entity.PaymentStatus;
import com.clothingshop.entity.Product;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.util.JpaUtil;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * FR-43: revenue by paidAt over PAID payments only, sales per product and row before vouchers, orders per status by
 * orderDate without waiting VNPAY payments (BR-26), stock. Data lives in 2031 so the other tests' orders (2026) stay out.
 */
class StatisticsServiceTest {

    private static final LocalDate MARCH_1 = LocalDate.of(2031, 3, 1);
    private static final LocalDate MARCH_31 = LocalDate.of(2031, 3, 31);

    private static final StatisticsService SERVICE = new StatisticsService(
            Clock.fixed(Instant.parse("2031-03-20T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")));
    private static String shirt;
    private static String jeans;

    @BeforeAll
    static void data() {
        TestDatabase.start();
        int customer = TestData.customer("matkhau-123").getId();
        shirt = TestData.unique("Áo thống kê ");
        jeans = TestData.unique("Quần thống kê ");
        Product shirtBlackM = TestData.product(shirt, "Đen", "M", 7, "100000");
        Product shirtWhiteL = TestData.product(shirt, "Trắng", "L", 0, "100000");
        Product jeansBlue = TestData.product(jeans, "Xanh", "30", 4, "150000");

        // Paid on 05/03: 2 shirts + 1 jeans = 350.000, voucher -50.000, shipping +20.000
        order(customer, "2031-03-02T09:00", OrderStatus.COMPLETED, PaymentMethod.COD, PaymentStatus.PAID, "2031-03-05T10:00",
                "320000", Map.of(shirtBlackM, 2, jeansBlue, 1), Map.of(shirtBlackM, "200000", jeansBlue, "150000"));
        // Paid in the last minute of the period
        order(customer, "2031-03-31T23:00", OrderStatus.SHIPPING, PaymentMethod.VNPAY, PaymentStatus.PAID, "2031-03-31T23:59",
                "120000", Map.of(shirtWhiteL, 1), Map.of(shirtWhiteL, "100000"));
        // Refunded: no revenue, still an order of the period
        order(customer, "2031-03-08T09:00", OrderStatus.CANCELLED, PaymentMethod.VNPAY, PaymentStatus.REFUND, "2031-03-08T09:05",
                "1020000", Map.of(shirtBlackM, 10), Map.of(shirtBlackM, "1000000"));
        // Waiting for VNPAY: hidden everywhere (BR-26)
        order(customer, "2031-03-15T09:00", OrderStatus.PENDING, PaymentMethod.VNPAY, PaymentStatus.UNPAID, null,
                "120000", Map.of(jeansBlue, 1), Map.of(jeansBlue, "100000"));
        // COD not delivered yet: an order of the period, no revenue
        order(customer, "2031-03-20T09:00", OrderStatus.PENDING, PaymentMethod.COD, PaymentStatus.UNPAID, null,
                "170000", Map.of(jeansBlue, 1), Map.of(jeansBlue, "150000"));
        // Placed in the period, paid just after it
        order(customer, "2031-03-30T09:00", OrderStatus.COMPLETED, PaymentMethod.COD, PaymentStatus.PAID, "2031-04-01T00:00",
                "770000", Map.of(jeansBlue, 5), Map.of(jeansBlue, "750000"));
        // Placed and paid before the period
        order(customer, "2031-02-27T09:00", OrderStatus.COMPLETED, PaymentMethod.COD, PaymentStatus.PAID, "2031-02-28T23:59",
                "220000", Map.of(shirtBlackM, 2), Map.of(shirtBlackM, "200000"));
    }

    private static void order(int customerId, String placedAt, OrderStatus status, PaymentMethod method, PaymentStatus paymentStatus,
                              String paidAt, String total, Map<Product, Integer> quantities, Map<Product, String> subtotals) {
        JpaUtil.inTransaction(em -> {
            Order o = new Order(em.getReference(Customer.class, customerId), null, LocalDateTime.parse(placedAt),
                    "1 Võ Văn Ngân, Thủ Đức, TP. HCM", null, new BigDecimal("20000"), new BigDecimal(total));
            o.setStatus(status);
            quantities.forEach((product, quantity) -> o.addDetail(new OrderDetail(em.getReference(Product.class, product.getId()),
                    quantity, new BigDecimal(subtotals.get(product)))));
            em.persist(o);
            Payment p = new Payment(o, method);
            p.setStatus(paymentStatus);
            p.setPaidAt(paidAt == null ? null : LocalDateTime.parse(paidAt));
            em.persist(p);
            return null;
        });
    }

    private static StatisticsReport.Bucket bucket(StatisticsReport report, String label) {
        return report.series().stream().filter(b -> b.label().equals(label)).findFirst().orElseThrow();
    }

    @Test
    void revenueCountsPaidPaymentsByPaidAtIncludingShipping() {
        StatisticsReport r = SERVICE.report(MARCH_1, MARCH_31);

        assertEquals(new BigDecimal("440000"), r.revenue());
        assertEquals(2, r.paidOrders());
        assertEquals(new BigDecimal("1210000"), SERVICE.report(MARCH_1, LocalDate.of(2031, 4, 1)).revenue(), "+ the order paid on 01/04");
        assertEquals(new BigDecimal("0"), SERVICE.report(LocalDate.of(2031, 3, 6), LocalDate.of(2031, 3, 30)).revenue());
    }

    @Test
    void salesPerProductAndRowAreSubtotalsOfPaidOrders() {
        StatisticsReport r = SERVICE.report(MARCH_1, MARCH_31);

        assertEquals(List.of(new StatisticsReport.ProductStat(shirt, 3, new BigDecimal("300000")),
                new StatisticsReport.ProductStat(jeans, 1, new BigDecimal("150000"))), r.products());
        assertEquals(4, r.soldQuantity());
        assertEquals(List.of("Đen/M 2 200000", "Xanh/30 1 150000", "Trắng/L 1 100000"), r.rows().stream()
                .map(row -> row.color() + "/" + row.size() + " " + row.quantity() + " " + row.revenue()).toList());
    }

    @Test
    void ordersPerStatusUseOrderDateAndHideWaitingVnpayPayments() {
        StatisticsReport r = SERVICE.report(MARCH_1, MARCH_31);

        assertEquals(5, r.placedOrders());
        assertEquals(2L, r.ordersByStatus().get(OrderStatus.COMPLETED));
        assertEquals(1L, r.ordersByStatus().get(OrderStatus.SHIPPING));
        assertEquals(1L, r.ordersByStatus().get(OrderStatus.CANCELLED));
        assertEquals(1L, r.ordersByStatus().get(OrderStatus.PENDING), "the COD order only");
        assertEquals(0L, r.ordersByStatus().get(OrderStatus.CONFIRMED));
    }

    @Test
    void dailySeriesHasEveryDayAndARoundScale() {
        StatisticsReport r = SERVICE.report(MARCH_1, MARCH_31);

        assertFalse(r.monthly());
        assertEquals(31, r.series().size());
        assertEquals("01/03", r.series().get(0).label());
        assertEquals(new BigDecimal("320000"), bucket(r, "05/03").revenue());
        assertEquals(1, bucket(r, "05/03").paidOrders());
        assertEquals(0, bucket(r, "05/03").placedOrders());
        assertEquals(1, bucket(r, "02/03").placedOrders());
        assertEquals(1, bucket(r, "31/03").paidOrders());
        assertEquals(0, bucket(r, "15/03").placedOrders(), "waiting VNPAY payment");
        assertEquals(new BigDecimal("500000"), r.seriesMax());
        assertEquals(64.0, bucket(r, "05/03").heightPercent());
        assertEquals(List.of("0 0.0", "100000 20.0", "200000 40.0", "300000 60.0", "400000 80.0", "500000 100.0"),
                r.ticks().stream().map(t -> t.amount() + " " + t.percent()).toList());
        assertEquals(3, r.labelEvery());
    }

    @Test
    void longPeriodsAreGroupedByMonth() {
        StatisticsReport r = SERVICE.report(LocalDate.of(2031, 1, 1), MARCH_31);

        assertTrue(r.monthly());
        assertEquals(List.of("01/2031", "02/2031", "03/2031"), r.series().stream().map(StatisticsReport.Bucket::label).toList());
        assertEquals(new BigDecimal("220000"), bucket(r, "02/2031").revenue());
        assertEquals(new BigDecimal("440000"), bucket(r, "03/2031").revenue());
        assertEquals(1, r.labelEvery());
    }

    @Test
    void stockPerProductAndRowIncludesEmptyRows() {
        StatisticsReport.StockStat s = SERVICE.report(MARCH_1, MARCH_31).stock().stream()
                .filter(st -> st.name().equals(shirt)).findFirst().orElseThrow();

        assertEquals(7, s.total());
        assertEquals(List.of(new StatisticsReport.StockRow("Đen", "M", 7), new StatisticsReport.StockRow("Trắng", "L", 0)), s.rows());
    }

    @Test
    void periodMustBeOrderedAndAtMostAYear() {
        assertThrows(ValidationException.class, () -> SERVICE.report(MARCH_31, MARCH_1));
        assertThrows(ValidationException.class, () -> SERVICE.report(LocalDate.of(2031, 1, 1), LocalDate.of(2032, 1, 2)));
        SERVICE.report(LocalDate.of(2032, 1, 1), LocalDate.of(2032, 12, 31));   // 366 days, leap year
        assertEquals(LocalDate.of(2031, 3, 1), SERVICE.defaultPeriod()[0]);
        assertEquals(LocalDate.of(2031, 3, 20), SERVICE.defaultPeriod()[1]);
    }

    @Test
    void scaleTopIsOneTwoOrFiveTimesAPowerOfTen() {
        assertEquals(new BigDecimal("1000000"), StatisticsService.niceTop(BigDecimal.ZERO));
        assertEquals(new BigDecimal("500000"), StatisticsService.niceTop(new BigDecimal("320000")));
        assertEquals(new BigDecimal("1000000"), StatisticsService.niceTop(new BigDecimal("1000000")));
        assertEquals(new BigDecimal("2000000"), StatisticsService.niceTop(new BigDecimal("1000001")));
        assertEquals(new BigDecimal("10"), StatisticsService.niceTop(new BigDecimal("7")));
        assertEquals(List.of("0", "250000", "500000", "750000", "1000000"),
                StatisticsService.ticks(new BigDecimal("1000000")).stream().map(t -> t.amount().toString()).toList());
        assertEquals(List.of("0", "500000", "1000000", "1500000", "2000000"),
                StatisticsService.ticks(new BigDecimal("2000000")).stream().map(t -> t.amount().toString()).toList());
    }
}
