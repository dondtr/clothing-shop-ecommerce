package com.clothingshop.dao;

import com.clothingshop.entity.ReturnRequest;
import com.clothingshop.entity.ReturnRequestStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

/**
 * Data access for return requests ({@code ReturnRequest}, FR-25, FR-26). An order has at most one request.
 */
public class ReturnRequestDao {

    private final EntityManager em;

    public ReturnRequestDao(EntityManager em) {
        this.em = em;
    }

    public Optional<ReturnRequest> findById(int id) {
        return Optional.ofNullable(em.find(ReturnRequest.class, id));
    }

    /** The request locked for update, so two employees cannot decide on it at the same time. */
    public Optional<ReturnRequest> lock(int id) {
        return Optional.ofNullable(em.find(ReturnRequest.class, id, LockModeType.PESSIMISTIC_WRITE));
    }

    /** The request of an order, with the employee who handled it loaded. */
    public Optional<ReturnRequest> findByOrder(int orderId) {
        return em.createQuery("select r from ReturnRequest r left join fetch r.handledBy where r.order.id = :o", ReturnRequest.class)
                .setParameter("o", orderId).getResultStream().findFirst();
    }

    public boolean existsForOrder(int orderId) {
        return em.createQuery("select count(r) from ReturnRequest r where r.order.id = :o", Long.class)
                .setParameter("o", orderId).getSingleResult() > 0;
    }

    /** The customer's requests, newest first, with their orders loaded. */
    public List<ReturnRequest> findByCustomer(int customerId) {
        return em.createQuery("select r from ReturnRequest r join fetch r.order o"
                        + " where o.customer.id = :c order by r.requestedAt desc", ReturnRequest.class)
                .setParameter("c", customerId).getResultList();
    }

    /**
     * Requests waiting for a decision, oldest first ({@code pending}), or the decided ones, newest first; orders, customers
     * and handling employees loaded.
     */
    public List<ReturnRequest> findForStaff(boolean pending, int limit) {
        return em.createQuery("select r from ReturnRequest r join fetch r.order o join fetch o.customer"
                        + " left join fetch r.handledBy where r.status " + (pending ? "=" : "<>") + " :p order by r.requestedAt "
                        + (pending ? "asc" : "desc"), ReturnRequest.class)
                .setParameter("p", ReturnRequestStatus.PENDING).setMaxResults(limit)
                .getResultList();
    }

    public long countPending() {
        return em.createQuery("select count(r) from ReturnRequest r where r.status = :p", Long.class)
                .setParameter("p", ReturnRequestStatus.PENDING).getSingleResult();
    }

    public void persist(ReturnRequest request) {
        em.persist(request);
    }
}
