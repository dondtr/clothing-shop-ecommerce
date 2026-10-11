package com.clothingshop.dao;

import com.clothingshop.entity.Product;
import com.clothingshop.entity.ProductCategory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Data access for product variant rows ({@code Product}). Rows with the same name form one product (Q44); names are
 * compared without regard to letter case (section 3.1), so {@code lower()} is used on both sides.
 */
public class ProductDao {

    private final EntityManager em;

    public ProductDao(EntityManager em) {
        this.em = em;
    }

    public Optional<Product> findById(int id) {
        return Optional.ofNullable(em.find(Product.class, id));
    }

    /** The rows with the given ids that exist (deleted rows are simply absent). */
    public List<Product> findByIds(Collection<Integer> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return em.createQuery("select p from Product p where p.id in :ids", Product.class)
                .setParameter("ids", ids).getResultList();
    }

    /**
     * Rows on sale (active = TRUE), optionally of one category and whose name contains {@code nameContains}.
     * Both filters apply to values shared by every row of a product, so a product is either fully returned or not at all.
     */
    public List<Product> findActive(ProductCategory category, String nameContains) {
        StringBuilder jpql = new StringBuilder("select p from Product p where p.active = true");
        if (category != null) {
            jpql.append(" and p.category = :category");
        }
        if (nameContains != null) {
            jpql.append(" and lower(p.name) like :name escape '!'");
        }
        jpql.append(" order by p.name, p.id");
        TypedQuery<Product> query = em.createQuery(jpql.toString(), Product.class);
        if (category != null) {
            query.setParameter("category", category);
        }
        if (nameContains != null) {
            query.setParameter("name", "%" + escapeLike(nameContains.toLowerCase(Locale.ROOT)) + "%");
        }
        return query.getResultList();
    }

    /** Every row of the product called {@code name}, on sale or not, in the order they were created. */
    public List<Product> findByName(String name) {
        return em.createQuery("select p from Product p where lower(p.name) = lower(:name) order by p.id", Product.class)
                .setParameter("name", name).getResultList();
    }

    /** Colors of the rows on sale, for the shop filter. */
    public List<String> findActiveColors() {
        return em.createQuery("select distinct p.color from Product p where p.active = true order by p.color", String.class)
                .getResultList();
    }

    /** Makes {@code %} and {@code _} typed by the user match literally. */
    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
