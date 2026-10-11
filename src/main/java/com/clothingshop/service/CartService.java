package com.clothingshop.service;

import com.clothingshop.dao.DiscountDao;
import com.clothingshop.dao.ProductDao;
import com.clothingshop.dto.CartView;
import com.clothingshop.entity.Discount;
import com.clothingshop.entity.Product;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.util.JpaUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shopping cart of a signed-in customer (FR-09). The cart is not stored in the database (Q18); it lives in a
 * {@link CartStore} keyed by userId. Stock and sale status are read from the database before each change; checkout
 * checks them again inside its transaction (FR-12), because they can change after a row is added.
 */
public class CartService {

    static final String MSG_QUANTITY = "Số lượng phải là số nguyên từ 1 trở lên.";
    static final String MSG_NOT_FOUND = "Sản phẩm không tồn tại hoặc đã bị xóa.";
    /** Keeps a cart small in memory; far above what a customer buys in one order. */
    static final int MAX_LINES = 50;

    private final CartStore store;
    private final Clock clock;

    public CartService(CartStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    /**
     * FR-09 step 1: adds {@code quantity} of row {@code productId}; a row already in the cart gets the quantities added.
     * Only rows on sale with stock can be added, and the quantity in the cart cannot exceed the stock.
     *
     * @return the quantity of the row now in the cart
     */
    public int addItem(int userId, int productId, int quantity) {
        requirePositive(quantity);
        Product product = find(productId).orElseThrow(() -> new BusinessException(MSG_NOT_FOUND));
        String label = label(product);
        if (!product.isActive()) {
            throw new BusinessException(label + " đã ngừng bán.");
        }
        int stock = product.getStockQuantity();
        if (stock <= 0) {
            throw new BusinessException(label + " đã hết hàng.");
        }
        return store.update(userId, cart -> {
            int inCart = cart.quantityOf(productId);
            if (inCart == 0 && cart.size() >= MAX_LINES) {
                throw new BusinessException("Giỏ hàng có tối đa " + MAX_LINES + " sản phẩm khác nhau.");
            }
            if (inCart + quantity > stock) {
                throw new BusinessException(inCart == 0 ? label + " chỉ còn " + stock + " sản phẩm."
                        : label + " chỉ còn " + stock + " sản phẩm, giỏ hàng đã có " + inCart + ".");
            }
            cart.addItem(productId, quantity);
            return inCart + quantity;
        });
    }

    /** FR-09 step 2: sets the quantity of a row already in the cart, at most its stock. */
    public void updateQuantity(int userId, int productId, int quantity) {
        requirePositive(quantity);
        Optional<Product> found = find(productId);
        if (found.isEmpty()) {
            removeItem(userId, productId);
            throw new BusinessException(MSG_NOT_FOUND + " Đã bỏ khỏi giỏ hàng.");
        }
        Product product = found.get();
        String label = label(product);
        if (!product.isActive()) {
            throw new BusinessException(label + " đã ngừng bán, chỉ có thể xóa khỏi giỏ hàng.");
        }
        int stock = product.getStockQuantity();
        if (quantity > stock) {
            throw new BusinessException(stock == 0 ? label + " đã hết hàng." : label + " chỉ còn " + stock + " sản phẩm.");
        }
        store.update(userId, cart -> {
            if (!cart.contains(productId)) {
                throw new BusinessException(label + " không còn trong giỏ hàng.");
            }
            cart.updateQuantity(productId, quantity);
            return null;
        });
    }

    /** FR-09 step 2: removes a row from the cart. */
    public void removeItem(int userId, int productId) {
        store.update(userId, cart -> {
            cart.removeItem(productId);
            return null;
        });
    }

    /**
     * FR-09 step 3: the cart at the current price after discount. Rows that no longer exist (deleted by the manager,
     * FR-31) are dropped from the cart.
     */
    public CartView view(int userId) {
        Map<Integer, Integer> lines = store.lines(userId);
        if (lines.isEmpty()) {
            return CartView.EMPTY;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Map<Integer, Product> products = new HashMap<>();
        Map<Integer, Discount> discounts = JpaUtil.read(em -> {
            new ProductDao(em).findByIds(lines.keySet()).forEach(p -> products.put(p.getId(), p));
            return new DiscountDao(em).findAvailable(products.keySet(), now);
        });

        Set<Integer> deleted = lines.keySet().stream().filter(id -> !products.containsKey(id)).collect(Collectors.toSet());
        if (!deleted.isEmpty()) {
            store.update(userId, cart -> {
                deleted.forEach(cart::removeItem);
                return null;
            });
        }

        List<CartView.Line> result = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int orderable = 0;
        for (Map.Entry<Integer, Integer> line : lines.entrySet()) {
            Product p = products.get(line.getKey());
            if (p == null) {
                continue;
            }
            int quantity = line.getValue();
            BigDecimal basePrice = p.getBasePrice().setScale(0, RoundingMode.HALF_UP);
            BigDecimal unitPrice = p.getCurrentPrice(discounts.get(p.getId()));
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            CartView.Status status = !p.isActive() ? CartView.Status.NOT_ON_SALE
                    : p.getStockQuantity() == 0 ? CartView.Status.OUT_OF_STOCK
                    : quantity > p.getStockQuantity() ? CartView.Status.NOT_ENOUGH_STOCK
                    : CartView.Status.OK;
            boolean ok = status == CartView.Status.OK;
            if (ok) {
                total = total.add(subtotal);
                orderable++;
            }
            result.add(new CartView.Line(p.getId(), p.getName(), p.getColor(), p.getSize(), p.getImgUrl(), basePrice,
                    unitPrice, unitPrice.compareTo(basePrice) < 0, quantity, subtotal, p.getStockQuantity(), status, ok));
        }
        return new CartView(List.copyOf(result), total, orderable, orderable < result.size());
    }

    private static void requirePositive(int quantity) {
        if (quantity < 1) {
            throw new BusinessException(MSG_QUANTITY);
        }
    }

    private static Optional<Product> find(int productId) {
        return JpaUtil.read(em -> new ProductDao(em).findById(productId));
    }

    /** "Áo thun" (Đen, M): names the row in messages. */
    private static String label(Product p) {
        return "\"" + p.getName() + "\" (" + p.getColor() + ", " + p.getSize() + ")";
    }
}
