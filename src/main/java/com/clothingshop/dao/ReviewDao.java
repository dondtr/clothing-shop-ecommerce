package com.clothingshop.dao;

import com.clothingshop.entity.Review;
import jakarta.persistence.EntityManager;
import java.util.List;

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
}
