package com.clothingshop.dao;

import com.clothingshop.entity.Payment;
import com.clothingshop.entity.PaymentMethod;
import com.clothingshop.entity.PaymentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Data access for payments ({@code Payment}); every order has exactly one, created with it. The plain lookup used by
 * order pages is {@link OrderDao#paymentOf(int)}.
 */
public class PaymentDao {

    private final EntityManager em;

    public PaymentDao(EntityManager em) {
        this.em = em;
    }

    /** The payment of an order with the order loaded; empty when the order does not exist (any more). */
    public Optional<Payment> findByOrder(int orderId) {
        return em.createQuery("select p from Payment p join fetch p.order where p.order.id = :o", Payment.class)
                .setParameter("o", orderId).getResultStream().findFirst();
    }

    /** As {@link #findByOrder(int)}, locked for update so IPN, the return page and J7 cannot handle it twice. */
    public Optional<Payment> lockByOrder(int orderId) {
        return em.createQuery("select p from Payment p join fetch p.order where p.order.id = :o", Payment.class)
                .setParameter("o", orderId).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst();
    }

    /**
     * Orders waiting for a VNPAY payment placed at or before {@code cutoff} (J7): rows of order id, order date, total.
     */
    public List<Object[]> findVnpayUnpaidPlacedBefore(LocalDateTime cutoff) {
        return em.createQuery("select o.id, o.orderDate, o.totalAmount from Payment p"
                        + " join p.order o where p.method = :vnpay and p.status = :unpaid and o.orderDate <= :cutoff", Object[].class)
                .setParameter("vnpay", PaymentMethod.VNPAY).setParameter("unpaid", PaymentStatus.UNPAID)
                .setParameter("cutoff", cutoff).getResultList();
    }

    public void persist(Payment payment) {
        em.persist(payment);
    }

    public void remove(Payment payment) {
        em.remove(payment);
    }
}
