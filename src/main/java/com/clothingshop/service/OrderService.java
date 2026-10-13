package com.clothingshop.service;

import com.clothingshop.dao.DeliveryIssueDao;
import com.clothingshop.dao.EmployeeDao;
import com.clothingshop.dao.OrderDao;
import com.clothingshop.dao.ProductDao;
import com.clothingshop.dao.VoucherDao;
import com.clothingshop.entity.Customer;
import com.clothingshop.entity.DeliveryIssueStatus;
import com.clothingshop.entity.Order;
import com.clothingshop.entity.OrderDetail;
import com.clothingshop.entity.OrderStatus;
import com.clothingshop.entity.Payment;
import com.clothingshop.entity.PaymentMethod;
import com.clothingshop.entity.PaymentStatus;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.util.JpaUtil;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Order life cycle (section 6.1): staff confirm and ship (FR-18), the customer confirms receipt (FR-19) or the system
 * after 7 days (FR-22, J2), and cancellation by the customer (FR-17), by staff after a failed delivery (FR-23) or by the
 * system for COD orders not shipped within 15 days (FR-24, J3). Every change locks the order row and checks its status
 * again, so only the transitions of table 6.1 happen, once.
 */
public class OrderService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderService.class);

    static final Duration AUTO_COMPLETE_AFTER = Duration.ofDays(7);    // BR-13
    static final Duration COD_SHIP_DEADLINE = Duration.ofDays(15);     // BR-12
    static final String MSG_CHANGED = "Đơn hàng vừa thay đổi trạng thái, vui lòng tải lại trang";

    /** Why an order is cancelled; the text goes into email E6. */
    public enum CancelReason {
        CUSTOMER("Khách hàng hủy đơn", EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.SHIPPING)),
        DELIVERY_FAILED("Giao hàng không thành công", EnumSet.of(OrderStatus.SHIPPING)),
        COD_NOT_SHIPPED("Đơn thanh toán khi nhận hàng quá 15 ngày chưa được giao", EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED));

        final String text;
        final Set<OrderStatus> from;

        CancelReason(String text, Set<OrderStatus> from) {
            this.text = text;
            this.from = from;
        }
    }

    /** Result of a "received" confirmation: whether it completed the order now, and the order's status afterwards. */
    public record ReceivedResult(boolean completedNow, OrderStatus status) {
    }

    private final PaymentService payments;
    private final EmailService mail;
    private final DeliveryLinks links;
    private final Clock clock;

    public OrderService(PaymentService payments, EmailService mail, DeliveryLinks links, Clock clock) {
        this.payments = payments;
        this.mail = mail;
        this.links = links;
        this.clock = clock;
    }

    /** FR-18 step 1: PENDING → CONFIRMED. */
    public void confirm(int orderId) {
        JpaUtil.inTransaction(em -> {
            Order o = lockVisible(em, orderId);
            require(o, OrderStatus.PENDING);
            o.setStatus(OrderStatus.CONFIRMED);
            return null;
        });
        LOG.info("Order confirmed: orderId={}", orderId);
    }

    /** FR-18 step 2 (Order.markShipped()): CONFIRMED → SHIPPING, shippedAt = now (Q03), then email E1. */
    public void ship(int orderId) {
        LocalDateTime now = LocalDateTime.now(clock);
        String[] recipient = JpaUtil.inTransaction(em -> {
            Order o = lockVisible(em, orderId);
            require(o, OrderStatus.CONFIRMED);
            o.setStatus(OrderStatus.SHIPPING);
            o.setShippedAt(now);
            return new String[] {o.getCustomer().getEmail(), o.getCustomer().getFullName()};
        });
        Emails.Message m = Emails.shipped(recipient[1], orderId, links.received(orderId));
        mail.send(recipient[0], m.subject(), m.body());
        LOG.info("Order shipped: orderId={}", orderId);
    }

    /** FR-19 from the order page: only the customer's own order. */
    public ReceivedResult confirmReceivedByCustomer(int customerId, int orderId) {
        boolean own = JpaUtil.read(em -> new OrderDao(em).findById(orderId)
                .map(o -> o.getCustomer().getId() == customerId).orElse(false));
        if (!own) {
            throw new BusinessException(OrderQueries.MSG_NOT_FOUND);
        }
        return confirmReceived(orderId);
    }

    /**
     * Order.confirmReceived() (FR-19, FR-22): SHIPPING → COMPLETED with deliveredAt; a COD payment becomes PAID and the
     * level is re-evaluated (FR-27); an open delivery issue is closed with no resolution (Q62). An order no longer being
     * delivered is left as it is (Q24).
     */
    public ReceivedResult confirmReceived(int orderId) {
        LocalDateTime now = LocalDateTime.now(clock);
        ReceivedResult result = JpaUtil.inTransaction(em -> {
            Order o = new OrderDao(em).lock(orderId).orElseThrow(() -> new BusinessException(OrderQueries.MSG_NOT_FOUND));
            if (o.getStatus() != OrderStatus.SHIPPING) {
                return new ReceivedResult(false, o.getStatus());
            }
            o.setStatus(OrderStatus.COMPLETED);
            o.setDeliveredAt(now);
            Payment p = new OrderDao(em).paymentOf(orderId);
            if (p.getMethod() == PaymentMethod.COD && p.getStatus() == PaymentStatus.UNPAID) {
                p.setStatus(PaymentStatus.PAID);
                p.setPaidAt(now);
                new OrderDao(em).flush();
                CustomerService.evaluateLevel(em, o.getCustomer().getId());
            }
            closeOpenIssue(em, orderId, null, now);
            return new ReceivedResult(true, OrderStatus.COMPLETED);
        });
        if (result.completedNow()) {
            LOG.info("Order completed: orderId={}", orderId);
        }
        return result;
    }

    /** FR-17: the customer cancels an order not yet delivered (BR-10). */
    public void cancelByCustomer(int customerId, String userName, int orderId) {
        boolean own = JpaUtil.read(em -> new OrderDao(em).findById(orderId)
                .map(o -> o.getCustomer().getId() == customerId && new OrderDao(em).isVisible(orderId)).orElse(false));
        if (!own) {
            throw new BusinessException(OrderQueries.MSG_NOT_FOUND);
        }
        cancel(orderId, null, userName, CancelReason.CUSTOMER);
    }

    /** FR-23: staff cancel an order the carrier could not deliver. */
    public void cancelFailedDelivery(int orderId, int employeeId, String employeeUserName) {
        cancel(orderId, employeeId, employeeUserName, CancelReason.DELIVERY_FAILED);
    }

    /**
     * Order.cancel() for FR-17, FR-23 and FR-24. A paid VNPAY order is refunded through VNPAY first and cancelled only if
     * the refund succeeds (Q65). Then, in one transaction: CANCELLED; stock back for every line, even while shipping
     * (Q28); the voucher use back; COD UNPAID → FAILED or VNPAY PAID → REFUND (level re-evaluated); an open delivery
     * issue closed. Email E6 follows.
     */
    void cancel(int orderId, Integer employeeId, String actor, CancelReason reason) {
        Object[] before = JpaUtil.read(em -> {
            OrderDao orders = new OrderDao(em);
            Order o = orders.findById(orderId).filter(x -> orders.isVisible(orderId))
                    .orElseThrow(() -> new BusinessException(OrderQueries.MSG_NOT_FOUND));
            Payment p = orders.paymentOf(orderId);
            return new Object[] {o.getStatus(), p.getMethod(), p.getStatus()};
        });
        if (!reason.from.contains((OrderStatus) before[0])) {
            throw new BusinessException("Không thể hủy đơn hàng đang ở trạng thái \"" + ((OrderStatus) before[0]).getLabel() + "\"");
        }
        boolean refundVnpay = before[1] == PaymentMethod.VNPAY && before[2] == PaymentStatus.PAID;
        if (refundVnpay) {
            payments.refundVnpay(orderId, actor);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Object[] done;
        try {
            done = JpaUtil.inTransaction(em -> {
                Order o = lockVisible(em, orderId);
                if (!reason.from.contains(o.getStatus())) {
                    throw new BusinessException(MSG_CHANGED);
                }
                o.setStatus(OrderStatus.CANCELLED);
                giveBack(em, o);
                Payment p = new OrderDao(em).paymentOf(orderId);
                BigDecimal refunded = BigDecimal.ZERO;
                if (p.getStatus() == PaymentStatus.UNPAID) {
                    p.setStatus(PaymentStatus.FAILED);
                } else if (p.getStatus() == PaymentStatus.PAID) {
                    p.setStatus(PaymentStatus.REFUND);
                    refunded = o.getTotalAmount();
                    new OrderDao(em).flush();
                    CustomerService.evaluateLevel(em, o.getCustomer().getId());
                }
                closeOpenIssue(em, orderId, employeeId, now);
                Customer c = o.getCustomer();
                return new Object[] {c.getEmail(), c.getFullName(), p.getMethod(), refunded};
            });
        } catch (RuntimeException e) {
            if (refundVnpay) {
                LOG.error("Order {} was refunded through VNPAY but could not be cancelled; check it by hand", orderId, e);
            }
            throw e;
        }
        BigDecimal refunded = (BigDecimal) done[3];
        String refundNote = refunded.signum() > 0 ? Emails.refundNote(done[2] == PaymentMethod.VNPAY, refunded) : "";
        Emails.Message m = Emails.cancelled((String) done[1], orderId, reason.text, refundNote);
        mail.send((String) done[0], m.subject(), m.body());
        LOG.info("Order cancelled: orderId={}, reason={}, by={}", orderId, reason, actor);
    }

    /**
     * Task J2 (FR-22, daily): completes orders still being delivered 7 days after the reference time, which is resolvedAt
     * when a delivery issue was handled, otherwise shippedAt; never while an issue is open (BR-13).
     *
     * @return number of orders completed
     */
    public int autoComplete() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Object[]> shipping = JpaUtil.read(em -> new OrderDao(em).findShippingWithIssue());
        int completed = 0;
        for (Object[] r : shipping) {
            if (r[2] == DeliveryIssueStatus.OPEN) {
                continue;
            }
            LocalDateTime reference = r[3] != null ? (LocalDateTime) r[3] : (LocalDateTime) r[1];
            if (reference != null && !reference.plus(AUTO_COMPLETE_AFTER).isAfter(now)) {
                completed += confirmReceived((Integer) r[0]).completedNow() ? 1 : 0;
            }
        }
        LOG.info("J2: {} orders being delivered checked, {} completed automatically", shipping.size(), completed);
        return completed;
    }

    /**
     * Task J3 (FR-24, daily): cancels COD orders still PENDING or CONFIRMED more than 15 days after they were placed (BR-12).
     *
     * @return number of orders cancelled
     */
    public int autoCancelCod() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minus(COD_SHIP_DEADLINE);
        List<Integer> late = JpaUtil.read(em -> new OrderDao(em).findCodNotShippedBefore(cutoff));
        int cancelled = 0;
        for (int id : late) {
            try {
                cancel(id, null, "system", CancelReason.COD_NOT_SHIPPED);
                cancelled++;
            } catch (BusinessException e) {
                LOG.info("J3: order {} not cancelled: {}", id, e.getMessage());
            }
        }
        LOG.info("J3: {} COD orders not shipped within 15 days, {} cancelled", late.size(), cancelled);
        return cancelled;
    }

    /** Gives back the stock of every line and the voucher use (BR-05, BR-09); shared with approved returns (FR-26). */
    static void giveBack(EntityManager em, Order o) {
        ProductDao products = new ProductDao(em);
        for (OrderDetail d : o.getDetails()) {
            products.increaseStock(d.getProduct().getId(), d.getQuantity());
        }
        if (o.getVoucher() != null) {
            new VoucherDao(em).increaseQuantity(o.getVoucher().getCode());
        }
    }

    /** Closes an open delivery issue of the order with no resolution (Q62); resolvedBy is the employee who cancelled, if any. */
    private static void closeOpenIssue(EntityManager em, int orderId, Integer employeeId, LocalDateTime now) {
        new DeliveryIssueDao(em).findOpenByOrder(orderId).ifPresent(i -> {
                    i.setStatus(DeliveryIssueStatus.RESOLVED);
                    i.setResolution(null);
                    i.setResolvedBy(employeeId == null ? null : new EmployeeDao(em).reference(employeeId));
                    i.setResolvedAt(now);
                });
    }

    private static Order lockVisible(EntityManager em, int orderId) {
        OrderDao orders = new OrderDao(em);
        Order o = orders.lock(orderId).orElseThrow(() -> new BusinessException(OrderQueries.MSG_NOT_FOUND));
        if (!orders.isVisible(orderId)) {
            throw new BusinessException(OrderQueries.MSG_NOT_FOUND);
        }
        return o;
    }

    private static void require(Order o, OrderStatus expected) {
        if (o.getStatus() != expected) {
            throw new BusinessException(MSG_CHANGED);
        }
    }
}
