package com.clothingshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clothingshop.TestDatabase;
import com.clothingshop.dto.CartView;
import com.clothingshop.entity.Discount;
import com.clothingshop.entity.Product;
import com.clothingshop.entity.ProductCategory;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.util.JpaUtil;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** FR-09 shopping cart, with products in an in-memory database. */
class CartServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 10, 8, 0);
    private static final int USER = 7;

    private CartService cart;
    private String tag;

    @BeforeAll
    static void database() {
        TestDatabase.start();
    }

    @BeforeEach
    void setUp() {
        MutableClock clock = new MutableClock(NOW.atZone(ZONE).toInstant(), ZONE);
        cart = new CartService(new InMemoryCartStore(clock), clock);
        tag = UUID.randomUUID().toString().substring(0, 8);
    }

    private Product row(String size, int stock, boolean active) {
        return JpaUtil.inTransaction(em -> {
            Product p = new Product("Áo giỏ hàng " + tag, ProductCategory.SHIRT, null, "assets/images/cloth_1.jpg",
                    new BigDecimal("150000.00"), "Trắng", size, stock, active, NOW);
            em.persist(p);
            return p;
        });
    }

    private void change(Product row, Consumer<Product> change) {
        JpaUtil.inTransaction(em -> {
            change.accept(em.find(Product.class, row.getId()));
            return null;
        });
    }

    private String rejected(Runnable action) {
        return assertThrows(BusinessException.class, action::run).getMessage();
    }

    @Test
    void addingTheSameRowAddsQuantitiesUpToTheStock() {
        Product p = row("M", 5, true);
        assertEquals(2, cart.addItem(USER, p.getId(), 2));
        assertEquals(5, cart.addItem(USER, p.getId(), 3));
        assertTrue(rejected(() -> cart.addItem(USER, p.getId(), 1)).contains("chỉ còn 5 sản phẩm, giỏ hàng đã có 5"));
        assertEquals(5, cart.view(USER).lines().get(0).quantity());
    }

    @Test
    void onlyRowsOnSaleWithStockCanBeAdded() {
        Product hidden = row("S", 5, false);
        Product empty = row("M", 0, true);
        Product few = row("L", 2, true);
        assertTrue(rejected(() -> cart.addItem(USER, hidden.getId(), 1)).contains("ngừng bán"));
        assertTrue(rejected(() -> cart.addItem(USER, empty.getId(), 1)).contains("hết hàng"));
        assertTrue(rejected(() -> cart.addItem(USER, few.getId(), 3)).contains("chỉ còn 2 sản phẩm"));
        assertEquals(CartService.MSG_NOT_FOUND, rejected(() -> cart.addItem(USER, -1, 1)));
        assertEquals(CartService.MSG_QUANTITY, rejected(() -> cart.addItem(USER, few.getId(), 0)));
        assertTrue(cart.view(USER).lines().isEmpty());
    }

    @Test
    void quantitiesCanBeChangedWithinTheStockAndLinesRemoved() {
        Product p = row("M", 4, true);
        cart.addItem(USER, p.getId(), 1);
        cart.updateQuantity(USER, p.getId(), 4);
        assertEquals(4, cart.view(USER).lines().get(0).quantity());
        assertTrue(rejected(() -> cart.updateQuantity(USER, p.getId(), 5)).contains("chỉ còn 4 sản phẩm"));
        assertEquals(CartService.MSG_QUANTITY, rejected(() -> cart.updateQuantity(USER, p.getId(), 0)));
        cart.removeItem(USER, p.getId());
        assertTrue(cart.view(USER).lines().isEmpty());
        assertTrue(rejected(() -> cart.updateQuantity(USER, p.getId(), 1)).contains("không còn trong giỏ hàng"));
    }

    @Test
    void cartShowsCurrentPricesAndTotalsOnlyOrderableLines() {
        Product discounted = row("S", 10, true);
        Product plain = row("M", 10, true);
        JpaUtil.inTransaction(em -> {
            em.persist(new Discount(em.getReference(Product.class, discounted.getId()), new BigDecimal("15"),
                    NOW.minusDays(1), NOW.plusDays(1)));
            return null;
        });
        cart.addItem(USER, discounted.getId(), 2);
        cart.addItem(USER, plain.getId(), 1);

        CartView view = cart.view(USER);
        CartView.Line first = view.lines().get(0);
        assertEquals(new BigDecimal("128000"), first.unitPrice(), "150.000 − 15%, rounded (BR-21)");
        assertEquals(new BigDecimal("256000"), first.subtotal());
        assertTrue(first.onSale());
        assertEquals(new BigDecimal("406000"), view.total());
        assertFalse(view.hasProblems());

        change(plain, p -> p.setActive(false));
        view = cart.view(USER);
        assertEquals(2, view.lines().size(), "a row no longer on sale stays in the cart");
        assertEquals(CartView.Status.NOT_ON_SALE, view.lines().get(1).status());
        assertEquals(new BigDecimal("256000"), view.total(), "but it is not counted");
        assertTrue(view.hasProblems());
        assertTrue(rejected(() -> cart.updateQuantity(USER, plain.getId(), 1)).contains("chỉ có thể xóa"));
    }

    @Test
    void linesAboveTheStockLeftOrOutOfStockAreFlagged() {
        Product p = row("M", 5, true);
        Product q = row("L", 5, true);
        cart.addItem(USER, p.getId(), 4);
        cart.addItem(USER, q.getId(), 1);
        change(p, x -> x.setStockQuantity(3));
        change(q, x -> x.setStockQuantity(0));

        CartView view = cart.view(USER);
        assertEquals(CartView.Status.NOT_ENOUGH_STOCK, view.lines().get(0).status());
        assertEquals(CartView.Status.OUT_OF_STOCK, view.lines().get(1).status());
        assertEquals(0, view.orderableCount());
        assertEquals(BigDecimal.ZERO, view.total());
        cart.updateQuantity(USER, p.getId(), 3);
        assertEquals(CartView.Status.OK, cart.view(USER).lines().get(0).status());
    }

    @Test
    void rowsDeletedFromTheCatalogAreDroppedFromTheCart() {
        Product kept = row("S", 5, true);
        Product deleted = row("M", 5, true);
        cart.addItem(USER, kept.getId(), 1);
        cart.addItem(USER, deleted.getId(), 1);
        JpaUtil.inTransaction(em -> {
            em.remove(em.find(Product.class, deleted.getId()));
            return null;
        });
        assertEquals(1, cart.view(USER).lines().size());
        assertEquals(kept.getId(), cart.view(USER).lines().get(0).productId());
    }
}
