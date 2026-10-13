package com.clothingshop.dao;

import com.clothingshop.entity.Wishlist;
import jakarta.persistence.EntityManager;
import java.util.List;
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

    /** Wishlists holding the row {@code productId}. */
    public List<Wishlist> findContaining(int productId) {
        return em.createQuery("select w from Wishlist w join w.products p where p.id = :id", Wishlist.class)
                .setParameter("id", productId).getResultList();
    }

    /** Customers with a row of the product called {@code productName} in their wishlist: rows of email, full name. */
    public List<Object[]> findCustomersWatching(String productName) {
        return em.createQuery("select distinct cu.email, cu.fullName from Wishlist w join w.customer cu"
                        + " join w.products p where lower(p.name) = lower(:name)", Object[].class)
                .setParameter("name", productName).getResultList();
    }

    public void persist(Wishlist wishlist) {
        em.persist(wishlist);
    }
}
