package com.clothingshop.dao;

import com.clothingshop.entity.Customer;
import com.clothingshop.entity.PaymentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Data access for customer accounts ({@code Customer}, a subtype of {@code User}).
 */
public class CustomerDao {

    private final EntityManager em;

    public CustomerDao(EntityManager em) {
        this.em = em;
    }

    public Optional<Customer> findById(int id) {
        return Optional.ofNullable(em.find(Customer.class, id));
    }

    /** The customer locked for update, so changes for one customer (default address, review) run one after the other. */
    public Optional<Customer> lock(int id) {
        return Optional.ofNullable(em.find(Customer.class, id, LockModeType.PESSIMISTIC_WRITE));
    }

    /** A reference to the customer without loading it, to link a new row to an existing customer. */
    public Customer reference(int id) {
        return em.getReference(Customer.class, id);
    }

    /** Customer.getPaidTotal(): Σ Order.totalAmount of the customer's orders whose payment is PAID (Q32). */
    public BigDecimal paidTotal(int customerId) {
        return em.createQuery("select coalesce(sum(o.totalAmount), 0) from Payment p join p.order o"
                        + " where o.customer.id = :c and p.status = :paid", BigDecimal.class)
                .setParameter("c", customerId).setParameter("paid", PaymentStatus.PAID).getSingleResult();
    }

    /** Customers whose name, email or phone contains {@code query} (all when empty), newest first, at most {@code limit}. */
    public List<Customer> search(String query, int limit) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return em.createQuery("select c from Customer c where lower(c.fullName) like :q escape '!'"
                        + " or lower(c.email) like :q escape '!' or c.phone like :q escape '!' order by c.createdAt desc", Customer.class)
                .setParameter("q", "%" + q.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%")
                .setMaxResults(limit)
                .getResultList();
    }

    public long count() {
        return em.createQuery("select count(c) from Customer c", Long.class).getSingleResult();
    }
}
