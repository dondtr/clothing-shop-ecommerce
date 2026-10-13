package com.clothingshop.service;

import com.clothingshop.dao.EmployeeDao;
import com.clothingshop.dao.OrderDao;
import com.clothingshop.dao.ReturnRequestDao;
import com.clothingshop.dto.ReturnRow;
import com.clothingshop.entity.Customer;
import com.clothingshop.entity.Order;
import com.clothingshop.entity.OrderStatus;
import com.clothingshop.entity.Payment;
import com.clothingshop.entity.PaymentMethod;
import com.clothingshop.entity.PaymentStatus;
import com.clothingshop.entity.ReturnRequest;
import com.clothingshop.entity.ReturnRequestStatus;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.util.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Returns and refunds of whole orders (FR-25, FR-26, BR-11): the customer asks within 15 days of delivery, staff
 * approve or reject. Approval refunds through VNPAY first (Q65); a COD refund is paid outside the system (Q30).
 */
public class ReturnService {

    private static final Logger LOG = LoggerFactory.getLogger(ReturnService.class);

    static final int REASON_MAX = 500;
    static final String MSG_CANNOT = "Đơn hàng không thể yêu cầu trả hàng: chỉ đơn đã giao thành công, đã thanh toán,"
            + " trong 15 ngày kể từ khi nhận hàng và chưa gửi yêu cầu nào";
    static final String MSG_HANDLED = "Yêu cầu trả hàng đã được xử lý, vui lòng tải lại";

    private final PaymentService payments;
    private final EmailService mail;
    private final Clock clock;

    public ReturnService(PaymentService payments, EmailService mail, Clock clock) {
        this.payments = payments;
        this.mail = mail;
        this.clock = clock;
    }

    /** FR-25 (ReturnRequest.submit()): only when Order.canRequestReturn() holds. */
    public void submit(int customerId, int orderId, String reason) {
        String text = reason == null ? "" : reason.trim();
        if (text.isEmpty()) {
            throw new ValidationException(Map.of("reason", "Vui lòng nhập lý do trả hàng"));
        }
        if (text.length() > REASON_MAX) {
            throw new ValidationException(Map.of("reason", "Lý do tối đa " + REASON_MAX + " ký tự"));
        }
        LocalDateTime now = LocalDateTime.now(clock);
        try {
            JpaUtil.inTransaction(em -> {
                Order o = new OrderDao(em).lock(orderId).filter(x -> x.getCustomer().getId() == customerId)
                        .orElseThrow(() -> new BusinessException(OrderQueries.MSG_NOT_FOUND));
                if (!canRequestReturn(em, o, now)) {
                    throw new BusinessException(MSG_CANNOT);
                }
                new ReturnRequestDao(em).persist(new ReturnRequest(o, now, text));
                return null;
            });
        } catch (PersistenceException e) {
            throw new BusinessException(MSG_CANNOT);   // a request for the order was just created (unique orderId)
        }
        LOG.info("Return requested: orderId={}", orderId);
    }

    /** Order.canRequestReturn() (BR-11): COMPLETED, paid, within 15 days of deliveredAt, no request yet. */
    static boolean canRequestReturn(EntityManager em, Order o, LocalDateTime now) {
        if (o.getStatus() != OrderStatus.COMPLETED || o.getDeliveredAt() == null
                || now.isAfter(o.getDeliveredAt().plus(OrderQueries.RETURN_WINDOW))) {
            return false;
        }
        if (new OrderDao(em).paymentOf(o.getId()).getStatus() != PaymentStatus.PAID) {
            return false;
        }
        return !new ReturnRequestDao(em).existsForOrder(o.getId());
    }

    /** Requests waiting for a decision (FR-26), oldest first, then the last decisions for reference. */
    public List<ReturnRow> list(boolean pending) {
        return JpaUtil.read(em -> new ReturnRequestDao(em).findForStaff(pending, pending ? 500 : 30).stream().map(r -> {
                    Order o = r.getOrder();
                    Payment p = new OrderDao(em).paymentOf(o.getId());
                    return new ReturnRow(r.getId(), o.getId(), o.getCustomer().getId(), o.getCustomer().getFullName(),
                            o.getTotalAmount(), p.getMethod(), o.getDeliveredAt(), r.getRequestedAt(), r.getReason(), r.getStatus(),
                            r.getHandledBy() == null ? null : r.getHandledBy().getFullName(), r.getHandledAt());
                }).toList());
    }

    /**
     * FR-26 approve (ReturnRequest.approve()): VNPAY refund first, the request staying PENDING if it fails (Q65); then
     * in one transaction APPROVED (handledBy, handledAt), payment REFUND, order CANCELLED, stock and voucher use given
     * back (Q29), level re-evaluated; then email E7.
     */
    public void approve(int requestId, int employeeId, String employeeUserName) {
        Object[] before = JpaUtil.read(em -> {
            ReturnRequest r = new ReturnRequestDao(em).findById(requestId).orElse(null);
            if (r == null || r.getStatus() != ReturnRequestStatus.PENDING) {
                throw new BusinessException(MSG_HANDLED);
            }
            Payment p = new OrderDao(em).paymentOf(r.getOrder().getId());
            return new Object[] {r.getOrder().getId(), p.getMethod()};
        });
        int orderId = (Integer) before[0];
        boolean vnpay = before[1] == PaymentMethod.VNPAY;
        if (vnpay) {
            payments.refundVnpay(orderId, employeeUserName);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Object[] done;
        try {
            done = JpaUtil.inTransaction(em -> {
                ReturnRequest r = new ReturnRequestDao(em).lock(requestId).orElseThrow(() -> new BusinessException(MSG_HANDLED));
                Order o = r.getOrder();
                Payment p = new OrderDao(em).paymentOf(o.getId());
                if (r.getStatus() != ReturnRequestStatus.PENDING || o.getStatus() != OrderStatus.COMPLETED
                        || p.getStatus() != PaymentStatus.PAID) {
                    throw new BusinessException(MSG_HANDLED);
                }
                r.setStatus(ReturnRequestStatus.APPROVED);
                r.setHandledBy(new EmployeeDao(em).reference(employeeId));
                r.setHandledAt(now);
                p.setStatus(PaymentStatus.REFUND);
                o.setStatus(OrderStatus.CANCELLED);
                OrderService.giveBack(em, o);
                new OrderDao(em).flush();
                CustomerService.evaluateLevel(em, o.getCustomer().getId());
                Customer c = o.getCustomer();
                return new Object[] {c.getEmail(), c.getFullName(), o.getTotalAmount()};
            });
        } catch (RuntimeException e) {
            if (vnpay) {
                LOG.error("Order {} was refunded through VNPAY but the return could not be approved; check it by hand", orderId, e);
            }
            throw e;
        }
        Emails.Message m = Emails.returnResult((String) done[1], orderId, true, Emails.refundNote(vnpay, (BigDecimal) done[2]));
        mail.send((String) done[0], m.subject(), m.body());
        LOG.info("Return approved: requestId={}, orderId={}", requestId, orderId);
    }

    /** FR-26 reject (ReturnRequest.reject()): REJECTED with handledBy, handledAt; the order stays COMPLETED; email E7. */
    public void reject(int requestId, int employeeId) {
        LocalDateTime now = LocalDateTime.now(clock);
        Object[] done = JpaUtil.inTransaction(em -> {
            ReturnRequest r = new ReturnRequestDao(em).lock(requestId).orElse(null);
            if (r == null || r.getStatus() != ReturnRequestStatus.PENDING) {
                throw new BusinessException(MSG_HANDLED);
            }
            r.setStatus(ReturnRequestStatus.REJECTED);
            r.setHandledBy(new EmployeeDao(em).reference(employeeId));
            r.setHandledAt(now);
            Customer c = r.getOrder().getCustomer();
            return new Object[] {c.getEmail(), c.getFullName(), r.getOrder().getId()};
        });
        Emails.Message m = Emails.returnResult((String) done[1], (Integer) done[2], false, "");
        mail.send((String) done[0], m.subject(), m.body());
        LOG.info("Return rejected: requestId={}", requestId);
    }
}
