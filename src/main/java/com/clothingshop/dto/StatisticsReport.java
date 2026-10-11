package com.clothingshop.dto;

import com.clothingshop.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Business statistics for a period (FR-43). Revenue counts orders whose payment is PAID, by paidAt (Q33); COD orders
 * delivered count as paid (Q30). Orders waiting for a VNPAY payment are never counted (BR-26).
 *
 * @param revenue        Σ Order.totalAmount, shipping fee included
 * @param ordersByStatus orders placed in the period (orderDate), per status
 * @param soldQuantity   Σ OrderDetail.quantity of the paid orders
 * @param products       per product: quantity sold and Σ subtotal (before vouchers, without shipping fee, Q45)
 * @param series         revenue and orders per day (per month for periods of 63 days or more)
 * @param labelEvery     the chart labels one bar in this many, so that at most 12 labels show
 * @param seriesMax      top of the chart's scale, a round amount at or above the largest bucket
 */
public record StatisticsReport(LocalDate from, LocalDate to, BigDecimal revenue, long paidOrders,
                               Map<OrderStatus, Long> ordersByStatus, long placedOrders, long soldQuantity,
                               List<ProductStat> products,
                               List<RowStat> rows, List<Bucket> series, boolean monthly, int labelEvery,
                               BigDecimal seriesMax, List<Tick> ticks, List<StockStat> stock) {

    /** One product (rows sharing a name). */
    public record ProductStat(String name, long quantity, BigDecimal revenue) {
    }

    /** One variant row. */
    public record RowStat(int id, String name, String color, String size, long quantity, BigDecimal revenue) {
    }

    /**
     * One bar of the revenue chart.
     *
     * @param paidOrders    orders paid that day (paidAt)
     * @param placedOrders  orders placed that day (orderDate), waiting VNPAY payments excluded
     * @param heightPercent bar height relative to {@code seriesMax}
     */
    public record Bucket(String label, BigDecimal revenue, long paidOrders, long placedOrders, double heightPercent) {
    }

    /** A gridline of the chart, {@code percent} of the way up the scale. */
    public record Tick(BigDecimal amount, double percent) {
    }

    /** Current stock of a product and its rows. */
    public record StockStat(int firstId, String name, boolean active, int total, List<StockRow> rows) {
    }

    public record StockRow(String color, String size, int stock) {
    }
}
