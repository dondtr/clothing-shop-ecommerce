package com.clothingshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clothingshop.TestDatabase;
import com.clothingshop.dto.ProductDetail;
import com.clothingshop.dto.ProductFilter;
import com.clothingshop.dto.ProductPage;
import com.clothingshop.dto.ProductSummary;
import com.clothingshop.entity.Customer;
import com.clothingshop.entity.Discount;
import com.clothingshop.entity.Gender;
import com.clothingshop.entity.Product;
import com.clothingshop.entity.ProductCategory;
import com.clothingshop.entity.Review;
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

/** FR-07 (browse and search) and FR-08 (product page), against an in-memory database. */
class CatalogServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 10, 8, 0);

    private CatalogService catalog;
    /** Unique per test; every product name contains it, and searching for it isolates the test's products. */
    private String tag;

    @BeforeAll
    static void database() {
        TestDatabase.start();
    }

    @BeforeEach
    void setUp() {
        catalog = new CatalogService(new MutableClock(NOW.atZone(ZONE).toInstant(), ZONE));
        tag = UUID.randomUUID().toString().substring(0, 8);
    }

    private Product row(String product, ProductCategory category, String color, String size, int stock, boolean active,
                        String basePrice, int daysOld) {
        return JpaUtil.inTransaction(em -> {
            Product p = new Product(product + " " + tag, category, "Mô tả " + product, "assets/images/" + color + ".jpg",
                    new BigDecimal(basePrice), color, size, stock, active, NOW.minusDays(daysOld));
            em.persist(p);
            return p;
        });
    }

    private Product row(String product, String color, String size, int stock) {
        return row(product, ProductCategory.SHIRT, color, size, stock, true, "150000.00", 1);
    }

    private void discount(Product row, String percent, LocalDateTime start, LocalDateTime end) {
        JpaUtil.inTransaction(em -> {
            em.persist(new Discount(em.getReference(Product.class, row.getId()), new BigDecimal(percent), start, end));
            return null;
        });
    }

    private ProductPage search(ProductCategory category, String color, String min, String max, int page) {
        return catalog.search(new ProductFilter(category, tag, color, min == null ? null : new BigDecimal(min),
                max == null ? null : new BigDecimal(max), page));
    }

    private ProductPage search() {
        return search(null, null, null, null, 1);
    }

    private static ProductSummary only(ProductPage page) {
        assertEquals(1, page.items().size(), () -> "expected one product, got " + page.items());
        return page.items().get(0);
    }

    @Test
    void rowsWithTheSameNameFormOneProductAndHiddenProductsAreNotListed() {
        row("Áo thun", "Trắng", "S", 5);
        row("Áo thun", "Trắng", "M", 5);
        row("Áo thun", "Đen", "M", 5);
        row("Mũ", ProductCategory.HAT, "Đen", "Free", 5, false, "90000", 1);

        ProductPage page = search();
        assertEquals(1, page.totalItems());
        assertEquals("Áo thun " + tag, only(page).name());
    }

    @Test
    void soldOutOnlyWhenEveryRowIsOutOfStockAndSoldOutProductsStayVisible() {
        row("Còn một dòng", "Trắng", "S", 0);
        Product inStock = row("Còn một dòng", "Trắng", "M", 3);
        row("Hết sạch", "Đỏ", "S", 0);
        row("Hết sạch", "Đỏ", "M", 0);

        List<ProductSummary> items = search().items();
        assertEquals(2, items.size());
        ProductSummary partly = items.stream().filter(s -> s.name().startsWith("Còn")).findFirst().orElseThrow();
        ProductSummary empty = items.stream().filter(s -> s.name().startsWith("Hết")).findFirst().orElseThrow();
        assertFalse(partly.soldOut());
        assertEquals(inStock.getId(), partly.id(), "the tile shows a row that is in stock");
        assertTrue(empty.soldOut());
    }

    @Test
    void pricesUseOnlyDiscountsInEffectAndAreRoundedToThousands() {
        Product current = row("Đang giảm", "Trắng", "S", 5);
        Product startsNow = row("Bắt đầu đúng lúc này", "Trắng", "S", 5);
        Product future = row("Sắp giảm", "Trắng", "S", 5);
        Product ended = row("Hết giảm", "Trắng", "S", 5);
        discount(current, "15", NOW.minusDays(1), NOW.plusDays(1));
        discount(startsNow, "20", NOW, NOW.plusDays(1));
        discount(future, "15", NOW.plusSeconds(1), NOW.plusDays(5));
        discount(ended, "15", NOW.minusDays(5), NOW.minusSeconds(1));

        List<ProductSummary> items = search().items();
        ProductSummary onSale = items.stream().filter(s -> s.name().startsWith("Đang giảm")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("150000"), onSale.basePrice());
        assertEquals(new BigDecimal("128000"), onSale.currentPrice(), "150.000 − 15% = 127.500 → 128.000 (BR-21)");
        assertEquals(0, new BigDecimal("15").compareTo(onSale.discountPercent()));
        assertTrue(onSale.onSale());
        ProductSummary atStart = items.stream().filter(s -> s.name().startsWith("Bắt đầu")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("120000"), atStart.currentPrice(), "startDate ≤ now is in effect");
        for (String name : List.of("Sắp giảm", "Hết giảm")) {
            ProductSummary s = items.stream().filter(i -> i.name().startsWith(name)).findFirst().orElseThrow();
            assertEquals(new BigDecimal("150000"), s.currentPrice(), name);
            assertFalse(s.onSale(), name);
            assertNull(s.discountPercent(), name);
        }
    }

    @Test
    void priceRangeUsesThePriceAfterDiscountInclusive() {
        Product cheap = row("Giảm còn 128 nghìn", "Trắng", "S", 5);
        discount(cheap, "15", NOW.minusDays(1), NOW.plusDays(1));
        row("Giá gốc 150 nghìn", "Trắng", "S", 5);

        assertTrue(only(search(null, null, null, "128000", 1)).name().startsWith("Giảm"));
        assertTrue(only(search(null, null, "128001", null, 1)).name().startsWith("Giá gốc"));
        assertEquals(2, search(null, null, "128000", "150000", 1).totalItems());
        assertEquals(0, search(null, null, "150001", null, 1).totalItems());
    }

    @Test
    void filtersByCategoryNameAndColor() {
        row("Giày vải", ProductCategory.SHOES, "Xanh", "40", 5, true, "450000", 1);
        row("Áo sơ mi", "Trắng", "M", 5);
        Product red = row("Áo sơ mi", "Đỏ", "M", 5);

        assertTrue(only(search(ProductCategory.SHOES, null, null, null, 1)).name().startsWith("Giày"));
        ProductSummary byColor = only(search(null, "đỏ", null, null, 1));
        assertEquals(red.getId(), byColor.id(), "the tile shows a row of the chosen color");
        assertEquals(0, search(null, "Tím", null, null, 1).totalItems());

        ProductPage byName = catalog.search(new ProductFilter(null, "GIÀY VẢI " + tag.toUpperCase(), null, null, null, 1));
        assertTrue(only(byName).name().startsWith("Giày"), "name search ignores letter case");
        assertEquals(0, catalog.search(new ProductFilter(null, "%" + tag, null, null, null, 1)).totalItems(),
                "% typed by the user is not a wildcard");
    }

    @Test
    void newestProductsComeFirstAndPagesHoldTwelve() {
        for (int i = 0; i < CatalogService.PAGE_SIZE + 1; i++) {
            row(String.format("Mặt hàng %02d", i), ProductCategory.SHIRT, "Trắng", "M", 1, true, "100000", i);
        }
        ProductPage first = search();
        assertEquals(13, first.totalItems());
        assertEquals(2, first.totalPages());
        assertEquals(CatalogService.PAGE_SIZE, first.items().size());
        assertTrue(first.items().get(0).name().startsWith("Mặt hàng 00"), "created most recently");
        ProductPage last = search(null, null, null, null, 99);
        assertEquals(2, last.page(), "a page past the end shows the last page");
        assertTrue(only(last).name().startsWith("Mặt hàng 12"));
    }

    @Test
    void productPageShowsEveryRowOnSaleWithReviewsOfAllRows() {
        Product whiteS = row("Áo polo", "Trắng", "S", 4);
        Product whiteM = row("Áo polo", "Trắng", "M", 0);
        Product blackS = row("Áo polo", "Đen", "S", 2);
        discount(whiteS, "20", NOW.minusDays(1), NOW.plusDays(1));
        discount(whiteM, "20", NOW.minusDays(1), NOW.plusDays(1));
        discount(blackS, "20", NOW.minusDays(1), NOW.plusDays(1));
        review(whiteS, 5, "Đẹp");
        review(blackS, 4, null);

        ProductDetail detail = catalog.findDetail(blackS.getId()).orElseThrow();
        assertEquals("Áo polo " + tag, detail.name());
        assertEquals(List.of("Trắng", "Đen"), detail.colors());
        assertEquals(List.of("S", "M"), detail.sizes());
        assertEquals(3, detail.variants().size());
        assertEquals(blackS.getId(), detail.selectedVariant().id(), "the row in the link is selected");
        assertEquals(0, detail.variants().stream().filter(v -> v.id() == whiteM.getId()).findFirst().orElseThrow().stockQuantity());
        assertEquals(new BigDecimal("120000"), detail.currentPrice());
        assertEquals(new BigDecimal("150000"), detail.basePrice());
        assertTrue(detail.onSale());
        assertEquals(new BigDecimal("4.5"), detail.averageRating());
        assertEquals(2, detail.reviews().size());
        assertTrue(detail.reviews().stream().allMatch(r -> r.authorName().startsWith("Khách")));
    }

    @Test
    void productPageIsNotFoundForHiddenProductsAndUnknownRows() {
        Product hidden = row("Ngừng bán", ProductCategory.HAT, "Đen", "Free", 5, false, "90000", 1);
        assertTrue(catalog.findDetail(hidden.getId()).isEmpty());
        assertTrue(catalog.findDetail(-1).isEmpty());

        Product noReviews = row("Chưa đánh giá", "Trắng", "S", 0);
        ProductDetail detail = catalog.findDetail(noReviews.getId()).orElseThrow();
        assertNull(detail.averageRating());
        assertTrue(detail.reviews().isEmpty());
    }

    private void review(Product row, int rating, String comment) {
        String user = "rv" + UUID.randomUUID().toString().substring(0, 8);
        JpaUtil.inTransaction(em -> {
            Customer customer = new Customer(user, "x", "Khách " + user, Gender.OTHER, LocalDate.of(2000, 1, 1),
                    user + "@example.com", "0900000000", NOW);
            em.persist(customer);
            em.persist(new Review(customer, em.getReference(Product.class, row.getId()), rating, comment, NOW));
            return null;
        });
    }
}
