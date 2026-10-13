package com.clothingshop.service;

import com.clothingshop.dao.DiscountDao;
import com.clothingshop.dao.ProductDao;
import com.clothingshop.dao.WishlistDao;
import com.clothingshop.dto.ProductAdminView;
import com.clothingshop.dto.ProductForm;
import com.clothingshop.entity.Discount;
import com.clothingshop.entity.Product;
import com.clothingshop.entity.ProductCategory;
import com.clothingshop.entity.Wishlist;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ConflictException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.util.JpaUtil;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Product management (FR-31 – FR-33): products and their variant rows, sale status and stock. A product is the set of
 * rows sharing a name (Q44); common information is kept identical on every row. Rows already ordered keep their color
 * and size and cannot be deleted (BR-15). Deleting is for the manager only (Q36; checked by the pages).
 */
public class ProductAdminService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductAdminService.class);

    static final int DESCRIPTION_MAX = 5000;
    private static final BigDecimal PRICE_MAX = new BigDecimal("9999999999");

    private final CartStore carts;
    private final Clock clock;

    public ProductAdminService(CartStore carts, Clock clock) {
        this.carts = carts;
        this.clock = clock;
    }

    /** Products for the staff list, including those not on sale, newest first; filtered by name when {@code query} is set. */
    public List<ProductAdminView.Summary> list(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return JpaUtil.read(em -> {
            List<Product> rows = new ProductDao(em).findAllNewestFirst();
            Map<String, List<Product>> groups = new LinkedHashMap<>();
            for (Product p : rows) {
                if (p.getName().toLowerCase(Locale.ROOT).contains(q)) {
                    groups.computeIfAbsent(p.getName().toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(p);
                }
            }
            return groups.values().stream().map(g -> {
                Product first = g.stream().min(Comparator.comparing(Product::getId)).orElseThrow();
                return new ProductAdminView.Summary(first.getId(), first.getName(), first.getCategory(), first.getBasePrice(),
                        first.isActive(), g.size(), g.stream().mapToInt(Product::getStockQuantity).sum(), first.getImgUrl());
            }).toList();
        });
    }

    /** The product that row {@code rowId} belongs to, with its rows and discount campaigns. */
    public ProductAdminView get(int rowId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return JpaUtil.read(em -> {
            List<Product> rows = rowsOf(em, rowId);
            Product first = rows.get(0);
            Set<Integer> sold = soldRows(em, rows);
            List<ProductAdminView.Row> views = rows.stream().map(p -> new ProductAdminView.Row(p.getId(), p.getColor(), p.getSize(),
                    p.getStockQuantity(), p.getImgUrl(), sold.contains(p.getId()), p.getVersion())).toList();
            return new ProductAdminView(first.getId(), first.getName(), first.getCategory(), first.getDescription(),
                    first.getBasePrice(), first.isActive(), first.getCreatedAt(), views, stamp(rows), sold.isEmpty(),
                    DiscountAdminService.campaigns(em, rows, now));
        });
    }

    /**
     * FR-31 step 1 (Product.createProduct()): one row per variant carrying all common information; the name is not used
     * by another product (BR-28), the price is a multiple of 1.000 (BR-21), every variant has an image and no two share
     * color + size.
     *
     * @return the id of the first row
     */
    public int create(ProductForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        Common c = common(form, errors);
        List<ProductForm.Variant> variants = form.variants() == null ? List.of() : form.variants();
        if (variants.isEmpty()) {
            errors.put("variants", "Cần ít nhất một biến thể (màu + kích thước)");
        }
        List<VariantValues> values = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < variants.size(); i++) {
            VariantValues v = variant(variants.get(i), "variants[" + i + "].", errors, true);
            if (v != null && !seen.add(v.key())) {
                errors.put("variants[" + i + "].size", "Trùng màu và kích thước với biến thể khác");
            }
            values.add(v);
        }
        reportTakenName(c.name(), null, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        int id = JpaUtil.inTransaction(em -> {
            requireFreeName(em, c.name(), null);
            Integer first = null;
            for (VariantValues v : values) {
                Product p = new Product(c.name(), c.category(), c.description(), v.imgUrl(), c.basePrice(), v.color(), v.size(),
                        v.stock(), c.active(), now);
                new ProductDao(em).persist(p);
                first = first == null ? p.getId() : first;
            }
            return first;
        });
        LOG.info("Product created: name={}, variants={}", c.name(), values.size());
        return id;
    }

    /**
     * FR-31 step 3 and FR-32 (Product.updateProduct()): saves the common information and the sale status on every row of
     * the product. A new name must not be used by another product (BR-28). Old orders keep their subtotals (Q45).
     */
    public void updateCommon(int rowId, ProductForm form, String stamp) {
        Map<String, String> errors = new LinkedHashMap<>();
        Common c = common(form, errors);
        String current = JpaUtil.read(em -> new ProductDao(em).findById(rowId).map(Product::getName).orElse(null));
        reportTakenName(c.name(), current, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        JpaUtil.inTransaction(em -> {
            List<Product> rows = lockRows(em, rowId, stamp);
            requireFreeName(em, c.name(), rows.get(0).getName());
            for (Product p : rows) {
                p.setName(c.name());
                p.setCategory(c.category());
                p.setDescription(c.description());
                p.setBasePrice(c.basePrice());
                p.setActive(c.active());
            }
            return null;
        });
        LOG.info("Product updated: rowId={}, name={}, active={}", rowId, c.name(), c.active());
    }

    /** FR-32: sale status only, on every row (BR-16: never changed automatically). */
    public void setActive(int rowId, boolean active, String stamp) {
        JpaUtil.inTransaction(em -> {
            lockRows(em, rowId, stamp).forEach(p -> p.setActive(active));
            return null;
        });
        LOG.info("Product {}: rowId={}", active ? "put on sale" : "taken off sale", rowId);
    }

    /**
     * FR-31 step 2 (Product.addVariant()): a new row copying the common information, plus a copy of every discount
     * campaign of the product that has not ended, so all rows keep the same price after discount (FR-34).
     */
    public int addVariant(int rowId, ProductForm.Variant form, String stamp) {
        Map<String, String> errors = new LinkedHashMap<>();
        VariantValues v = variant(form, "", errors, true);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        int id = JpaUtil.inTransaction(em -> {
            List<Product> rows = lockRows(em, rowId, stamp);
            requireFreeVariant(rows, v, null);
            Product model = rows.get(0);
            Product p = new Product(model.getName(), model.getCategory(), model.getDescription(), v.imgUrl(), model.getBasePrice(),
                    v.color(), v.size(), v.stock(), model.isActive(), now);
            new ProductDao(em).persist(p);
            DiscountDao discounts = new DiscountDao(em);
            for (Discount d : DiscountAdminService.openCampaignTemplates(em, rows, now)) {
                discounts.persist(new Discount(p, d.getDiscountValue(), d.getStartDate(), d.getEndDate()));
            }
            return p.getId();
        });
        LOG.info("Variant added: rowId={}, color={}, size={}", id, v.color(), v.size());
        return id;
    }

    /** FR-31 step 4: the image can always change; color and size only while the row was never ordered (BR-15). */
    public void updateVariant(int rowId, ProductForm.Variant form, Long version) {
        Map<String, String> errors = new LinkedHashMap<>();
        VariantValues v = variant(form, "", errors, false);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        JpaUtil.inTransaction(em -> {
            Product row = new ProductDao(em).lock(rowId).orElse(null);
            if (row == null) {
                throw new BusinessException("Biến thể không tồn tại");
            }
            ConflictException.check(row.getVersion(), version);
            boolean changesVariant = !row.getColor().equalsIgnoreCase(v.color()) || !row.getSize().equalsIgnoreCase(v.size());
            if (changesVariant) {
                if (soldRows(em, List.of(row)).contains(rowId)) {
                    throw new BusinessException("Biến thể đã có trong đơn hàng nên không đổi được màu, kích thước (BR-15)");
                }
                requireFreeVariant(new ProductDao(em).findByName(row.getName()), v, rowId);
            }
            row.setColor(v.color());
            row.setSize(v.size());
            row.setImgUrl(v.imgUrl());
            return null;
        });
    }

    /**
     * FR-33 (Product.updateStock()): {@code add} adds a quantity (negative to take some away) atomically; otherwise the
     * stock is set to the quantity, refused when the row changed since the page was opened. Stock never goes below 0.
     */
    public void updateStock(int rowId, String quantityText, boolean add, Long version) {
        int quantity;
        try {
            quantity = Integer.parseInt(quantityText == null ? "" : quantityText.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(Map.of("quantity", "Số lượng phải là số nguyên"));
        }
        if (!add && quantity < 0) {
            throw new ValidationException(Map.of("quantity", "Tồn kho không được âm"));
        }
        JpaUtil.inTransaction(em -> {
            Product row = new ProductDao(em).lock(rowId).orElse(null);
            if (row == null) {
                throw new BusinessException("Biến thể không tồn tại");
            }
            if (add) {
                if (row.getStockQuantity() + quantity < 0) {
                    throw new BusinessException("Tồn kho không được âm (hiện có " + row.getStockQuantity() + ")");
                }
                row.setStockQuantity(row.getStockQuantity() + quantity);
            } else {
                ConflictException.check(row.getVersion(), version);
                row.setStockQuantity(quantity);
            }
            return null;
        });
        LOG.info("Stock {}: rowId={}, quantity={}", add ? "added" : "set", rowId, quantity);
    }

    /**
     * FR-31 step 5 (Product.removeVariant(), manager only): deletes a row never ordered, with its discounts; wishlist
     * entries move to another row of the product or are removed when none is left; the row leaves every cart.
     * Deleting the last row deletes the product.
     */
    public void deleteVariant(int rowId) {
        JpaUtil.inTransaction(em -> {
            Product row = new ProductDao(em).lock(rowId).orElse(null);
            if (row == null) {
                throw new BusinessException("Biến thể không tồn tại");
            }
            List<Product> others = new ProductDao(em).findByName(row.getName()).stream().filter(p -> !p.getId().equals(rowId)).toList();
            delete(em, row, others.isEmpty() ? null : others.get(0));
            return null;
        });
        carts.removeProduct(rowId);
        LOG.info("Variant deleted: rowId={}", rowId);
    }

    /** FR-31 step 5 (Product.deleteProduct(), manager only): every row must never have been ordered. */
    public void deleteProduct(int rowId) {
        List<Integer> ids = JpaUtil.inTransaction(em -> {
            List<Product> rows = new ProductDao(em).lockByName(rowsOf(em, rowId).get(0).getName());
            if (!soldRows(em, rows).isEmpty()) {
                throw new BusinessException("Mặt hàng có biến thể đã nằm trong đơn hàng nên không xóa được (BR-15). "
                        + "Hãy chuyển sang không bán hoặc đặt tồn kho về 0.");
            }
            for (Product p : rows) {
                delete(em, p, null);
            }
            return rows.stream().map(Product::getId).toList();
        });
        ids.forEach(carts::removeProduct);
        LOG.info("Product deleted: rows={}", ids);
    }

    private static void delete(EntityManager em, Product row, Product replacement) {
        if (!soldRows(em, List.of(row)).isEmpty()) {
            throw new BusinessException("Biến thể đã nằm trong đơn hàng nên không xóa được (BR-15). "
                    + "Hãy chuyển mặt hàng sang không bán hoặc đặt tồn kho về 0.");
        }
        for (Wishlist w : new WishlistDao(em).findContaining(row.getId())) {
            w.getProducts().remove(row);
            if (replacement != null && w.getProducts().stream().noneMatch(p -> p.getName().equalsIgnoreCase(replacement.getName()))) {
                w.getProducts().add(replacement);
            }
        }
        new DiscountDao(em).deleteByProduct(row.getId());
        ProductDao products = new ProductDao(em);
        products.flush();
        products.remove(row);
    }

    /** Rows of the product of {@code rowId}, in creation order. */
    private static List<Product> rowsOf(EntityManager em, int rowId) {
        Product row = new ProductDao(em).findById(rowId).orElse(null);
        if (row == null) {
            throw new BusinessException("Mặt hàng không tồn tại");
        }
        return new ProductDao(em).findByName(row.getName());
    }

    /** Locks every row of the product and refuses the change if any row changed since the page was opened. */
    private static List<Product> lockRows(EntityManager em, int rowId, String stamp) {
        Product row = new ProductDao(em).findById(rowId).orElse(null);
        if (row == null) {
            throw new BusinessException("Mặt hàng không tồn tại");
        }
        List<Product> rows = new ProductDao(em).lockByName(row.getName());
        if (!Objects.equals(stamp(rows), stamp)) {
            throw new ConflictException();
        }
        return rows;
    }

    static String stamp(List<Product> rows) {
        return rows.stream().sorted(Comparator.comparing(Product::getId)).map(p -> p.getId() + ":" + p.getVersion())
                .collect(Collectors.joining(","));
    }

    /** Rows that are in an order or have a review: their history must stay readable (BR-15). */
    static Set<Integer> soldRows(EntityManager em, List<Product> rows) {
        return new ProductDao(em).findOrderedOrReviewed(rows.stream().map(Product::getId).toList());
    }

    /** Reports a name used by another product together with the other form errors; checked again when saving. */
    private static void reportTakenName(String name, String currentName, Map<String, String> errors) {
        if (name.isEmpty() || errors.containsKey("name") || (currentName != null && currentName.equalsIgnoreCase(name))) {
            return;
        }
        if (!JpaUtil.read(em -> new ProductDao(em).findByName(name)).isEmpty()) {
            errors.put("name", "Tên này đã được dùng cho mặt hàng khác (BR-28)");
        }
    }

    private static void requireFreeName(EntityManager em, String name, String currentName) {
        if (currentName != null && currentName.equalsIgnoreCase(name)) {
            return;
        }
        if (!new ProductDao(em).findByName(name).isEmpty()) {
            throw new ValidationException(Map.of("name", "Tên này đã được dùng cho mặt hàng khác (BR-28)"));
        }
    }

    private static void requireFreeVariant(List<Product> rows, VariantValues v, Integer exceptId) {
        boolean taken = rows.stream().anyMatch(p -> !p.getId().equals(exceptId) && p.getColor().equalsIgnoreCase(v.color())
                && p.getSize().equalsIgnoreCase(v.size()));
        if (taken) {
            throw new ValidationException(Map.of("size", "Mặt hàng đã có biến thể " + v.color() + " / " + v.size()));
        }
    }

    private record Common(String name, ProductCategory category, String description, BigDecimal basePrice, boolean active) {
    }

    private record VariantValues(String color, String size, int stock, String imgUrl) {
        String key() {
            return color.toLowerCase(Locale.ROOT) + "|" + size.toLowerCase(Locale.ROOT);
        }
    }

    private static Common common(ProductForm form, Map<String, String> errors) {
        String name = required(form.name(), 200, "name", "tên mặt hàng", errors);
        ProductCategory category = null;
        try {
            category = ProductCategory.valueOf(form.category() == null ? "" : form.category().trim());
        } catch (IllegalArgumentException e) {
            errors.put("category", "Vui lòng chọn danh mục");
        }
        String description = form.description() == null || form.description().isBlank() ? null : form.description().trim();
        if (description != null && description.length() > DESCRIPTION_MAX) {
            errors.put("description", "Mô tả tối đa " + DESCRIPTION_MAX + " ký tự");
        }
        BigDecimal price = null;
        try {
            price = new BigDecimal(form.basePrice() == null ? "" : form.basePrice().trim());
            if (price.signum() <= 0 || price.compareTo(PRICE_MAX) > 0) {
                errors.put("basePrice", "Giá bán phải lớn hơn 0");
            } else if (price.remainder(BigDecimal.valueOf(1000)).signum() != 0) {
                errors.put("basePrice", "Giá bán phải là bội số của 1.000 (BR-21)");
            }
        } catch (NumberFormatException e) {
            errors.put("basePrice", "Giá bán phải là số tiền (VND)");
        }
        return new Common(name, category, description, price, form.active());
    }

    private static VariantValues variant(ProductForm.Variant form, String prefix, Map<String, String> errors, boolean withStock) {
        String color = required(form.color(), 50, prefix + "color", "màu", errors);
        String size = required(form.size(), 20, prefix + "size", "kích thước", errors);
        String img = required(form.imgUrl(), 255, prefix + "imgUrl", "ảnh (đường dẫn)", errors);
        int stock = 0;
        if (withStock) {
            try {
                stock = Integer.parseInt(form.stockQuantity() == null ? "" : form.stockQuantity().trim());
                if (stock < 0) {
                    errors.put(prefix + "stockQuantity", "Tồn kho không được âm");
                }
            } catch (NumberFormatException e) {
                errors.put(prefix + "stockQuantity", "Tồn kho phải là số nguyên");
            }
        }
        return color.isEmpty() || size.isEmpty() ? null : new VariantValues(color, size, stock, img);
    }

    private static String required(String raw, int max, String field, String label, Map<String, String> errors) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            errors.put(field, "Vui lòng nhập " + label);
        } else if (value.length() > max) {
            errors.put(field, "Tối đa " + max + " ký tự");
        }
        return value;
    }
}
