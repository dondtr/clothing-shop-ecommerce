package com.clothingshop.service;

import com.clothingshop.dao.OrderDao;
import com.clothingshop.dao.PaymentDao;
import com.clothingshop.dao.ProductDao;
import com.clothingshop.dao.VoucherDao;
import com.clothingshop.entity.Order;
import com.clothingshop.entity.OrderDetail;
import com.clothingshop.entity.Payment;
import com.clothingshop.entity.PaymentMethod;
import com.clothingshop.entity.PaymentStatus;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.util.AppConfig;
import com.clothingshop.util.JpaUtil;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VNPAY payments (FR-13, section 3.6, Q58): an order waiting for payment is created before the customer goes to VNPAY;
 * the IPN call decides the result; task J7 asks VNPAY about orders whose payment time is over; refunds go through VNPAY
 * before an order is cancelled or a return approved (Q65).
 *
 * <p>Every change locks the payment row and checks it is still UNPAID, so an IPN and J7 arriving together never handle
 * one order twice.
 */
public class PaymentService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);

    static final String MSG_REFUND_FAILED = "Hoàn tiền qua VNPAY chưa thành công nên chưa thực hiện được. Vui lòng thử lại sau.";

    /** Answer to VNPAY's IPN call, as VNPAY expects it. */
    public record IpnResponse(String rspCode, String message) {
        public String json() {
            return "{\"RspCode\":\"" + rspCode + "\",\"Message\":\"" + message + "\"}";
        }
    }

    /** What the Return URL page shows (FR-13 step 5). */
    public enum ReturnStatus { PAID, WAITING, FAILED, INVALID }

    private final VnpayGateway vnpay;
    private final CartService cart;
    private final Clock clock;
    private final Duration timeout;
    private final boolean processReturnUrl;

    /**
     * @param vnpay            the gateway, or {@code null} when VNPAY is not configured
     * @param processReturnUrl also apply the signed result received on the Return URL ({@code vnpay.processReturnUrl}),
     *                         for a development server that VNPAY cannot reach with IPN
     */
    public PaymentService(VnpayGateway vnpay, CartService cart, Clock clock, boolean processReturnUrl) {
        this.vnpay = vnpay;
        this.cart = cart;
        this.clock = clock;
        this.timeout = Duration.ofMinutes(Long.parseLong(AppConfig.get("vnpay.paymentTimeoutMinutes", "15")));
        this.processReturnUrl = processReturnUrl;
    }

    public boolean isVnpayEnabled() {
        return vnpay != null;
    }

    /** FR-13 step 3: VNPAY's payment page for the order the customer has just placed; payable until orderDate + 15 min. */
    public String paymentUrl(int customerId, int orderId, String clientIp, String returnUrl) {
        if (vnpay == null) {
            throw new BusinessException("Thanh toán VNPAY chưa được cấu hình");
        }
        Order order = JpaUtil.read(em -> {
            Payment p = paymentOf(em, orderId, false).orElseThrow(() -> new BusinessException("Đơn hàng không tồn tại"));
            Order o = p.getOrder();
            if (o.getCustomer().getId() != customerId || p.getMethod() != PaymentMethod.VNPAY || p.getStatus() != PaymentStatus.UNPAID) {
                throw new BusinessException("Đơn hàng không chờ thanh toán VNPAY");
            }
            return o;
        });
        return vnpay.paymentUrl(orderId, order.getTotalAmount(), order.getOrderDate(), order.getOrderDate().plus(timeout),
                clientIp, returnUrl);
    }

    /** FR-13 step 4 (Payment.handleVnpayResult()): checks signature, order and amount, then applies the result once. */
    public IpnResponse handleIpn(Map<String, String> params) {
        if (vnpay == null || !vnpay.isSignatureValid(params)) {
            return new IpnResponse("97", "Invalid signature");
        }
        Integer orderId = parseId(params.get("vnp_TxnRef"));
        Optional<Object[]> found = orderId == null ? Optional.empty() : JpaUtil.read(em -> paymentOf(em, orderId, false)
                .map(p -> new Object[] {p.getMethod(), p.getStatus(), p.getOrder().getTotalAmount()}));
        if (found.isEmpty() || found.get()[0] != PaymentMethod.VNPAY) {
            if (orderId != null && "00".equals(params.get("vnp_ResponseCode"))) {
                LOG.error("VNPAY reports a successful payment for order {} which no longer waits for payment; refund by hand", orderId);
            }
            return new IpnResponse("01", "Order not found");
        }
        BigDecimal amount = VnpayClient.parseVnpAmount(params.get("vnp_Amount"));
        if (amount == null || amount.compareTo((BigDecimal) found.get()[2]) != 0) {
            return new IpnResponse("04", "Invalid amount");
        }
        if (found.get()[1] != PaymentStatus.UNPAID) {
            return new IpnResponse("02", "Order already confirmed");
        }
        boolean success = "00".equals(params.get("vnp_ResponseCode")) && "00".equals(params.get("vnp_TransactionStatus"));
        boolean changed = success ? markPaid(orderId, params.get("vnp_TransactionNo")) : discardPending(orderId);
        return changed ? new IpnResponse("00", "Confirm Success") : new IpnResponse("02", "Order already confirmed");
    }

    /** FR-13 step 5: the Return URL page shows the state of the payment; it changes nothing unless configured to. */
    public ReturnStatus returnStatus(Map<String, String> params) {
        if (vnpay == null || !vnpay.isSignatureValid(params)) {
            return ReturnStatus.INVALID;
        }
        if (processReturnUrl) {
            handleIpn(params);
        }
        Integer orderId = parseId(params.get("vnp_TxnRef"));
        Optional<PaymentStatus> status = orderId == null ? Optional.empty()
                : JpaUtil.read(em -> paymentOf(em, orderId, false).map(Payment::getStatus));
        if (status.isEmpty()) {
            return ReturnStatus.FAILED;   // the waiting order was discarded: payment failed, cancelled or expired
        }
        return status.get() == PaymentStatus.PAID ? ReturnStatus.PAID : ReturnStatus.WAITING;
    }

    /**
     * Task J7 (FR-13 step 7, every minute): for each order still waiting for VNPAY after its payment time, asks VNPAY;
     * paid → confirmed, not paid → discarded, no answer → kept and asked again next time.
     *
     * @return number of orders confirmed or discarded
     */
    public int reconcilePending() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minus(timeout);
        List<Object[]> waiting = JpaUtil.read(em -> new PaymentDao(em).findVnpayUnpaidPlacedBefore(cutoff));
        if (waiting.isEmpty()) {
            return 0;
        }
        if (vnpay == null) {
            LOG.warn("J7: {} orders wait for a VNPAY payment but VNPAY is not configured", waiting.size());
            return 0;
        }
        int handled = 0;
        for (Object[] w : waiting) {
            int orderId = (Integer) w[0];
            VnpayGateway.QueryResult r = vnpay.query(orderId, (LocalDateTime) w[1]);
            switch (r.status()) {
                case PAID -> {
                    if (r.amount() != null && r.amount().compareTo((BigDecimal) w[2]) == 0) {
                        handled += markPaid(orderId, r.transactionNo()) ? 1 : 0;
                    } else {
                        LOG.error("J7: VNPAY amount {} differs from order {} total; order kept", r.amount(), orderId);
                    }
                }
                case NOT_PAID -> handled += discardPending(orderId) ? 1 : 0;
                case UNKNOWN -> LOG.info("J7: no result yet for order {} ({}); will ask again", orderId, r.message());
            }
        }
        LOG.info("J7: {} orders waiting for VNPAY checked, {} confirmed or discarded", waiting.size(), handled);
        return handled;
    }

    /**
     * Payment.markPaid() for a VNPAY order: PAID with VNPAY's transaction number, the customer's level re-evaluated
     * (FR-27), then the ordered rows leave the cart (FR-13 step 4).
     *
     * @return {@code false} when the payment was no longer waiting (already handled)
     */
    boolean markPaid(int orderId, String transactionNo) {
        LocalDateTime now = LocalDateTime.now(clock);
        Object[] paid = JpaUtil.inTransaction(em -> {
            Optional<Payment> locked = paymentOf(em, orderId, true);
            if (locked.isEmpty() || locked.get().getMethod() != PaymentMethod.VNPAY || locked.get().getStatus() != PaymentStatus.UNPAID) {
                return null;
            }
            Payment p = locked.get();
            p.setStatus(PaymentStatus.PAID);
            p.setTransactionNo(transactionNo);
            p.setPaidAt(now);
            new OrderDao(em).flush();
            int customerId = p.getOrder().getCustomer().getId();
            CustomerService.evaluateLevel(em, customerId);
            Set<Integer> products = p.getOrder().getDetails().stream().map(d -> d.getProduct().getId()).collect(Collectors.toSet());
            return new Object[] {customerId, products};
        });
        if (paid == null) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Set<Integer> products = (Set<Integer>) paid[1];
        cart.removeItems((Integer) paid[0], products);
        LOG.info("VNPAY payment confirmed: orderId={}, transactionNo={}", orderId, transactionNo);
        return true;
    }

    /**
     * Order.discardPending(): deletes an unpaid VNPAY order (Order, OrderDetail, Payment) and gives back the stock and
     * the voucher use; the cart was never emptied (FR-13 step 6).
     *
     * @return {@code false} when the payment was no longer waiting (already handled)
     */
    boolean discardPending(int orderId) {
        boolean discarded = JpaUtil.inTransaction(em -> {
            Optional<Payment> locked = paymentOf(em, orderId, true);
            if (locked.isEmpty() || locked.get().getMethod() != PaymentMethod.VNPAY || locked.get().getStatus() != PaymentStatus.UNPAID) {
                return false;
            }
            Order order = locked.get().getOrder();
            ProductDao products = new ProductDao(em);
            for (OrderDetail d : order.getDetails()) {
                products.increaseStock(d.getProduct().getId(), d.getQuantity());
            }
            if (order.getVoucher() != null) {
                new VoucherDao(em).increaseQuantity(order.getVoucher().getCode());
            }
            new PaymentDao(em).remove(locked.get());
            OrderDao orders = new OrderDao(em);
            orders.flush();
            orders.remove(order);
            return true;
        });
        if (discarded) {
            LOG.info("Unpaid VNPAY order discarded: orderId={}", orderId);
        }
        return discarded;
    }

    /**
     * Payment.refund() through VNPAY for a paid VNPAY order, called before the order is cancelled or the return approved;
     * the caller then saves REFUND in its own transaction (Q65, SD08, SD09).
     *
     * @throws BusinessException when VNPAY does not accept the refund (nothing changes; try again later)
     */
    public void refundVnpay(int orderId, String createdBy) {
        if (vnpay == null) {
            throw new BusinessException(MSG_REFUND_FAILED);
        }
        Object[] p = JpaUtil.read(em -> paymentOf(em, orderId, false)
                .filter(x -> x.getMethod() == PaymentMethod.VNPAY && x.getStatus() == PaymentStatus.PAID)
                .map(x -> new Object[] {x.getOrder().getTotalAmount(), x.getTransactionNo(), x.getOrder().getOrderDate()})
                .orElseThrow(() -> new BusinessException("Đơn hàng không có thanh toán VNPAY để hoàn tiền")));
        if (!vnpay.refund(orderId, (BigDecimal) p[0], (String) p[1], (LocalDateTime) p[2], createdBy)) {
            throw new BusinessException(MSG_REFUND_FAILED);
        }
        LOG.info("VNPAY refund accepted: orderId={}, by={}", orderId, createdBy);
    }

    private static Optional<Payment> paymentOf(EntityManager em, int orderId, boolean lock) {
        PaymentDao payments = new PaymentDao(em);
        return lock ? payments.lockByOrder(orderId) : payments.findByOrder(orderId);
    }

    private static Integer parseId(String value) {
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
