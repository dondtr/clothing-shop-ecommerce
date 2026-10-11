package com.clothingshop.service;

import com.clothingshop.dao.OrderDao;
import com.clothingshop.dto.StatisticsReport;
import com.clothingshop.entity.OrderStatus;
import com.clothingshop.entity.PaymentStatus;
import com.clothingshop.entity.Product;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.util.JpaUtil;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Business statistics, manager only (FR-43): revenue (Statistics.revenue()), orders per status (orderCount()), quantities
 * sold (soldQuantity()) and stock (stockReport()). Only PAID payments make revenue (Q33); refunded orders do not count.
 */
public class StatisticsService {

    static final int MAX_DAYS = 366;
    private static final int DAILY_UP_TO = 62;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM/yyyy");

    private final Clock clock;

    public StatisticsService(Clock clock) {
        this.clock = clock;
    }

    /** The current month up to today, used when no period is chosen. */
    public LocalDate[] defaultPeriod() {
        LocalDate today = LocalDate.now(clock);
        return new LocalDate[] {today.withDayOfMonth(1), today};
    }

    /** Statistics from {@code from} to {@code to}, both days included (at most 366 days). */
    public StatisticsReport report(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new ValidationException(Map.of("to", "Ngày kết thúc phải từ ngày bắt đầu trở đi"));
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_DAYS) {
            throw new ValidationException(Map.of("to", "Khoảng thời gian tối đa " + MAX_DAYS + " ngày"));
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        return JpaUtil.read(em -> {
            Object[] totals = em.createQuery("select coalesce(sum(o.totalAmount), 0), count(o) from Payment p join p.order o"
                    + " where p.status = :paid and p.paidAt >= :s and p.paidAt < :e", Object[].class)
                    .setParameter("paid", PaymentStatus.PAID).setParameter("s", start).setParameter("e", end).getSingleResult();

            Map<OrderStatus, Long> byStatus = new EnumMap<>(OrderStatus.class);
            for (OrderStatus s : OrderStatus.values()) {
                byStatus.put(s, 0L);
            }
            long placed = 0;
            for (Object[] r : em.createQuery("select o.status, count(o) from Payment p join p.order o where " + OrderDao.VISIBLE
                    + " and o.orderDate >= :s and o.orderDate < :e group by o.status", Object[].class)
                    .setParameter("s", start).setParameter("e", end).getResultList()) {
                byStatus.put((OrderStatus) r[0], (Long) r[1]);
                placed += (Long) r[1];
            }

            List<StatisticsReport.ProductStat> products = em.createQuery("select pr.name, sum(d.quantity), sum(d.subtotal)"
                            + " from Payment p join p.order o join o.details d join d.product pr where p.status = :paid"
                            + " and p.paidAt >= :s and p.paidAt < :e group by pr.name order by sum(d.subtotal) desc", Object[].class)
                    .setParameter("paid", PaymentStatus.PAID).setParameter("s", start).setParameter("e", end)
                    .getResultList().stream()
                    .map(r -> new StatisticsReport.ProductStat((String) r[0], ((Number) r[1]).longValue(), money(r[2]))).toList();
            List<StatisticsReport.RowStat> rows = em.createQuery("select pr.id, pr.name, pr.color, pr.size, sum(d.quantity),"
                            + " sum(d.subtotal) from Payment p join p.order o join o.details d join d.product pr where p.status = :paid"
                            + " and p.paidAt >= :s and p.paidAt < :e group by pr.id, pr.name, pr.color, pr.size"
                            + " order by sum(d.subtotal) desc", Object[].class)
                    .setParameter("paid", PaymentStatus.PAID).setParameter("s", start).setParameter("e", end)
                    .getResultList().stream()
                    .map(r -> new StatisticsReport.RowStat((Integer) r[0], (String) r[1], (String) r[2], (String) r[3],
                            ((Number) r[4]).longValue(), money(r[5]))).toList();

            boolean monthly = ChronoUnit.DAYS.between(from, to) >= DAILY_UP_TO;
            Map<String, BigDecimal[]> buckets = buckets(from, to, monthly);
            for (Object[] r : em.createQuery("select p.paidAt, o.totalAmount from Payment p join p.order o where p.status = :paid"
                    + " and p.paidAt >= :s and p.paidAt < :e", Object[].class)
                    .setParameter("paid", PaymentStatus.PAID).setParameter("s", start).setParameter("e", end).getResultList()) {
                LocalDate day = ((LocalDateTime) r[0]).toLocalDate();
                BigDecimal[] b = buckets.get(monthly ? MONTH.format(day) : DAY.format(day));
                b[0] = b[0].add((BigDecimal) r[1]);
                b[1] = b[1].add(BigDecimal.ONE);
            }
            for (LocalDateTime placedAt : em.createQuery("select o.orderDate from Payment p join p.order o where " + OrderDao.VISIBLE
                    + " and o.orderDate >= :s and o.orderDate < :e", LocalDateTime.class)
                    .setParameter("s", start).setParameter("e", end).getResultList()) {
                BigDecimal[] b = buckets.get(monthly ? MONTH.format(placedAt) : DAY.format(placedAt));
                b[2] = b[2].add(BigDecimal.ONE);
            }
            BigDecimal largest = buckets.values().stream().map(b -> b[0]).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            BigDecimal top = niceTop(largest);
            List<StatisticsReport.Tick> ticks = ticks(top);
            List<StatisticsReport.Bucket> series = new ArrayList<>();
            buckets.forEach((label, b) -> series.add(new StatisticsReport.Bucket(label, money(b[0]), b[1].longValue(),
                    b[2].longValue(), top.signum() == 0 ? 0 : b[0].multiply(BigDecimal.valueOf(100)).divide(top, 2, RoundingMode.HALF_UP).doubleValue())));

            long sold = products.stream().mapToLong(StatisticsReport.ProductStat::quantity).sum();
            return new StatisticsReport(from, to, money(totals[0]), (Long) totals[1], byStatus, placed, sold, products, rows,
                    series, monthly, Math.max(1, (series.size() + 11) / 12), top, ticks, stock(em));
        });
    }

    /** Statistics.stockReport(): current stock per product and row, products not on sale included. */
    private static List<StatisticsReport.StockStat> stock(EntityManager em) {
        Map<String, List<Product>> groups = new LinkedHashMap<>();
        for (Product p : em.createQuery("select p from Product p order by p.name, p.id", Product.class).getResultList()) {
            groups.computeIfAbsent(p.getName().toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(p);
        }
        return groups.values().stream().map(g -> new StatisticsReport.StockStat(g.get(0).getId(), g.get(0).getName(),
                g.get(0).isActive(), g.stream().mapToInt(Product::getStockQuantity).sum(),
                g.stream().map(p -> new StatisticsReport.StockRow(p.getColor(), p.getSize(), p.getStockQuantity())).toList())).toList();
    }

    /** Empty buckets for every day (or month) of the period, in order: [revenue, paid orders, placed orders]. */
    private static Map<String, BigDecimal[]> buckets(LocalDate from, LocalDate to, boolean monthly) {
        Map<String, BigDecimal[]> buckets = new LinkedHashMap<>();
        if (monthly) {
            for (YearMonth m = YearMonth.from(from); !m.isAfter(YearMonth.from(to)); m = m.plusMonths(1)) {
                buckets.put(MONTH.format(m.atDay(1)), new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            }
        } else {
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                buckets.put(DAY.format(d), new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            }
        }
        return buckets;
    }

    /** Gridlines at round steps: a top of 5 × 10^k gets 5 steps, 1 or 2 × 10^k gets 4 (0,25 or 0,5 × 10^k). */
    static List<StatisticsReport.Tick> ticks(BigDecimal top) {
        int steps = top.unscaledValue().toString().startsWith("5") ? 5 : 4;
        List<StatisticsReport.Tick> ticks = new ArrayList<>();
        for (int i = 0; i <= steps; i++) {
            ticks.add(new StatisticsReport.Tick(top.multiply(BigDecimal.valueOf(i)).divide(BigDecimal.valueOf(steps), 0, RoundingMode.HALF_UP),
                    i * 100.0 / steps));
        }
        return ticks;
    }

    /** A round top for the chart scale: 1, 2 or 5 × a power of ten, at or above {@code value} (1.000.000 when empty). */
    static BigDecimal niceTop(BigDecimal value) {
        if (value.signum() <= 0) {
            return new BigDecimal("1000000");
        }
        BigDecimal power = BigDecimal.ONE;
        while (power.multiply(BigDecimal.TEN).compareTo(value) <= 0) {
            power = power.multiply(BigDecimal.TEN);
        }
        for (int step : new int[] {1, 2, 5, 10}) {
            BigDecimal candidate = power.multiply(BigDecimal.valueOf(step));
            if (candidate.compareTo(value) >= 0) {
                return candidate;
            }
        }
        return power.multiply(BigDecimal.TEN);
    }

    private static BigDecimal money(Object value) {
        BigDecimal amount = value instanceof BigDecimal b ? b : value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
        return amount.setScale(0, RoundingMode.HALF_UP);
    }
}
