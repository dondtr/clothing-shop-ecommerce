package com.clothingshop.dao;

import com.clothingshop.entity.Review;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Data access for product reviews ({@code Review}).
 */
public class ReviewDao {

    private final EntityManager em;

    public ReviewDao(EntityManager em) {
        this.em = em;
    }

    /** Reviews of every row of the product called {@code name}, newest first, with the author loaded (FR-08). */
    public List<Review> findByProductName(String name) {
        return em.createQuery("select r from Review r join fetch r.customer join r.product p"
                        + " where lower(p.name) = lower(:name) order by r.createdAt desc, r.id desc", Review.class)
                .setParameter("name", name).getResultList();
    }

    public Optional<Review> findById(int id) {
        return Optional.ofNullable(em.find(Review.class, id));
    }

    /** The customer's reviews as rows of review id and product name (one review per product, BR-14, Q44). */
    public List<Object[]> findIdAndProductNameByCustomer(int customerId) {
        return em.createQuery("select r.id, r.product.name from Review r where r.customer.id = :c", Object[].class)
                .setParameter("c", customerId).getResultList();
    }

    /** Whether the customer already reviewed a row of the product called {@code productName}. */
    public boolean existsForProductName(int customerId, String productName) {
        return em.createQuery("select count(r) from Review r where r.customer.id = :c"
                        + " and lower(r.product.name) = lower(:name)", Long.class)
                .setParameter("c", customerId).setParameter("name", productName).getSingleResult() > 0;
    }

    /** Ids among {@code productIds} of rows that have at least one review. */
    public List<Integer> findReviewedProductIds(Collection<Integer> productIds) {
        return em.createQuery("select distinct r.product.id from Review r where r.product.id in :ids", Integer.class)
                .setParameter("ids", productIds).getResultList();
    }

    public void persist(Review review) {
        em.persist(review);
    }

    public void remove(Review review) {
        em.remove(review);
    }
}
