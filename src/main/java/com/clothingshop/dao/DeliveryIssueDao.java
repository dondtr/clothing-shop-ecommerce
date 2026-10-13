package com.clothingshop.dao;

import com.clothingshop.entity.DeliveryIssue;
import com.clothingshop.entity.DeliveryIssueStatus;
import com.clothingshop.entity.OrderStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

/**
 * Data access for delivery issues ({@code DeliveryIssue}, "Chưa thấy hàng đâu", FR-20, FR-21). An order has at most one.
 */
public class DeliveryIssueDao {

    private final EntityManager em;

    public DeliveryIssueDao(EntityManager em) {
        this.em = em;
    }

    /** The issue locked for update, so two employees cannot handle it at the same time. */
    public Optional<DeliveryIssue> lock(int id) {
        return Optional.ofNullable(em.find(DeliveryIssue.class, id, LockModeType.PESSIMISTIC_WRITE));
    }

    /** The issue of an order, with the employee who resolved it loaded. */
    public Optional<DeliveryIssue> findByOrder(int orderId) {
        return em.createQuery("select i from DeliveryIssue i left join fetch i.resolvedBy where i.order.id = :o", DeliveryIssue.class)
                .setParameter("o", orderId).getResultStream().findFirst();
    }

    /** The issue of an order while it is still OPEN. */
    public Optional<DeliveryIssue> findOpenByOrder(int orderId) {
        return em.createQuery("select i from DeliveryIssue i where i.order.id = :o and i.status = :open", DeliveryIssue.class)
                .setParameter("o", orderId).setParameter("open", DeliveryIssueStatus.OPEN)
                .getResultStream().findFirst();
    }

    public boolean existsForOrder(int orderId) {
        return em.createQuery("select count(i) from DeliveryIssue i where i.order.id = :o", Long.class)
                .setParameter("o", orderId).getSingleResult() > 0;
    }

    /** OPEN issues of orders still being delivered, oldest first, with orders and customers loaded. */
    public List<DeliveryIssue> findOpenOfShippingOrders() {
        return em.createQuery("select i from DeliveryIssue i join fetch i.order o join fetch o.customer"
                        + " where i.status = :open and o.status = :s order by i.reportedAt", DeliveryIssue.class)
                .setParameter("open", DeliveryIssueStatus.OPEN).setParameter("s", OrderStatus.SHIPPING)
                .getResultList();
    }

    public long countOpenOfShippingOrders() {
        return em.createQuery("select count(i) from DeliveryIssue i where i.status = :open"
                        + " and i.order.status = :shipping", Long.class)
                .setParameter("open", DeliveryIssueStatus.OPEN).setParameter("shipping", OrderStatus.SHIPPING)
                .getSingleResult();
    }

    public void persist(DeliveryIssue issue) {
        em.persist(issue);
    }
}
