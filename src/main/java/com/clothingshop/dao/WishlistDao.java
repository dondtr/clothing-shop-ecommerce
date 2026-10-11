package com.clothingshop.dao;

import com.clothingshop.entity.Wishlist;
import jakarta.persistence.EntityManager;
import java.util.Optional;

/**
 * Data access for wishlists ({@code Wishlist} and the join table {@code WishlistItem}).
 */
public class WishlistDao {

    private final EntityManager em;

    public WishlistDao(EntityManager em) {
        this.em = em;
    }

    /** The customer's wishlist with its product rows loaded; empty before the first product is added. */
    public Optional<Wishlist> findByCustomer(int customerId) {
        return em.createQuery("select distinct w from Wishlist w left join fetch w.products where w.customer.id = :id",
                        Wishlist.class)
                .setParameter("id", customerId).getResultStream().findFirst();
    }
}
