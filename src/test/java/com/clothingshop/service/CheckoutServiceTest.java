package com.clothingshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clothingshop.TestDatabase;
import com.clothingshop.dto.AddressForm;
import com.clothingshop.dto.CheckoutView;
import com.clothingshop.entity.CustomerLevel;
import com.clothingshop.entity.Discount;
import com.clothingshop.entity.DiscountType;
import com.clothingshop.entity.EligibleCustomer;
import com.clothingshop.entity.Order;
import com.clothingshop.entity.OrderStatus;
import com.clothingshop.entity.Payment;
import com.clothingshop.entity.PaymentMethod;
import com.clothingshop.entity.PaymentStatus;
import com.clothingshop.entity.Product;
import com.clothingshop.entity.Voucher;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.PriceChangedException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.util.JpaUtil;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** FR-12 order placement (COD), FR-14 voucher, FR-15 amounts. */
class CheckoutServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final AddressForm ADDRESS = new AddressForm("TP. Hồ Chí Minh", "Phường Thủ Đức", "1 Võ Văn Ngân");

    private CartService cart;
    private CheckoutService checkout;
    private int customerId;
    private String name;

    @BeforeAll
    static void database() {
        TestDatabase.start();
    }

    @BeforeEach
    void setUp() {
        MutableClock clock = new MutableClock(TestData.NOW.atZone(ZONE).toInstant(), ZONE);
        cart = new CartService(new InMemoryCartStore(clock), clock);
        checkout = new CheckoutService(cart, new AddressService(), clock, false);
        customerId = TestData.customer("matkhau-123").getId();
        name = TestData.unique("Áo đặt hàng ");
    }

    private Product product(String size, int stock, String price) {
        return TestData.product(name, "Trắng", size, stock, price);
    }

    private CheckoutService.Request request(Set<Integer> ids, String voucher, BigDecimal expected) {
        return new CheckoutService.Request(ids, null, ADDRESS, "Giao giờ hành chính", voucher, PaymentMethod.COD, expected);
    }

    private int stock(Product p) {
        return JpaUtil.read(em -> em.find(Product.class, p.getId()).getStockQuantity());
    }

    private int voucherQuantity(String code) {
        return JpaUtil.read(em -> em.find(Voucher.class, code).getQuantity());
    }

    private long orders() {
        return JpaUtil.read(em -> em.createQuery("select count(o) from Order o where o.customer.id = :c", Long.class)
                .setParameter("c", customerId).getSingleResult());
    }

    @Test
    void placesACodOrderWithTheAmountsOfFr15() {
        Product discounted = product("S", 10, "150000");
        Product plain = product("M", 5, "200000");
        Product left = product("L", 5, "90000");
        JpaUtil.inTransaction(em -> {
            em.persist(new Discount(em.getReference(Product.class, discounted.getId()), new BigDecimal("15"),
                    TestData.NOW.minusDays(1), TestData.NOW.plusDays(1)));
            return null;
        });
        String code = TestData.voucher(DiscountType.PERCENT, "10", 5, EligibleCustomer.ALL, "300000", "50000");
        cart.addItem(customerId, discounted.getId(), 2);
        cart.addItem(customerId, plain.getId(), 1);
        cart.addItem(customerId, left.getId(), 1);
        Set<Integer> chosen = Set.of(discounted.getId(), plain.getId());

        CheckoutView preview = checkout.preview(customerId, request(chosen, code, null));
        assertEquals(new BigDecimal("456000"), preview.productAmount(), "128.000 × 2 + 200.000");
        assertEquals(new BigDecimal("46000"), preview.discount(), "10% = 45.600 → 46.000 (BR-21), under the 50.000 cap");
        assertEquals(new BigDecimal("430000"), preview.total(), "456.000 − 46.000 + 20.000");
        assertTrue(preview.canPlace());

        int orderId = checkout.place(customerId, request(chosen, code, preview.total()));
        JpaUtil.read(em -> {
            Order o = em.find(Order.class, orderId);
            assertEquals(OrderStatus.PENDING, o.getStatus());
            assertEquals(0, new BigDecimal("430000").compareTo(o.getTotalAmount()));
            assertEquals(0, new BigDecimal("20000").compareTo(o.getShippingFee()));
            assertEquals("1 Võ Văn Ngân, Phường Thủ Đức, TP. Hồ Chí Minh", o.getAddress());
            assertEquals(code, o.getVoucher().getCode());
            assertEquals(2, o.getDetails().size());
            assertTrue(o.getDetails().stream().anyMatch(d -> d.getQuantity() == 2 && d.getSubtotal().compareTo(new BigDecimal("256000")) == 0));
            Payment p = new com.clothingshop.dao.OrderDao(em).paymentOf(orderId);
            assertEquals(PaymentMethod.COD, p.getMethod());
            assertEquals(PaymentStatus.UNPAID, p.getStatus());
            return null;
        });
        assertEquals(8, stock(discounted));
        assertEquals(4, stock(plain));
        assertEquals(5, stock(left), "not ordered");
        assertEquals(4, voucherQuantity(code));
        assertEquals(Set.of(left.getId()), cart.lines(customerId).keySet(), "only the ordered lines leave the cart");
        assertTrue(new AddressService().list(customerId).get(0).defaultAddress(), "the new address was saved as the first one");
    }

    @Test
    void stockThatRanOutRefusesTheWholeOrder() {
        Product a = product("S", 5, "100000");
        Product b = product("M", 5, "100000");
        String code = TestData.voucher(DiscountType.FIXED_AMOUNT, "20000", 5, EligibleCustomer.ALL, "0", null);
        cart.addItem(customerId, a.getId(), 2);
        cart.addItem(customerId, b.getId(), 3);
        JpaUtil.inTransaction(em -> {
            em.find(Product.class, b.getId()).setStockQuantity(2);
            return null;
        });
        BusinessException e = assertThrows(BusinessException.class,
                () -> checkout.place(customerId, request(Set.of(a.getId(), b.getId()), code, null)));
        assertTrue(e.getMessage().contains("chỉ còn 2 sản phẩm"), e.getMessage());
        assertEquals(5, stock(a), "the stock taken for the first line is given back");
        assertEquals(5, voucherQuantity(code));
        assertEquals(0, orders());
        assertTrue(new AddressService().list(customerId).isEmpty(), "the new address is not saved either");
    }

    @Test
    void vouchersAreCheckedWithASpecificReason() {
        Product p = product("S", 5, "100000");
        cart.addItem(customerId, p.getId(), 1);
        Set<Integer> ids = Set.of(p.getId());
        String minimum = TestData.voucher(DiscountType.PERCENT, "10", 5, EligibleCustomer.ALL, "500000", null);
        String loyal = TestData.voucher(DiscountType.PERCENT, "10", 5, EligibleCustomer.LOYAL, "0", null);
        String usedUp = TestData.voucher(DiscountType.PERCENT, "10", 0, EligibleCustomer.ALL, "0", null);
        String expired = TestData.voucher(DiscountType.PERCENT, "10", 5, EligibleCustomer.ALL, "0", null,
                TestData.NOW.minusDays(10), TestData.NOW.minusDays(1));
        assertTrue(checkout.preview(customerId, request(ids, minimum, null)).voucherError().contains("500.000"));
        assertTrue(checkout.preview(customerId, request(ids, loyal, null)).voucherError().contains("trung thành"));
        assertEquals(VoucherService.MSG_USED_UP, checkout.preview(customerId, request(ids, usedUp, null)).voucherError());
        assertEquals("Voucher đã hết hạn", checkout.preview(customerId, request(ids, expired, null)).voucherError());
        assertEquals(VoucherService.MSG_NOT_FOUND, checkout.preview(customerId, request(ids, "KHONGCO", null)).voucherError());
        CheckoutView lower = checkout.preview(customerId, request(ids, minimum.toLowerCase(), null));
        assertFalse(lower.canPlace());
        assertThrows(BusinessException.class, () -> checkout.place(customerId, request(ids, loyal, null)));
        assertEquals(0, orders());
    }

    @Test
    void aFixedVoucherNeverExceedsTheProductAmount() {
        Product p = product("S", 5, "30000");
        cart.addItem(customerId, p.getId(), 1);
        String code = TestData.voucher(DiscountType.FIXED_AMOUNT, "50000", 5, EligibleCustomer.ALL, "0", null);
        CheckoutView v = checkout.preview(customerId, request(Set.of(p.getId()), code, null));
        assertEquals(new BigDecimal("30000"), v.discount());
        assertEquals(new BigDecimal("20000"), v.total(), "only the shipping fee is left");
    }

    @Test
    void aTotalDifferentFromTheConfirmedOneIsNotPlaced() {
        Product p = product("S", 5, "100000");
        cart.addItem(customerId, p.getId(), 1);
        assertThrows(PriceChangedException.class,
                () -> checkout.place(customerId, request(Set.of(p.getId()), null, new BigDecimal("99000"))));
        assertEquals(0, orders());
        assertEquals(5, stock(p));
    }

    @Test
    void linesNotInTheCartOrNoLongerSoldAreRefused() {
        Product p = product("S", 5, "100000");
        Product hidden = product("M", 5, "100000");
        cart.addItem(customerId, hidden.getId(), 1);
        JpaUtil.inTransaction(em -> {
            em.find(Product.class, hidden.getId()).setActive(false);
            return null;
        });
        assertEquals(CheckoutService.MSG_CART_CHANGED,
                assertThrows(BusinessException.class, () -> checkout.place(customerId, request(Set.of(p.getId()), null, null))).getMessage());
        assertTrue(assertThrows(BusinessException.class,
                () -> checkout.place(customerId, request(Set.of(hidden.getId()), null, null))).getMessage().contains("ngừng bán"));
        ValidationException note = assertThrows(ValidationException.class, () -> checkout.place(customerId,
                new CheckoutService.Request(Set.of(hidden.getId()), null, ADDRESS, "x".repeat(501), null, PaymentMethod.COD, null)));
        assertTrue(note.getErrors().containsKey("note"));
    }

    @Test
    void twoCustomersBuyingTheLastItemAtOnceGetOneOrder() throws Exception {
        Product last = product("S", 1, "100000");
        int other = TestData.customer("matkhau-123").getId();
        cart.addItem(customerId, last.getId(), 1);
        cart.addItem(other, last.getId(), 1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int buyer : new int[] {customerId, other}) {
                results.add(pool.submit(() -> checkout.place(buyer, request(Set.of(last.getId()), null, null))));
            }
            int placed = 0;
            for (Future<Integer> r : results) {
                try {
                    r.get();
                    placed++;
                } catch (java.util.concurrent.ExecutionException e) {
                    assertTrue(e.getCause() instanceof BusinessException, e.getCause().toString());
                }
            }
            assertEquals(1, placed);
        } finally {
            pool.shutdown();
        }
        assertEquals(0, stock(last), "never negative (section 10)");
    }

    @Test
    void theRecipientIsTheAccountHolderAndTheDefaultAddressIsPreselected() {
        Product p = product("S", 5, "100000");
        cart.addItem(customerId, p.getId(), 1);
        new AddressService().add(customerId, ADDRESS);
        CheckoutView v = checkout.preview(customerId, new CheckoutService.Request(null, null, null, null, null, null, null));
        assertEquals(new AddressService().list(customerId).get(0).id(), v.addressId());
        assertTrue(v.recipientName().startsWith("Khách "));
        assertNull(v.voucherError());
        assertEquals(CustomerLevel.CASUAL.name(), JpaUtil.read(em -> em.find(com.clothingshop.entity.Customer.class, customerId))
                .getCustomerLevel().name());
    }
}
