package com.clothingshop.service;

import com.clothingshop.dao.DeliveryIssueDao;
import com.clothingshop.dao.OrderDao;
import com.clothingshop.dao.ReturnRequestDao;
import com.clothingshop.dao.ReviewDao;
import com.clothingshop.dto.OrderDetailView;
import com.clothingshop.dto.OrderRow;
import com.clothingshop.entity.Customer;
import com.clothingshop.entity.DeliveryIssue;
import com.clothingshop.entity.Order;
import com.clothingshop.entity.OrderDetail;
import com.clothingshop.entity.OrderStatus;
import com.clothingshop.entity.Payment;
import com.clothingshop.entity.PaymentStatus;
import com.clothingshop.entity.Product;
import com.clothingshop.entity.ReturnRequest;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.util.JpaUtil;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reading orders: the customer's history and order page (FR-16), the staff order list and order page. Orders waiting
 * for a VNPAY payment are never shown (BR-26).
 */
public class OrderQueries {

    /** BR-11: a return can be requested within 15 days of delivery. */
    static final Duration RETURN_WINDOW = Duration.ofDays(15);
    static final String MSG_NOT_FOUND = "Đơn hàng không tồn tại";
    private static final int STAFF_LIST_LIMIT = 300;

    private final Clock clock;

    public OrderQueries(Clock clock) {
        this.clock = clock;
    }

    /** FR-16 step 1: the customer's orders, newest first. */
    public List<OrderRow> forCustomer(int customerId) {
        return JpaUtil.read(em -> new OrderDao(em).findVisibleByCustomer(customerId));
    }

    /** FR-16 steps 2 – 5: one of the customer's own orders, with the buttons that apply now. */
    public OrderDetailView forCustomer(int customerId, int orderId) {
        return JpaUtil.read(em -> {
            Order o = new OrderDao(em).findById(orderId).orElseThrow(() -> new BusinessException(MSG_NOT_FOUND));
            if (o.getCustomer().getId() != customerId || !new OrderDao(em).isVisible(orderId)) {
                throw new BusinessException(MSG_NOT_FOUND);
            }
            return build(em, o, true);
        });
    }

    /** Orders for staff, newest first, optionally of one status. */
    public List<OrderRow> forStaff(OrderStatus status) {
        return JpaUtil.read(em -> new OrderDao(em).findVisible(status, STAFF_LIST_LIMIT));
    }

    /** One order for staff, with customer contact, issue and return request. */
    public OrderDetailView forStaff(int orderId) {
        return JpaUtil.read(em -> {
            OrderDao orders = new OrderDao(em);
            Order o = orders.findById(orderId).filter(x -> orders.isVisible(orderId))
                    .orElseThrow(() -> new BusinessException(MSG_NOT_FOUND));
            return build(em, o, false);
        });
    }

    private OrderDetailView build(EntityManager em, Order o, boolean forCustomer) {
        LocalDateTime now = LocalDateTime.now(clock);
        Payment p = new OrderDao(em).paymentOf(o.getId());
        Customer c = o.getCustomer();
        DeliveryIssue issue = new DeliveryIssueDao(em).findByOrder(o.getId()).orElse(null);
        ReturnRequest ret = new ReturnRequestDao(em).findByOrder(o.getId()).orElse(null);

        // Reviews of this customer by product (rows sharing a name): one review per product (BR-14, Q44)
        Map<String, Integer> reviews = new HashMap<>();
        if (forCustomer) {
            for (Object[] r : new ReviewDao(em).findIdAndProductNameByCustomer(c.getId())) {
                reviews.put(((String) r[1]).toLowerCase(Locale.ROOT), (Integer) r[0]);
            }
        }
        boolean completed = o.getStatus() == OrderStatus.COMPLETED;
        BigDecimal productAmount = BigDecimal.ZERO;
        List<OrderDetailView.Line> lines = new ArrayList<>();
        for (OrderDetail d : o.getDetails()) {
            Product product = d.getProduct();
            Integer reviewId = reviews.get(product.getName().toLowerCase(Locale.ROOT));
            productAmount = productAmount.add(d.getSubtotal());
            lines.add(new OrderDetailView.Line(product.getId(), product.getName(), product.getColor(), product.getSize(),
                    product.getImgUrl(), d.getQuantity(),
                    d.getSubtotal().divide(BigDecimal.valueOf(d.getQuantity()), 0, RoundingMode.HALF_UP),
                    d.getSubtotal().setScale(0, RoundingMode.HALF_UP), product.getStockQuantity(),
                    forCustomer && completed && reviewId == null, reviewId));
        }
        BigDecimal discount = productAmount.add(o.getShippingFee()).subtract(o.getTotalAmount()).setScale(0, RoundingMode.HALF_UP);
        boolean canRequestReturn = completed && p.getStatus() == PaymentStatus.PAID && ret == null
                && o.getDeliveredAt() != null && !now.isAfter(o.getDeliveredAt().plus(RETURN_WINDOW));
        OrderStatus s = o.getStatus();
        return new OrderDetailView(o.getId(), c.getId(), c.getFullName(), c.getPhone(), c.getEmail(), o.getOrderDate(),
                o.getAddress(), o.getNote(), o.getVoucher() == null ? null : o.getVoucher().getCode(), s, o.getShippedAt(),
                o.getDeliveredAt(), p.getMethod(), p.getStatus(), p.getPaidAt(), p.getTransactionNo(), List.copyOf(lines),
                productAmount.setScale(0, RoundingMode.HALF_UP), discount, o.getShippingFee().setScale(0, RoundingMode.HALF_UP),
                o.getTotalAmount().setScale(0, RoundingMode.HALF_UP),
                issue == null ? null : new OrderDetailView.Issue(issue.getId(), issue.getReportedAt(), issue.getStatus(),
                        issue.getResolution(), issue.getResolvedBy() == null ? null : issue.getResolvedBy().getFullName(),
                        issue.getResolvedAt()),
                ret == null ? null : new OrderDetailView.Return(ret.getId(), ret.getRequestedAt(), ret.getReason(), ret.getStatus(),
                        ret.getHandledBy() == null ? null : ret.getHandledBy().getFullName(), ret.getHandledAt()),
                s == OrderStatus.PENDING || s == OrderStatus.CONFIRMED || s == OrderStatus.SHIPPING,
                s == OrderStatus.SHIPPING, canRequestReturn);
    }
}
