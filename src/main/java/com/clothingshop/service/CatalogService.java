package com.clothingshop.service;

import com.clothingshop.dao.DiscountDao;
import com.clothingshop.dao.ProductDao;
import com.clothingshop.dao.ReviewDao;
import com.clothingshop.dto.ProductDetail;
import com.clothingshop.dto.ProductFilter;
import com.clothingshop.dto.ProductPage;
import com.clothingshop.dto.ProductSummary;
import com.clothingshop.entity.Discount;
import com.clothingshop.entity.Product;
import com.clothingshop.entity.Review;
import com.clothingshop.util.JpaUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Browsing and searching products (FR-07) and the product page (FR-08); read-only and open to every visitor.
 *
 * <p>A product is the set of rows sharing a name (Q44). Rows on sale are loaded and grouped in memory because the price
 * filter uses the price after discount, which depends on the discounts in effect now. A clothing shop has at most a few
 * thousand rows, so this stays cheap; move grouping and paging into SQL if the catalog becomes much larger.
 */
public class CatalogService {

    public static final int PAGE_SIZE = 12;

    private static final Comparator<ProductSummary> NEWEST_FIRST =
            Comparator.comparing(ProductSummary::newestCreatedAt).reversed().thenComparing(ProductSummary::name);

    private final Clock clock;

    public CatalogService(Clock clock) {
        this.clock = clock;
    }

    /** FR-07: products with a row on sale that match {@code filter}, newest first. */
    public ProductPage search(ProductFilter filter) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<ProductSummary> matches = JpaUtil.read(em -> {
            List<Product> rows = new ProductDao(em).findActive(filter.category(), filter.query());
            Map<Integer, Discount> discounts = new DiscountDao(em).findAvailable(ids(rows), now);
            List<ProductSummary> result = new ArrayList<>();
            for (List<Product> product : groupByName(rows)) {
                if (filter.color() != null && product.stream().noneMatch(p -> p.getColor().equalsIgnoreCase(filter.color()))) {
                    continue;
                }
                ProductSummary summary = summarize(product, discounts, filter.color());
                if (filter.minPrice() != null && summary.currentPrice().compareTo(filter.minPrice()) < 0
                        || filter.maxPrice() != null && summary.currentPrice().compareTo(filter.maxPrice()) > 0) {
                    continue;
                }
                result.add(summary);
            }
            return result;
        });
        matches.sort(NEWEST_FIRST);
        return ProductPage.of(matches, filter.page(), PAGE_SIZE);
    }

    /** Colors offered by the rows on sale, for the color filter. */
    public List<String> colors() {
        return JpaUtil.read(em -> new ProductDao(em).findActiveColors());
    }

    /**
     * FR-08: the product that row {@code productId} belongs to, or empty when the row does not exist or the product has
     * no row on sale (hidden products stay in the database but are not shown, BR-16).
     */
    public Optional<ProductDetail> findDetail(int productId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return JpaUtil.read(em -> {
            ProductDao products = new ProductDao(em);
            Optional<Product> requested = products.findById(productId);
            if (requested.isEmpty()) {
                return Optional.empty();
            }
            String name = requested.get().getName();
            List<Product> rows = products.findByName(name).stream().filter(Product::isActive).toList();
            if (rows.isEmpty()) {
                return Optional.empty();
            }
            Map<Integer, Discount> discounts = new DiscountDao(em).findAvailable(ids(rows), now);
            List<Review> reviews = new ReviewDao(em).findByProductName(name);
            return Optional.of(detail(rows, discounts, reviews, productId));
        });
    }

    /** One tile for the rows of a product; also used for the wishlist (FR-10). */
    static ProductSummary summarize(List<Product> rows, Map<Integer, Discount> discounts, String preferredColor) {
        Product cheapest = cheapest(rows, discounts);
        Product shown = pickRow(rows, preferredColor);
        boolean soldOut = rows.stream().allMatch(p -> p.getStockQuantity() == 0);
        LocalDateTime newest = rows.stream().map(Product::getCreatedAt).max(Comparator.naturalOrder()).orElseThrow();
        BigDecimal basePrice = plain(cheapest.getBasePrice());
        BigDecimal currentPrice = cheapest.getCurrentPrice(discounts.get(cheapest.getId()));
        return new ProductSummary(shown.getId(), shown.getName(), shown.getCategory(), shown.getImgUrl(), basePrice,
                currentPrice, currentPrice.compareTo(basePrice) < 0, percentOf(cheapest, discounts), soldOut, newest);
    }

    private static ProductDetail detail(List<Product> rows, Map<Integer, Discount> discounts, List<Review> reviews,
                                        int requestedId) {
        Product first = rows.get(0);
        Product cheapest = cheapest(rows, discounts);
        List<ProductDetail.Variant> variants = new ArrayList<>();
        LinkedHashSet<String> colors = new LinkedHashSet<>();
        LinkedHashSet<String> sizes = new LinkedHashSet<>();
        for (Product p : rows) {
            variants.add(new ProductDetail.Variant(p.getId(), p.getColor(), p.getSize(), p.getImgUrl(), p.getStockQuantity()));
            colors.add(p.getColor());
            sizes.add(p.getSize());
        }
        int selectedId = rows.stream().anyMatch(p -> p.getId() == requestedId) ? requestedId : pickRow(rows, null).getId();
        ProductDetail.Variant selected = variants.stream().filter(v -> v.id() == selectedId).findFirst().orElseThrow();
        List<ProductDetail.ReviewItem> reviewItems = reviews.stream()
                .map(r -> new ProductDetail.ReviewItem(r.getId(), r.getCustomer().getId(), r.getCustomer().getFullName(),
                        r.getRating(), r.getComment(), r.getCreatedAt()))
                .toList();
        BigDecimal average = reviews.isEmpty() ? null
                : BigDecimal.valueOf(reviews.stream().mapToInt(Review::getRating).sum())
                        .divide(BigDecimal.valueOf(reviews.size()), 1, RoundingMode.HALF_UP);
        BigDecimal basePrice = plain(cheapest.getBasePrice());
        BigDecimal currentPrice = cheapest.getCurrentPrice(discounts.get(cheapest.getId()));
        return new ProductDetail(first.getName(), first.getCategory(), first.getDescription(), basePrice, currentPrice,
                currentPrice.compareTo(basePrice) < 0, percentOf(cheapest, discounts), List.copyOf(variants),
                List.copyOf(colors), List.copyOf(sizes), selected, average, reviewItems);
    }

    /** Groups rows into products by name (letter case ignored, section 3.1), keeping the order of the rows. */
    static Collection<List<Product>> groupByName(List<Product> rows) {
        Map<String, List<Product>> groups = new LinkedHashMap<>();
        for (Product p : rows) {
            groups.computeIfAbsent(p.getName().toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(p);
        }
        return groups.values();
    }

    /**
     * Rows of a product share the base price and the discounts (FR-31, FR-34); the lowest price is used anyway so that a
     * product is never shown dearer than one of its rows.
     */
    private static Product cheapest(List<Product> rows, Map<Integer, Discount> discounts) {
        return rows.stream().min(Comparator.comparing((Product p) -> p.getCurrentPrice(discounts.get(p.getId())))).orElseThrow();
    }

    /** The row whose image represents the product: in stock and of the preferred color when possible. */
    private static Product pickRow(List<Product> rows, String preferredColor) {
        List<Product> candidates = preferredColor == null ? rows
                : rows.stream().filter(p -> p.getColor().equalsIgnoreCase(preferredColor)).toList();
        if (candidates.isEmpty()) {
            candidates = rows;
        }
        return candidates.stream().filter(p -> p.getStockQuantity() > 0).findFirst().orElse(candidates.get(0));
    }

    private static BigDecimal percentOf(Product row, Map<Integer, Discount> discounts) {
        Discount discount = discounts.get(row.getId());
        return discount == null ? null : discount.getDiscountValue();
    }

    /** DECIMAL(12,2) values such as 150000.00 are shown and compared as whole VND amounts. */
    private static BigDecimal plain(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP);
    }

    static List<Integer> ids(List<Product> rows) {
        return rows.stream().map(Product::getId).toList();
    }
}
