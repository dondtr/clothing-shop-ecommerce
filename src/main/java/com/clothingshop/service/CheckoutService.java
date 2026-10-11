package com.clothingshop.service;

import com.clothingshop.dao.DiscountDao;
import com.clothingshop.dao.ProductDao;
import com.clothingshop.dao.VoucherDao;
import com.clothingshop.dto.AddressForm;
import com.clothingshop.dto.AddressView;
import com.clothingshop.dto.CartView;
import com.clothingshop.dto.CheckoutView;
import com.clothingshop.entity.Address;
import com.clothingshop.entity.Customer;
import com.clothingshop.entity.Discount;
import com.clothingshop.entity.Order;
import com.clothingshop.entity.OrderDetail;
import com.clothingshop.entity.Payment;
import com.clothingshop.entity.PaymentMethod;
import com.clothingshop.entity.Product;
import com.clothingshop.entity.Voucher;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.PriceChangedException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.util.AppConfig;
import com.clothingshop.util.JpaUtil;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placing an order from the cart (FR-12 COD, FR-13 VNPAY) with a voucher (FR-14) and the amounts of FR-15.
 *
 * <p>The page shows a preview computed from the cart; "Xác nhận mua hàng" computes everything again inside one database
 * transaction (Order.place()): rows still on sale, stock taken with conditional UPDATEs, voucher checked and one use
 * taken, then Order, OrderDetail and Payment created. Any failure leaves nothing changed (BR-04, BR-05, BR-09).
 */
public class CheckoutService {

    private static final Logger LOG = LoggerFactory.getLogger(CheckoutService.class);

    public static final String MSG_CART_CHANGED = "Giỏ hàng đã thay đổi, vui lòng kiểm tra lại";
    static final int NOTE_MAX = 500;

    /**
     * What the customer chose on the checkout page.
     *
     * @param productIds    cart rows to order; {@code null} means every line that can be ordered
     * @param addressId     a saved address, or {@code null} to use {@code newAddress}
     * @param expectedTotal the total the customer saw when confirming, or {@code null} for a preview
     */
    public record Request(Set<Integer> productIds, Integer addressId, AddressForm newAddress, String note, String voucherCode,
                          PaymentMethod method, BigDecimal expectedTotal) {
    }

    private final CartService cart;
    private final AddressService addresses;
    private final Clock clock;
    private final boolean vnpayEnabled;
    private final BigDecimal shippingFee;

    public CheckoutService(CartService cart, AddressService addresses, Clock clock, boolean vnpayEnabled) {
        this.cart = cart;
        this.addresses = addresses;
        this.clock = clock;
        this.vnpayEnabled = vnpayEnabled;
        this.shippingFee = shippingFee();
    }

    /** BR-03: a fixed fee per order, 20.000 VND unless {@code shop.shippingFee} says otherwise (section 3.5). */
    public static BigDecimal shippingFee() {
        return new BigDecimal(AppConfig.get("shop.shippingFee", "20000"));
    }

    /** FR-12 steps 1 – 3: the page with the amounts for the current choice; nothing is saved. */
    public CheckoutView preview(int customerId, Request request) {
        LocalDateTime now = LocalDateTime.now(clock);
        CartView cartView = cart.view(customerId);
        List<CheckoutView.Line> lines = new ArrayList<>();
        BigDecimal productAmount = BigDecimal.ZERO;
        for (CartView.Line l : cartView.lines()) {
            boolean selected = l.orderable() && (request.productIds() == null || request.productIds().contains(l.productId()));
            if (selected) {
                productAmount = productAmount.add(l.subtotal());
            }
            lines.add(new CheckoutView.Line(l.productId(), l.name(), l.color(), l.size(), l.imgUrl(), l.unitPrice(),
                    l.quantity(), l.subtotal(), l.status(), l.orderable(), selected));
        }
        List<String> problems = new ArrayList<>();
        if (lines.stream().noneMatch(CheckoutView.Line::selected)) {
            problems.add("Vui lòng chọn ít nhất một sản phẩm còn đặt được trong giỏ hàng");
        }
        List<AddressView> saved = addresses.list(customerId);
        Integer addressId = request.addressId();
        if (addressId == null && request.newAddress() == null) {
            addressId = saved.stream().filter(AddressView::defaultAddress).map(AddressView::id).findFirst().orElse(null);
        }
        if (addressId == null && request.newAddress() == null) {
            problems.add("Vui lòng nhập địa chỉ nhận hàng");
        }
        PaymentMethod method = request.method() == null ? PaymentMethod.COD : request.method();
        if (method == PaymentMethod.VNPAY && !vnpayEnabled) {
            problems.add("Thanh toán VNPAY chưa được cấu hình trên máy chủ này; vui lòng chọn thanh toán khi nhận hàng");
        }

        String code = VoucherService.normalizeCode(request.voucherCode());
        BigDecimal amount = productAmount;
        String[] voucherError = new String[1];
        BigDecimal discount = code.isEmpty() ? BigDecimal.ZERO : JpaUtil.read(em -> {
            try {
                Customer customer = em.find(Customer.class, customerId);
                return VoucherService.checkUsable(em, code, customer.getCustomerLevel(), amount, now).calcDiscount(amount);
            } catch (BusinessException e) {
                voucherError[0] = e.getMessage();
                return BigDecimal.ZERO;
            }
        });
        Customer customer = JpaUtil.read(em -> em.find(Customer.class, customerId));
        return new CheckoutView(List.copyOf(lines), saved, addressId, request.newAddress(), customer.getFullName(),
                customer.getPhone(), request.note(), code, voucherError[0], method, productAmount, discount, shippingFee,
                productAmount.subtract(discount).add(shippingFee), List.copyOf(problems),
                problems.isEmpty() && voucherError[0] == null, vnpayEnabled);
    }

    /**
     * FR-12 step 4 (FR-13 step 2 for VNPAY): places the order in one transaction and returns its id. A COD order's lines
     * leave the cart at once (FR-12 step 5); a VNPAY order keeps them until the payment succeeds (FR-13 step 4).
     *
     * @throws ValidationException    the note or the new address breaks a rule
     * @throws PriceChangedException  the total is no longer the one the customer confirmed
     * @throws BusinessException      a line, the stock or the voucher no longer allows the order (nothing is saved)
     */
    public int place(int customerId, Request request) {
        String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
        if (note != null && note.length() > NOTE_MAX) {
            throw new ValidationException(Map.of("note", "Ghi chú tối đa " + NOTE_MAX + " ký tự"));
        }
        AddressForm newAddress = request.addressId() == null ? AddressService.validate(
                request.newAddress() == null ? new AddressForm(null, null, null) : request.newAddress()) : null;
        PaymentMethod method = request.method() == null ? PaymentMethod.COD : request.method();
        if (method == PaymentMethod.VNPAY && !vnpayEnabled) {
            throw new BusinessException("Thanh toán VNPAY chưa được cấu hình trên máy chủ này");
        }
        Map<Integer, Integer> inCart = cart.lines(customerId);
        // Rows in id order: concurrent orders take stock in the same order and cannot deadlock each other
        TreeMap<Integer, Integer> chosen = new TreeMap<>();
        Set<Integer> wanted = request.productIds() == null ? inCart.keySet() : request.productIds();
        for (Integer id : wanted) {
            if (!inCart.containsKey(id)) {
                throw new BusinessException(MSG_CART_CHANGED);
            }
            chosen.put(id, inCart.get(id));
        }
        if (chosen.isEmpty()) {
            throw new BusinessException("Vui lòng chọn ít nhất một sản phẩm");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        int orderId = JpaUtil.inTransaction(em -> {
            Customer customer = em.find(Customer.class, customerId);
            String address = request.addressId() != null ? savedAddress(em, customerId, request.addressId())
                    : AddressService.persistNew(em, customerId, newAddress, false).toAddressString();

            ProductDao products = new ProductDao(em);
            Map<Integer, Discount> discounts = new DiscountDao(em).findAvailable(chosen.keySet(), now);
            List<OrderDetail> details = new ArrayList<>();
            BigDecimal productAmount = BigDecimal.ZERO;
            for (Map.Entry<Integer, Integer> line : chosen.entrySet()) {
                Product p = products.findById(line.getKey()).orElseThrow(() -> new BusinessException(MSG_CART_CHANGED));
                String label = "\"" + p.getName() + "\" (" + p.getColor() + ", " + p.getSize() + ")";
                if (!p.isActive()) {
                    throw new BusinessException(label + " đã ngừng bán, vui lòng bỏ khỏi đơn");
                }
                int quantity = line.getValue();
                if (!products.decreaseStock(p.getId(), quantity)) {
                    int left = products.stockOf(p.getId());
                    throw new BusinessException(left == 0 ? label + " đã hết hàng" : label + " chỉ còn " + left + " sản phẩm");
                }
                // OrderDetail.calcUnitPrice() / calcSubtotal(): price after discount now, times the quantity (Q02, Q45)
                BigDecimal subtotal = p.getCurrentPrice(discounts.get(p.getId())).multiply(BigDecimal.valueOf(quantity));
                productAmount = productAmount.add(subtotal);
                details.add(new OrderDetail(p, quantity, subtotal));
            }

            Voucher voucher = null;
            BigDecimal discount = BigDecimal.ZERO;
            String code = VoucherService.normalizeCode(request.voucherCode());
            if (!code.isEmpty()) {
                voucher = VoucherService.checkUsable(em, code, customer.getCustomerLevel(), productAmount, now);
                if (!new VoucherDao(em).decreaseQuantity(voucher.getCode())) {
                    throw new BusinessException(VoucherService.MSG_USED_UP);
                }
                discount = voucher.calcDiscount(productAmount);
            }
            // Order.calcTotal() (BR-04)
            BigDecimal total = productAmount.subtract(discount).add(shippingFee);
            if (request.expectedTotal() != null && request.expectedTotal().compareTo(total) != 0) {
                throw new PriceChangedException();
            }

            Order order = new Order(customer, voucher, now, address, note, shippingFee, total);
            details.forEach(order::addDetail);
            em.persist(order);
            em.persist(new Payment(order, method));
            return order.getId();
        });
        if (method == PaymentMethod.COD) {
            cart.removeItems(customerId, chosen.keySet());
        }
        LOG.info("Order placed: orderId={}, customerId={}, method={}", orderId, customerId, method);
        return orderId;
    }

    private static String savedAddress(EntityManager em, int customerId, int addressId) {
        return em.createQuery("select a from Address a where a.id = :id and a.customer.id = :c", Address.class)
                .setParameter("id", addressId).setParameter("c", customerId).getResultStream().findFirst()
                .orElseThrow(() -> new BusinessException("Địa chỉ nhận hàng không còn, vui lòng chọn lại")).toAddressString();
    }
}
