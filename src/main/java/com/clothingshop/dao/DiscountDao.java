package com.clothingshop.dao;

import com.clothingshop.entity.Discount;
import com.clothingshop.entity.Product;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data access for product discounts ({@code Discount}).
 */
public class DiscountDao {

    /** Keeps each IN list a reasonable size. */
    private static final int IN_CHUNK = 500;

    private final EntityManager em;

    public DiscountDao(EntityManager em) {
        this.em = em;
    }

    /**
     * The discount in effect at {@code now} for each of the given rows, keyed by product id; rows without one are absent.
     * FR-34 forbids overlapping discounts on a product, so there is at most one; if bad data ever has two, the larger wins.
     */
    public Map<Integer, Discount> findAvailable(Collection<Integer> productIds, LocalDateTime now) {
        Map<Integer, Discount> result = new HashMap<>();
        List<Integer> ids = new ArrayList<>(productIds);
        for (int from = 0; from < ids.size(); from += IN_CHUNK) {
            List<Discount> found = em.createQuery("select d from Discount d where d.product.id in :ids"
                            + " and d.startDate <= :now and d.endDate >= :now", Discount.class)
                    .setParameter("ids", ids.subList(from, Math.min(ids.size(), from + IN_CHUNK)))
                    .setParameter("now", now)
                    .getResultList();
            for (Discount d : found) {
                result.merge(d.getProduct().getId(), d,
                        (a, b) -> a.getDiscountValue().compareTo(b.getDiscountValue()) >= 0 ? a : b);
            }
        }
        return result;
    }

    /** Every discount of the given rows (one product), the latest start first. */
    public List<Discount> findByRows(List<Product> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        return em.createQuery("select d from Discount d where d.product in :rows order by d.startDate desc, d.id", Discount.class)
                .setParameter("rows", rows).getResultList();
    }

    /** The discounts of one campaign: the rows' discounts that start at {@code start} (FR-34). */
    public List<Discount> findCampaign(List<Product> rows, LocalDateTime start) {
        return em.createQuery("select d from Discount d where d.product in :rows and d.startDate = :start", Discount.class)
                .setParameter("rows", rows).setParameter("start", start).getResultList();
    }

    /**
     * Campaigns of products on sale that start in [from, to) (J4, FR-11): rows of product name, discount value, start,
     * end and the smallest row id of the product.
     */
    public List<Object[]> findStartingBetween(LocalDateTime from, LocalDateTime to) {
        return em.createQuery("select p.name, d.discountValue, d.startDate, d.endDate, min(p.id)"
                        + " from Discount d join d.product p where d.startDate >= :today and d.startDate < :tomorrow"
                        + " and p.active = true group by p.name, d.discountValue, d.startDate, d.endDate", Object[].class)
                .setParameter("today", from).setParameter("tomorrow", to).getResultList();
    }

    /** Deletes every discount of a row (the row itself is being deleted, FR-31 step 5). */
    public void deleteByProduct(int productId) {
        em.createQuery("delete from Discount d where d.product.id = :id").setParameter("id", productId).executeUpdate();
    }

    public void persist(Discount discount) {
        em.persist(discount);
    }
}
