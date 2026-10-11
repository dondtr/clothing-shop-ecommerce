package com.clothingshop.dao;

import com.clothingshop.entity.Product;
import com.clothingshop.entity.ProductCategory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
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

    /** Every row of the products with the given names (letter case ignored), on sale or not. */
    public List<Product> findByNames(Collection<String> names) {
        if (names.isEmpty()) {
            return List.of();
        }
        List<String> lower = names.stream().map(n -> n.toLowerCase(Locale.ROOT)).distinct().toList();
        return em.createQuery("select p from Product p where lower(p.name) in :names order by p.name, p.id", Product.class)
                .setParameter("names", lower).getResultList();
    }

    /** Every row of the product called {@code name}, locked for update so concurrent changes run one after the other. */
    public List<Product> lockByName(String name) {
        return em.createQuery("select p from Product p where lower(p.name) = lower(:name) order by p.id", Product.class)
                .setParameter("name", name).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
    }

    /** Colors of the rows on sale, for the shop filter. */
    public List<String> findActiveColors() {
        return em.createQuery("select distinct p.color from Product p where p.active = true order by p.color", String.class)
                .getResultList();
    }

    /**
     * Product.updateStock(−n): takes {@code quantity} from the row only when that much is left, in one conditional UPDATE
     * so that concurrent orders cannot sell more than the stock (section 10). The version is increased as well, so
     * optimistic locking still sees the change (section 3.1).
     *
     * @return {@code false} when the stock was not enough (nothing changed)
     */
    public boolean decreaseStock(int productId, int quantity) {
        return em.createQuery("update Product p set p.stockQuantity = p.stockQuantity - :q, p.version = p.version + 1"
                        + " where p.id = :id and p.stockQuantity >= :q")
                .setParameter("q", quantity).setParameter("id", productId).executeUpdate() == 1;
    }

    /** Product.updateStock(+n): gives stock back (order cancelled, return approved). */
    public void increaseStock(int productId, int quantity) {
        em.createQuery("update Product p set p.stockQuantity = p.stockQuantity + :q, p.version = p.version + 1 where p.id = :id")
                .setParameter("q", quantity).setParameter("id", productId).executeUpdate();
    }

    /** Stock read from the database, bypassing rows already loaded in this persistence context. */
    public int stockOf(int productId) {
        return em.createQuery("select p.stockQuantity from Product p where p.id = :id", Integer.class)
                .setParameter("id", productId).getResultStream().findFirst().orElse(0);
    }

    /** Makes {@code %} and {@code _} typed by the user match literally. */
    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
