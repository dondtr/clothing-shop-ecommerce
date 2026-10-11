package com.clothingshop.dao;

import com.clothingshop.dto.OrderRow;
import com.clothingshop.entity.Order;
import com.clothingshop.entity.OrderStatus;
import com.clothingshop.entity.Payment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data access for orders and their payments. Orders waiting for a VNPAY payment (Payment VNPAY + UNPAID) are hidden
 * from customers, staff and statistics (BR-26); every "visible" query excludes them.
 */
public class OrderDao {

    /** JPQL condition, on {@code p} (Payment), that excludes orders waiting for a VNPAY payment (BR-26). */
    public static final String VISIBLE = "not (p.method = com.clothingshop.entity.PaymentMethod.VNPAY"
            + " and p.status = com.clothingshop.entity.PaymentStatus.UNPAID)";

    private final EntityManager em;

    public OrderDao(EntityManager em) {
        this.em = em;
    }

    public Optional<Order> findById(int id) {
        return Optional.ofNullable(em.find(Order.class, id));
    }

    /** The order locked for update, so concurrent status changes run one after the other. */
    public Optional<Order> lock(int id) {
        return Optional.ofNullable(em.find(Order.class, id, LockModeType.PESSIMISTIC_WRITE));
    }

    /** The payment of an order (every order has exactly one, created with it). */
    public Payment paymentOf(int orderId) {
        return em.createQuery("select p from Payment p where p.order.id = :o", Payment.class)
                .setParameter("o", orderId).getSingleResult();
    }

    /** Whether the order exists and is not waiting for a VNPAY payment. */
    public boolean isVisible(int orderId) {
        return em.createQuery("select count(p) from Payment p where p.order.id = :o and " + VISIBLE, Long.class)
                .setParameter("o", orderId).getSingleResult() > 0;
    }

    /** FR-16 step 1: the customer's orders, newest first. */
    public List<OrderRow> findVisibleByCustomer(int customerId) {
        return rows(em.createQuery("select o, p from Payment p join p.order o join fetch o.customer c"
                        + " where c.id = :c and " + VISIBLE + " order by o.orderDate desc, o.id desc", Object[].class)
                .setParameter("c", customerId));
    }

    /** Orders for staff, newest first, optionally of one status. */
    public List<OrderRow> findVisible(OrderStatus status, int limit) {
        TypedQuery<Object[]> query = em.createQuery("select o, p from Payment p join p.order o join fetch o.customer c where "
                + VISIBLE + (status == null ? "" : " and o.status = :s") + " order by o.orderDate desc, o.id desc", Object[].class);
        if (status != null) {
            query.setParameter("s", status);
        }
        return rows(query.setMaxResults(limit));
    }

    /** Number of visible orders in each status. */
    public Map<OrderStatus, Long> countVisibleByStatus() {
        Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
        for (OrderStatus s : OrderStatus.values()) {
            counts.put(s, 0L);
        }
        for (Object[] row : em.createQuery("select o.status, count(o) from Payment p join p.order o where " + VISIBLE
                + " group by o.status", Object[].class).getResultList()) {
            counts.put((OrderStatus) row[0], (Long) row[1]);
        }
        return counts;
    }

    private static List<OrderRow> rows(TypedQuery<Object[]> query) {
        return query.getResultList().stream().map(r -> {
            Order o = (Order) r[0];
            Payment p = (Payment) r[1];
            return new OrderRow(o.getId(), o.getCustomer().getFullName(), o.getOrderDate(), o.getTotalAmount(), o.getStatus(),
                    p.getMethod(), p.getStatus());
        }).toList();
    }
}
