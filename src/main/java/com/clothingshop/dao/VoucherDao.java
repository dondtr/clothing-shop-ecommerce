package com.clothingshop.dao;

import com.clothingshop.entity.EligibleCustomer;
import com.clothingshop.entity.Voucher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    /** The voucher with this code (codes are stored in capitals, BR-22). */
    public Optional<Voucher> findByCode(String code) {
        return Optional.ofNullable(em.find(Voucher.class, code));
    }

    /** The voucher locked for update, so a save cannot overlap another change. */
    public Optional<Voucher> lock(String code) {
        return Optional.ofNullable(em.find(Voucher.class, code, LockModeType.PESSIMISTIC_WRITE));
    }

    /** Every voucher, the latest start first (FR-36). */
    public List<Voucher> findAll() {
        return em.createQuery("select v from Voucher v order by v.policy.startDate desc, v.code", Voucher.class).getResultList();
    }

    /** Vouchers whose code starts with {@code prefix} (holiday campaigns, FR-38). */
    public List<Voucher> findByCodePrefix(String prefix) {
        return em.createQuery("select v from Voucher v where v.code like :prefix", Voucher.class)
                .setParameter("prefix", prefix + "%").getResultList();
    }

    /** Vouchers with uses left, valid at {@code now}, for one of the given customer groups (FR-39), ending soonest first. */
    public List<Voucher> findUsable(Collection<EligibleCustomer> eligible, LocalDateTime now) {
        return em.createQuery("select v from Voucher v where v.quantity > 0 and v.policy.startDate <= :now"
                        + " and v.policy.endDate >= :now and v.policy.eligibleCustomer in :eligible"
                        + " order by v.policy.endDate, v.code", Voucher.class)
                .setParameter("now", now)
                .setParameter("eligible", eligible)
                .getResultList();
    }

    /** Saves a new voucher with its policy (secondary table). */
    public void persist(Voucher voucher) {
        em.persist(voucher);
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
