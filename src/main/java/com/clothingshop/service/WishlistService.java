package com.clothingshop.service;

import com.clothingshop.dao.DiscountDao;
import com.clothingshop.dao.ProductDao;
import com.clothingshop.dao.WishlistDao;
import com.clothingshop.dto.WishlistEntry;
import com.clothingshop.entity.Customer;
import com.clothingshop.entity.Discount;
import com.clothingshop.entity.Product;
import com.clothingshop.entity.Wishlist;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.util.JpaUtil;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.hibernate.StaleStateException;
import org.hibernate.exception.ConstraintViolationException;

/**
 * Wishlist of a signed-in customer (FR-10). A product (all rows sharing a name) appears at most once, whichever row the
 * customer was looking at when adding it (Q44); the saved row is the one on the page.
 */
public class WishlistService {

    /** Section 3.1: another request saved the same wishlist first (optimistic locking). */
    static final String MSG_CONFLICT = "Dữ liệu đã được người khác thay đổi, vui lòng tải lại";
    static final String MSG_NOT_FOUND = "Sản phẩm không tồn tại hoặc đã bị xóa.";
    static final String MSG_NOT_SOLD = "Sản phẩm đã ngừng bán.";

    private final Clock clock;

    public WishlistService(Clock clock) {
        this.clock = clock;
    }

    /**
     * FR-10 steps 1 – 2: adds the product of row {@code productId}; the first addition creates the wishlist.
     *
     * @return {@code false} when the product was already in the wishlist (nothing changes)
     */
    public boolean add(int customerId, int productId) {
        return write(() -> JpaUtil.inTransaction(em -> {
            Product product = new ProductDao(em).findById(productId).orElseThrow(() -> new BusinessException(MSG_NOT_FOUND));
            if (!product.isActive()) {
                throw new BusinessException(MSG_NOT_SOLD);
            }
            Wishlist wishlist = new WishlistDao(em).findByCustomer(customerId).orElseGet(() -> {
                Wishlist created = new Wishlist(em.getReference(Customer.class, customerId));
                em.persist(created);
                return created;
            });
            if (wishlist.getProducts().stream().anyMatch(p -> sameProduct(p, product))) {
                return false;
            }
            wishlist.getProducts().add(product);
            return true;
        }));
    }

    /** FR-10 step 3: removes the product of row {@code productId}, whichever of its rows was saved. */
    public void remove(int customerId, int productId) {
        write(() -> JpaUtil.inTransaction(em -> {
            Optional<Product> row = new ProductDao(em).findById(productId);
            new WishlistDao(em).findByCustomer(customerId).ifPresent(wishlist -> wishlist.getProducts().removeIf(
                    p -> p.getId() == productId || row.isPresent() && sameProduct(p, row.get())));
            return null;
        }));
    }

    /** Whether the product called {@code productName} is in the customer's wishlist (for the button on the product page). */
    public boolean contains(int customerId, String productName) {
        return JpaUtil.read(em -> new WishlistDao(em).findByCustomer(customerId)
                .map(w -> w.getProducts().stream().anyMatch(p -> p.getName().equalsIgnoreCase(productName)))
                .orElse(false));
    }

    /** FR-10 step 4: the wishlist by product, at the current price; products no longer sold are kept and marked. */
    public List<WishlistEntry> list(int customerId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return JpaUtil.read(em -> {
            Optional<Wishlist> wishlist = new WishlistDao(em).findByCustomer(customerId);
            if (wishlist.isEmpty() || wishlist.get().getProducts().isEmpty()) {
                return List.of();
            }
            Set<String> names = wishlist.get().getProducts().stream().map(Product::getName).collect(Collectors.toSet());
            List<Product> rows = new ProductDao(em).findByNames(names);
            Map<Integer, Discount> discounts = new DiscountDao(em).findAvailable(CatalogService.ids(rows), now);
            List<WishlistEntry> entries = new ArrayList<>();
            for (List<Product> product : CatalogService.groupByName(rows)) {
                List<Product> sold = product.stream().filter(Product::isActive).toList();
                boolean active = !sold.isEmpty();
                entries.add(new WishlistEntry(CatalogService.summarize(active ? sold : product, discounts, null), active));
            }
            entries.sort(Comparator.comparing((WishlistEntry e) -> !e.active()).thenComparing(e -> e.product().name()));
            return List.copyOf(entries);
        });
    }

    /** Rows with the same name are the same product (Q44); names are compared without regard to letter case. */
    private static boolean sameProduct(Product a, Product b) {
        return a.getName().toLowerCase(Locale.ROOT).equals(b.getName().toLowerCase(Locale.ROOT));
    }

    /**
     * Two browsers changing the same wishlist at once: the later commit fails on the version check (or, for the very
     * first product, on the unique customerId) and the customer is asked to reload (section 3.1).
     */
    private static <T> T write(Supplier<T> work) {
        try {
            return work.get();
        } catch (PersistenceException e) {
            if (isConflict(e)) {
                throw new BusinessException(MSG_CONFLICT);
            }
            throw e;
        }
    }

    private static boolean isConflict(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof OptimisticLockException || t instanceof StaleStateException
                    || t instanceof ConstraintViolationException) {
                return true;
            }
        }
        return false;
    }
}
