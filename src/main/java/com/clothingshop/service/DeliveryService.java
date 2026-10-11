package com.clothingshop.service;

import com.clothingshop.dao.OrderDao;
import com.clothingshop.dao.ProductDao;
import com.clothingshop.dto.IssueRow;
import com.clothingshop.entity.DeliveryIssue;
import com.clothingshop.entity.DeliveryIssueStatus;
import com.clothingshop.entity.DeliveryResolution;
import com.clothingshop.entity.Employee;
import com.clothingshop.entity.Order;
import com.clothingshop.entity.OrderDetail;
import com.clothingshop.entity.OrderStatus;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.util.JpaUtil;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delivery follow-up: the reminder after 3 days (FR-20, J1), the customer's "Chưa thấy hàng đâu" report (FR-20) and its
 * handling by staff (FR-21). The orders stay SHIPPING throughout; completion and cancellation are in {@link OrderService}.
 */
public class DeliveryService {

    private static final Logger LOG = LoggerFactory.getLogger(DeliveryService.class);

    static final int TEXT_MAX = 500;

    /** Result of a "Chưa thấy hàng đâu" click. */
    public enum ReportResult {
        /** A new open issue was recorded. */
        CREATED,
        /** The order already has an issue, open or handled: nothing new (Q54). */
        ALREADY_REPORTED,
        /** The order is no longer being delivered: nothing changes (Q24). */
        NOT_SHIPPING
    }

    private final EmailService mail;
    private final DeliveryLinks links;
    private final Clock clock;

    public DeliveryService(EmailService mail, DeliveryLinks links, Clock clock) {
        this.mail = mail;
        this.links = links;
        this.clock = clock;
    }

    /**
     * Task J1 (FR-20, 00:00 every day, exactly once): email E2 for orders shipped in [today − 4 days, today − 3 days).
     * Each order falls in that window on exactly one day, so nothing records that the email was sent (Q04).
     *
     * @return number of reminders sent
     */
    public int sendReminders() {
        LocalDateTime today = LocalDate.now(clock).atStartOfDay();
        List<Object[]> due = JpaUtil.read(em -> em.createQuery("select o.id, c.email, c.fullName from Order o join o.customer c"
                        + " where o.status = :s and o.shippedAt >= :from and o.shippedAt < :to", Object[].class)
                .setParameter("s", OrderStatus.SHIPPING).setParameter("from", today.minusDays(4)).setParameter("to", today.minusDays(3))
                .getResultList());
        for (Object[] r : due) {
            int orderId = (Integer) r[0];
            Emails.Message m = Emails.reminder((String) r[2], orderId, links.received(orderId), links.notReceived(orderId));
            mail.send((String) r[1], m.subject(), m.body());
        }
        LOG.info("J1: {} reminder emails (E2) sent", due.size());
        return due.size();
    }

    /**
     * DeliveryIssue.report() (FR-20 step 4): records "Chưa thấy hàng đâu" as an OPEN issue when the order is still being
     * delivered and has no issue yet. The order stays SHIPPING and is not completed automatically meanwhile.
     */
    public ReportResult reportNotReceived(int orderId) {
        LocalDateTime now = LocalDateTime.now(clock);
        ReportResult result = JpaUtil.inTransaction(em -> {
            Order o = new OrderDao(em).lock(orderId).orElseThrow(() -> new BusinessException(OrderQueries.MSG_NOT_FOUND));
            if (o.getStatus() != OrderStatus.SHIPPING) {
                return ReportResult.NOT_SHIPPING;
            }
            boolean exists = em.createQuery("select count(i) from DeliveryIssue i where i.order.id = :o", Long.class)
                    .setParameter("o", orderId).getSingleResult() > 0;
            if (exists) {
                return ReportResult.ALREADY_REPORTED;
            }
            em.persist(new DeliveryIssue(o, now));
            return ReportResult.CREATED;
        });
        if (result == ReportResult.CREATED) {
            LOG.info("Delivery issue reported: orderId={}", orderId);
        }
        return result;
    }

    /** FR-42 step 3, FR-21 step 1: open issues of orders still being delivered, oldest first. */
    public List<IssueRow> openIssues() {
        return JpaUtil.read(em -> em.createQuery("select i from DeliveryIssue i join fetch i.order o join fetch o.customer"
                        + " where i.status = :open and o.status = :s order by i.reportedAt", DeliveryIssue.class)
                .setParameter("open", DeliveryIssueStatus.OPEN).setParameter("s", OrderStatus.SHIPPING)
                .getResultList().stream().map(i -> {
                    Order o = i.getOrder();
                    List<IssueRow.Line> lines = o.getDetails().stream().map(d -> new IssueRow.Line(d.getProduct().getName(),
                            d.getProduct().getColor(), d.getProduct().getSize(), d.getQuantity(), d.getProduct().getStockQuantity()))
                            .toList();
                    boolean reship = lines.stream().allMatch(l -> l.stockQuantity() >= l.quantity());
                    return new IssueRow(i.getId(), o.getId(), o.getCustomer().getId(), o.getCustomer().getFullName(),
                            o.getCustomer().getPhone(), o.getCustomer().getEmail(), i.getReportedAt(), o.getShippedAt(), lines, reship);
                }).toList());
    }

    /**
     * FR-21: staff handle an open issue. Reship (DeliveryIssue.reship()) takes the stock once more for every line, only
     * when all lines have enough (Q21, Q54), without a new shipping fee; resolved (DeliveryIssue.resolve()) only records
     * that the carrier was contacted. Either way the issue becomes RESOLVED, the order stays SHIPPING and the 7 days
     * before automatic completion start again from resolvedAt. The reason and solution typed by the employee go into the
     * email (E3 / E4) and are not stored (Q23, Q54).
     */
    public void resolve(int issueId, int employeeId, boolean reship, String reason, String solution) {
        Map<String, String> errors = new LinkedHashMap<>();
        String why = text(reason, "reason", "lý do", errors);
        String how = text(solution, "solution", "cách giải quyết", errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Object[] done = JpaUtil.inTransaction(em -> {
            DeliveryIssue issue = em.find(DeliveryIssue.class, issueId, LockModeType.PESSIMISTIC_WRITE);
            if (issue == null || issue.getStatus() != DeliveryIssueStatus.OPEN || issue.getOrder().getStatus() != OrderStatus.SHIPPING) {
                throw new BusinessException("Sự cố đã được xử lý hoặc đơn hàng đã thay đổi, vui lòng tải lại");
            }
            Order o = issue.getOrder();
            if (reship) {
                ProductDao products = new ProductDao(em);
                for (OrderDetail d : o.getDetails()) {
                    if (!products.decreaseStock(d.getProduct().getId(), d.getQuantity())) {
                        throw new BusinessException("Không đủ hàng để giao lại \"" + d.getProduct().getName() + "\" ("
                                + d.getProduct().getColor() + ", " + d.getProduct().getSize()
                                + "). Hãy chọn \"Đã giải quyết\" hoặc hủy đơn do giao không thành công.");
                    }
                }
            }
            issue.setStatus(DeliveryIssueStatus.RESOLVED);
            issue.setResolution(reship ? DeliveryResolution.RESHIP : DeliveryResolution.RESOLVED);
            issue.setResolvedBy(em.getReference(Employee.class, employeeId));
            issue.setResolvedAt(now);
            return new Object[] {o.getId(), o.getCustomer().getEmail(), o.getCustomer().getFullName()};
        });
        Emails.Message m = Emails.deliveryIssueHandled((String) done[2], (Integer) done[0], reship, why, how);
        mail.send((String) done[1], m.subject(), m.body());
        LOG.info("Delivery issue resolved: issueId={}, orderId={}, reship={}", issueId, done[0], reship);
    }

    private static String text(String raw, String field, String label, Map<String, String> errors) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            errors.put(field, "Vui lòng nhập " + label);
        } else if (value.length() > TEXT_MAX) {
            errors.put(field, "Tối đa " + TEXT_MAX + " ký tự");
        }
        return value;
    }
}
