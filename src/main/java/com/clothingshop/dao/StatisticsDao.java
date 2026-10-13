package com.clothingshop.dao;

import com.clothingshop.entity.PaymentStatus;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Aggregate queries for the business statistics (FR-43), over [start, end). Revenue and sales count payments that are
 * PAID by paidAt (Q33); order counts use orderDate and exclude orders waiting for a VNPAY payment (BR-26).
 */
public class StatisticsDao {

    private final EntityManager em;

    public StatisticsDao(EntityManager em) {
        this.em = em;
    }

    /** Σ Order.totalAmount and the number of orders paid in the period: one row {sum, count}. */
    public Object[] paidTotals(LocalDateTime start, LocalDateTime end) {
        return em.createQuery("select coalesce(sum(o.totalAmount), 0), count(o) from Payment p join p.order o"
                        + " where p.status = :paid and p.paidAt >= :s and p.paidAt < :e", Object[].class)
                .setParameter("paid", PaymentStatus.PAID).setParameter("s", start).setParameter("e", end).getSingleResult();
    }

    /** Orders placed in the period per status: rows of status, count. */
    public List<Object[]> placedByStatus(LocalDateTime start, LocalDateTime end) {
        return em.createQuery("select o.status, count(o) from Payment p join p.order o where " + OrderDao.VISIBLE
                        + " and o.orderDate >= :s and o.orderDate < :e group by o.status", Object[].class)
                .setParameter("s", start).setParameter("e", end).getResultList();
    }

    /** Sales per product (rows sharing a name) of orders paid in the period: rows of name, Σ quantity, Σ subtotal. */
    public List<Object[]> soldByProduct(LocalDateTime start, LocalDateTime end) {
        return em.createQuery("select pr.name, sum(d.quantity), sum(d.subtotal)"
                        + " from Payment p join p.order o join o.details d join d.product pr where p.status = :paid"
                        + " and p.paidAt >= :s and p.paidAt < :e group by pr.name order by sum(d.subtotal) desc", Object[].class)
                .setParameter("paid", PaymentStatus.PAID).setParameter("s", start).setParameter("e", end)
                .getResultList();
    }

    /** Sales per variant row of orders paid in the period: rows of id, name, color, size, Σ quantity, Σ subtotal. */
    public List<Object[]> soldByRow(LocalDateTime start, LocalDateTime end) {
        return em.createQuery("select pr.id, pr.name, pr.color, pr.size, sum(d.quantity),"
                        + " sum(d.subtotal) from Payment p join p.order o join o.details d join d.product pr where p.status = :paid"
                        + " and p.paidAt >= :s and p.paidAt < :e group by pr.id, pr.name, pr.color, pr.size"
                        + " order by sum(d.subtotal) desc", Object[].class)
                .setParameter("paid", PaymentStatus.PAID).setParameter("s", start).setParameter("e", end)
                .getResultList();
    }

    /** Every order paid in the period: rows of paidAt, totalAmount (for the revenue chart). */
    public List<Object[]> paidAmounts(LocalDateTime start, LocalDateTime end) {
        return em.createQuery("select p.paidAt, o.totalAmount from Payment p join p.order o where p.status = :paid"
                        + " and p.paidAt >= :s and p.paidAt < :e", Object[].class)
                .setParameter("paid", PaymentStatus.PAID).setParameter("s", start).setParameter("e", end).getResultList();
    }

    /** The order date of every order placed in the period (for the chart's order counts). */
    public List<LocalDateTime> placedDates(LocalDateTime start, LocalDateTime end) {
        return em.createQuery("select o.orderDate from Payment p join p.order o where " + OrderDao.VISIBLE
                        + " and o.orderDate >= :s and o.orderDate < :e", LocalDateTime.class)
                .setParameter("s", start).setParameter("e", end).getResultList();
    }
}
