package com.clothingshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clothingshop.TestDatabase;
import com.clothingshop.dao.WishlistDao;
import com.clothingshop.dto.WishlistEntry;
import com.clothingshop.entity.Customer;
import com.clothingshop.entity.Discount;
import com.clothingshop.entity.Gender;
import com.clothingshop.entity.Product;
import com.clothingshop.entity.ProductCategory;
import com.clothingshop.entity.Wishlist;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ConflictException;
import com.clothingshop.util.JpaUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** FR-10 wishlist, against an in-memory database. */
class WishlistServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 10, 8, 0);

    private WishlistService wishlist;
    private String tag;
    private int customerId;

    @BeforeAll
    static void database() {
        TestDatabase.start();
    }

    @BeforeEach
    void setUp() {
        wishlist = new WishlistService(new MutableClock(NOW.atZone(ZONE).toInstant(), ZONE));
        tag = UUID.randomUUID().toString().substring(0, 8);
        customerId = JpaUtil.inTransaction(em -> {
            Customer c = new Customer("wl" + tag, "x", "Khách " + tag, Gender.FEMALE, LocalDate.of(2000, 1, 1),
                    "wl" + tag + "@example.com", "0900000000", NOW);
            em.persist(c);
            return c.getId();
        });
    }

    private Product row(String product, String size, boolean active) {
        return JpaUtil.inTransaction(em -> {
            Product p = new Product(product + " " + tag, ProductCategory.SHIRT, null, "assets/images/cloth_1.jpg",
                    new BigDecimal("200000"), "Trắng", size, 5, active, NOW);
            em.persist(p);
            return p;
        });
    }

    private String rejected(Runnable action) {
        return assertThrows(BusinessException.class, action::run).getMessage();
    }

    @Test
    void theFirstAdditionCreatesTheWishlistAndAProductIsListedOnce() {
        Product small = row("Áo len", "S", true);
        Product medium = row("Áo len", "M", true);
        assertTrue(wishlist.add(customerId, small.getId()));
        assertFalse(wishlist.add(customerId, medium.getId()), "another row of the same product is not added again");
        assertTrue(wishlist.contains(customerId, "ÁO LEN " + tag.toUpperCase()));

        List<WishlistEntry> entries = wishlist.list(customerId);
        assertEquals(1, entries.size());
        assertEquals("Áo len " + tag, entries.get(0).product().name());
        assertEquals(List.of(small.getId()), JpaUtil.read(em -> new WishlistDao(em).findByCustomer(customerId)
                .orElseThrow().getProducts().stream().map(Product::getId).toList()), "the row on the page is saved");
    }

    @Test
    void onlyProductsOnSaleCanBeAdded() {
        Product hidden = row("Mũ", "Free", false);
        assertEquals(WishlistService.MSG_NOT_SOLD, rejected(() -> wishlist.add(customerId, hidden.getId())));
        assertEquals(WishlistService.MSG_NOT_FOUND, rejected(() -> wishlist.add(customerId, -1)));
        assertTrue(wishlist.list(customerId).isEmpty());
    }

    @Test
    void anyRowOfTheProductRemovesIt() {
        Product small = row("Quần jean", "S", true);
        Product large = row("Quần jean", "L", true);
        wishlist.add(customerId, small.getId());
        wishlist.remove(customerId, large.getId());
        assertTrue(wishlist.list(customerId).isEmpty());
        assertFalse(wishlist.contains(customerId, small.getName()));
    }

    @Test
    void productsNoLongerSoldStayInTheListAfterTheOthers() {
        Product stopped = row("A ngừng bán", "M", true);
        Product sold = row("B đang bán", "M", true);
        JpaUtil.inTransaction(em -> {
            em.persist(new Discount(em.getReference(Product.class, sold.getId()), new BigDecimal("10"),
                    NOW.minusDays(1), NOW.plusDays(1)));
            return null;
        });
        wishlist.add(customerId, stopped.getId());
        wishlist.add(customerId, sold.getId());
        JpaUtil.inTransaction(em -> {
            em.find(Product.class, stopped.getId()).setActive(false);
            return null;
        });

        List<WishlistEntry> entries = wishlist.list(customerId);
        assertEquals(2, entries.size());
        assertTrue(entries.get(0).active());
        assertEquals(new BigDecimal("180000"), entries.get(0).product().currentPrice());
        assertFalse(entries.get(1).active(), "kept, but marked as no longer sold");
        assertTrue(entries.get(1).product().name().startsWith("A ngừng bán"));
    }

    @Test
    void aChangeBasedOnAnOldCopyOfTheWishlistIsRejected() {
        Product first = row("Áo một", "M", true);
        Product second = row("Áo hai", "M", true);
        Product third = row("Áo ba", "M", true);
        wishlist.add(customerId, first.getId());

        // Two browsers: this transaction reads the wishlist, the other one adds a product and commits first
        assertThrows(ConflictException.class, () -> JpaUtil.inTransaction(em -> {
            Wishlist stale = new WishlistDao(em).findByCustomer(customerId).orElseThrow();
            assertTrue(wishlist.add(customerId, second.getId()));
            stale.getProducts().add(em.find(Product.class, third.getId()));
            return null;
        }));
        assertEquals(2, wishlist.list(customerId).size(), "only the change committed first is kept");
    }
}
