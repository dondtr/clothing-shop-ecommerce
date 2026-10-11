package com.clothingshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clothingshop.TestDatabase;
import com.clothingshop.dao.ProductDao;
import com.clothingshop.dto.ProductAdminView;
import com.clothingshop.dto.ProductForm;
import com.clothingshop.entity.Discount;
import com.clothingshop.entity.OrderStatus;
import com.clothingshop.entity.PaymentMethod;
import com.clothingshop.entity.PaymentStatus;
import com.clothingshop.entity.Product;
import com.clothingshop.entity.Wishlist;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ConflictException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.util.JpaUtil;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** FR-31 – FR-34: products, variants, sale status, stock, discount campaigns, and J4 (FR-11). */
class ProductAdminServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private MutableClock clock;
    private InMemoryCartStore carts;
    private ProductAdminService products;
    private DiscountAdminService discounts;
    private String name;

    @BeforeAll
    static void database() {
        TestDatabase.start();
    }

    @BeforeEach
    void setUp() {
        clock = new MutableClock(TestData.NOW.atZone(ZONE).toInstant(), ZONE);
        carts = new InMemoryCartStore(clock);
        products = new ProductAdminService(carts, clock);
        discounts = new DiscountAdminService(clock);
        name = TestData.unique("Áo quản lý ");
    }

    private static ProductForm.Variant v(String color, String size, String stock) {
        return new ProductForm.Variant(color, size, stock, "assets/images/cloth_1.jpg");
    }

    private int create(ProductForm.Variant... variants) {
        return products.create(new ProductForm(name, "SHIRT", "Mô tả", "250000", true, List.of(variants)));
    }

    private List<Product> rows(String productName) {
        return JpaUtil.read(em -> new ProductDao(em).findByName(productName));
    }

    @Test
    void createsOneRowPerVariantWithTheCommonInformation() {
        int id = create(v("Trắng", "S", "5"), v("Trắng", "M", "3"), v("Đen", "M", "0"));
        List<Product> rows = rows(name);
        assertEquals(3, rows.size());
        assertEquals(id, rows.get(0).getId());
        assertTrue(rows.stream().allMatch(p -> p.getBasePrice().compareTo(new BigDecimal("250000")) == 0 && p.isActive()
                && p.getCreatedAt().equals(TestData.NOW) && "Mô tả".equals(p.getDescription())));
        ProductAdminView view = products.get(rows.get(2).getId());
        assertEquals(name, view.name());
        assertTrue(view.canDelete());
    }

    @Test
    void createChecksNamePriceImagesAndDuplicateVariants() {
        create(v("Trắng", "S", "1"));
        ValidationException e = assertThrows(ValidationException.class, () -> products.create(new ProductForm(name.toUpperCase(),
                "SHIRT", null, "250500", true, List.of(v("Đỏ", "S", "1"), v("đỏ", "s", "2"),
                        new ProductForm.Variant("Xanh", "L", "-1", "")))));
        Map<String, String> errors = e.getErrors();
        assertTrue(errors.get("basePrice").contains("1.000"));
        assertTrue(errors.containsKey("variants[1].size"), "same color + size");
        assertTrue(errors.containsKey("variants[2].imgUrl"));
        assertTrue(errors.containsKey("variants[2].stockQuantity"));
        ValidationException taken = assertThrows(ValidationException.class, () -> products.create(new ProductForm(name.toUpperCase(),
                "SHIRT", null, "100000", true, List.of(v("Đỏ", "S", "1")))));
        assertTrue(taken.getErrors().get("name").contains("BR-28"));
    }

    @Test
    void commonInformationAndSaleStatusChangeOnEveryRowAndOldPagesAreRefused() {
        int id = create(v("Trắng", "S", "1"), v("Đen", "S", "1"));
        String stamp = products.get(id).stamp();
        String renamed = name + " mới";
        products.updateCommon(id, new ProductForm(renamed, "PANTS", "Mới", "300000", true, null), stamp);
        assertTrue(rows(name).isEmpty());
        assertTrue(rows(renamed).stream().allMatch(p -> p.getCategory().name().equals("PANTS")
                && p.getBasePrice().compareTo(new BigDecimal("300000")) == 0));
        assertThrows(ConflictException.class, () -> products.setActive(id, false, stamp), "page opened before the save");
        products.setActive(id, false, products.get(id).stamp());
        assertTrue(rows(renamed).stream().noneMatch(Product::isActive));
    }

    @Test
    void aNewVariantCopiesOpenDiscountCampaigns() {
        int id = create(v("Trắng", "S", "1"));
        discounts.create(id, "10", TestData.NOW.minusDays(10).toString(), TestData.NOW.minusDays(5).toString());
        discounts.create(id, "20", TestData.NOW.minusDays(1).toString(), TestData.NOW.plusDays(3).toString());
        discounts.create(id, "15", TestData.NOW.plusDays(5).toString(), TestData.NOW.plusDays(9).toString());
        int added = products.addVariant(id, v("Trắng", "XL", "4"), products.get(id).stamp());
        List<BigDecimal> copied = JpaUtil.read(em -> em.createQuery("select d.discountValue from Discount d where d.product.id = :p"
                + " order by d.startDate", BigDecimal.class).setParameter("p", added).getResultList());
        assertEquals(List.of(new BigDecimal("20.00"), new BigDecimal("15.00")), copied, "ended campaigns are not copied");
        assertThrows(ValidationException.class, () -> products.addVariant(id, v("trắng", "xl", "1"), products.get(id).stamp()));
    }

    @Test
    void soldRowsKeepColorAndSizeAndCannotBeDeleted() {
        int id = create(v("Trắng", "S", "5"), v("Đen", "S", "5"));
        List<Product> rows = rows(name);
        int customerId = TestData.customer("matkhau-123").getId();
        sell(customerId, rows.get(0));
        ProductAdminView view = products.get(id);
        ProductAdminView.Row sold = view.rows().get(0);
        assertTrue(sold.sold());
        assertThrows(BusinessException.class, () -> products.updateVariant(sold.id(), v("Xám", "S", null), sold.version()));
        products.updateVariant(sold.id(), new ProductForm.Variant("Trắng", "S", null, "assets/images/cloth_2.jpg"), sold.version());
        assertEquals("assets/images/cloth_2.jpg", rows(name).get(0).getImgUrl(), "the image can always change");
        assertThrows(BusinessException.class, () -> products.deleteVariant(sold.id()));
        assertThrows(BusinessException.class, () -> products.deleteProduct(id));
        assertFalse(products.get(id).canDelete());
    }

    @Test
    void stockIsAddedAtomicallyOrSetFromTheCurrentPageAndNeverNegative() {
        int id = create(v("Trắng", "S", "5"));
        ProductAdminView.Row row = products.get(id).rows().get(0);
        products.updateStock(id, "3", true, null);
        products.updateStock(id, "-2", true, null);
        assertEquals(6, rows(name).get(0).getStockQuantity());
        assertThrows(BusinessException.class, () -> products.updateStock(id, "-7", true, null));
        assertThrows(ConflictException.class, () -> products.updateStock(id, "10", false, row.version()), "the row changed meanwhile");
        products.updateStock(id, "10", false, products.get(id).rows().get(0).version());
        assertEquals(10, rows(name).get(0).getStockQuantity());
        assertThrows(ValidationException.class, () -> products.updateStock(id, "-1", false, null));
        assertThrows(ValidationException.class, () -> products.updateStock(id, "abc", true, null));
    }

    @Test
    void deletingARowMovesWishlistsRemovesItFromCartsAndTheLastRowRemovesTheProduct() {
        int id = create(v("Trắng", "S", "5"), v("Đen", "S", "5"));
        List<Product> rows = rows(name);
        int customerId = TestData.customer("matkhau-123").getId();
        new WishlistService(clock).add(customerId, rows.get(0).getId());
        discounts.create(id, "10", TestData.NOW.plusDays(1).toString(), TestData.NOW.plusDays(2).toString());
        CartService cart = new CartService(carts, clock);
        cart.addItem(customerId, rows.get(0).getId(), 1);

        products.deleteVariant(rows.get(0).getId());
        assertEquals(1, rows(name).size());
        assertTrue(cart.lines(customerId).isEmpty(), "removed from every cart");
        List<Integer> wished = JpaUtil.read(em -> em.createQuery("select w from Wishlist w left join fetch w.products where w.customer.id = :c",
                Wishlist.class).setParameter("c", customerId).getSingleResult().getProducts().stream().map(Product::getId).toList());
        assertEquals(List.of(rows.get(1).getId()), wished, "moved to the remaining row");

        products.deleteVariant(rows.get(1).getId());
        assertTrue(rows(name).isEmpty());
        assertEquals(0L, (long) JpaUtil.<Long>read(em -> em.createQuery("select count(w) from Wishlist w join w.products p"
                + " where w.customer.id = :c", Long.class).setParameter("c", customerId).getSingleResult()));
    }

    @Test
    void campaignsDoNotOverlapAndCanBeEditedOnlyBeforeTheyStart() {
        int id = create(v("Trắng", "S", "5"), v("Đen", "S", "5"));
        assertThrows(ValidationException.class, () -> discounts.validate(id, "0", "2026-10-11T00:00", "2026-10-12T00:00", null));
        assertThrows(ValidationException.class, () -> discounts.validate(id, "101", "2026-10-11T00:00", "2026-10-12T00:00", null));
        assertThrows(ValidationException.class, () -> discounts.validate(id, "10", "2026-10-12T00:00", "2026-10-11T00:00", null));
        discounts.create(id, "10", "2026-10-11T00:00", "2026-10-15T00:00");
        assertEquals(2L, (long) JpaUtil.<Long>read(em -> em.createQuery("select count(d) from Discount d where d.product.name = :n",
                Long.class).setParameter("n", name).getSingleResult()), "one Discount per row");
        ValidationException overlap = assertThrows(ValidationException.class,
                () -> discounts.create(id, "20", "2026-10-15T00:00", "2026-10-20T00:00"), "the end moment is shared");
        assertTrue(overlap.getErrors().get("startDate").contains("Trùng"));

        java.time.LocalDateTime start = java.time.LocalDateTime.parse("2026-10-11T00:00");
        discounts.update(id, start, "12.5", "2026-10-12T00:00", "2026-10-16T00:00");   // excludes itself from the overlap check
        assertEquals(List.of(new BigDecimal("12.50")), JpaUtil.read(em -> em.createQuery("select distinct d.discountValue from Discount d"
                + " where d.product.name = :n", BigDecimal.class).setParameter("n", name).getResultList()));
        clock.advance(Duration.ofDays(3));   // 13/10: the campaign has started
        assertThrows(BusinessException.class, () -> discounts.update(id, java.time.LocalDateTime.parse("2026-10-12T00:00"),
                "15", "2026-10-12T00:00", "2026-10-16T00:00"));
        assertEquals("Đang hiệu lực", products.get(id).campaigns().get(0).state());
        assertFalse(products.get(id).campaigns().get(0).editable());
    }

    @Test
    void j4EmailsWishlistsWhenADiscountStartsToday() {
        int id = create(v("Trắng", "S", "5"), v("Đen", "S", "5"));
        int a = TestData.customer("matkhau-123").getId();
        int b = TestData.customer("matkhau-123").getId();
        List<Product> rows = rows(name);
        new WishlistService(clock).add(a, rows.get(0).getId());
        new WishlistService(clock).add(b, rows.get(1).getId());
        discounts.create(id, "30", "2026-10-11T00:00", "2026-10-13T00:00");
        List<String> sent = new ArrayList<>();
        DiscountNotifier notifier = new DiscountNotifier((to, subject, body) -> {
            if (subject.contains(name)) {
                sent.add(to);
            }
        }, "http://localhost/shop", clock);
        notifier.notifyWishlists();
        assertTrue(sent.isEmpty(), "10/10: the discount starts tomorrow");
        clock.advance(Duration.ofDays(1));
        notifier.notifyWishlists();
        assertEquals(2, sent.size(), "one email per customer for the product");
        sent.clear();
        products.setActive(id, false, products.get(id).stamp());
        notifier.notifyWishlists();
        assertTrue(sent.isEmpty(), "products not on sale are skipped");
    }

    /** Puts the row in a completed order of the customer. */
    private static void sell(int customerId, Product row) {
        int orderId = TestData.order(customerId, "270000", OrderStatus.COMPLETED, PaymentMethod.COD, PaymentStatus.PAID);
        JpaUtil.inTransaction(em -> {
            com.clothingshop.entity.Order o = em.find(com.clothingshop.entity.Order.class, orderId);
            o.addDetail(new com.clothingshop.entity.OrderDetail(em.find(Product.class, row.getId()), 1, new BigDecimal("250000")));
            return null;
        });
    }
}
