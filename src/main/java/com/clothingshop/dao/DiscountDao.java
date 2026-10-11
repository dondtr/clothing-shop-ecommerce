package com.clothingshop.dao;

import com.clothingshop.entity.Discount;
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
}
