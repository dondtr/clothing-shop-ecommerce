package com.clothingshop.dao;

import jakarta.persistence.EntityManager;

/**
 * Data access for vouchers ({@code Voucher} with its {@code VoucherPolicy}).
 *
 * <p>The quantity updates are native SQL on the table {@code Voucher}: the entity spans two tables (secondary table
 * {@code VoucherPolicy}), and a JPQL bulk update on such an entity may make Hibernate create temporary tables, which the
 * application's database account is not allowed to do.
 */
public class VoucherDao {

    private final EntityManager em;

    public VoucherDao(EntityManager em) {
        this.em = em;
    }

    /**
     * Voucher.decreaseQuantity(): takes one use only when one is left, in one conditional UPDATE, so concurrent orders
     * cannot use more than the quantity (section 10); the version is increased too (section 3.1).
     *
     * @return {@code false} when no use was left
     */
    public boolean decreaseQuantity(String code) {
        return em.createNativeQuery("UPDATE Voucher SET quantity = quantity - 1, version = version + 1"
                        + " WHERE code = ?1 AND quantity > 0")
                .setParameter(1, code).executeUpdate() == 1;
    }

    /** Voucher.increaseQuantity(): gives the use back when the order is cancelled or returned (BR-05, Q29). */
    public void increaseQuantity(String code) {
        em.createNativeQuery("UPDATE Voucher SET quantity = quantity + 1, version = version + 1 WHERE code = ?1")
                .setParameter(1, code).executeUpdate();
    }
}
