package com.clothingshop.service;

import com.clothingshop.dao.OrderDao;
import com.clothingshop.dto.CustomerDetail;
import com.clothingshop.dto.CustomerRow;
import com.clothingshop.entity.Customer;
import com.clothingshop.entity.CustomerLevel;
import com.clothingshop.entity.PaymentStatus;
import com.clothingshop.entity.ReturnRequest;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ConflictException;
import com.clothingshop.util.JpaUtil;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customers seen by the shop: membership level (FR-27, FR-28) and lookup by staff (FR-42).
 * Customer accounts are never locked (Q10).
 */
public class CustomerService {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerService.class);

    /** BR-01: thresholds "from … upwards", shipping fee included (Q32). */
    static final BigDecimal REGULAR_FROM = new BigDecimal("2000000");
    static final BigDecimal LOYAL_FROM = new BigDecimal("10000000");
    private static final int SEARCH_LIMIT = 100;

    /**
     * FR-27: re-evaluates the level after a payment of the customer became PAID or REFUND, in the caller's transaction.
     * A higher level is applied at once (and a system upgrade to LOYAL turns autoDowngrade back on, Q31); a lower level
     * only when autoDowngrade is on (BR-02).
     */
    public static void evaluateLevel(EntityManager em, int customerId) {
        Customer customer = em.find(Customer.class, customerId);
        CustomerLevel current = customer.getCustomerLevel();
        CustomerLevel target = levelFor(paidTotal(em, customerId));
        if (target.compareTo(current) > 0) {
            customer.setCustomerLevel(target);
            if (target == CustomerLevel.LOYAL) {
                customer.setAutoDowngrade(true);
            }
        } else if (target.compareTo(current) < 0 && customer.isAutoDowngrade()) {
            customer.setCustomerLevel(target);
        }
        if (customer.getCustomerLevel() != current) {
            LOG.info("Customer level changed: customerId={}, {} -> {}", customerId, current, customer.getCustomerLevel());
        }
    }

    /** Customer.getPaidTotal(): Σ Order.totalAmount of the customer's orders whose payment is PAID (Q32). */
    public static BigDecimal paidTotal(EntityManager em, int customerId) {
        return em.createQuery("select coalesce(sum(o.totalAmount), 0) from Payment p join p.order o"
                        + " where o.customer.id = :c and p.status = :paid", BigDecimal.class)
                .setParameter("c", customerId).setParameter("paid", PaymentStatus.PAID).getSingleResult();
    }

    static CustomerLevel levelFor(BigDecimal paidTotal) {
        if (paidTotal.compareTo(LOYAL_FROM) >= 0) {
            return CustomerLevel.LOYAL;
        }
        return paidTotal.compareTo(REGULAR_FROM) >= 0 ? CustomerLevel.REGULAR : CustomerLevel.CASUAL;
    }

    /** FR-28 step 1: the manager raises the level (never lowers it, BR-02) and autoDowngrade becomes FALSE. */
    public void upgradeLevel(int customerId, CustomerLevel newLevel, Long version) {
        JpaUtil.inTransaction(em -> {
            Customer customer = find(em, customerId);
            ConflictException.check(customer.getVersion(), version);
            if (newLevel == null || newLevel.compareTo(customer.getCustomerLevel()) <= 0) {
                throw new BusinessException("Chỉ được nâng lên hạng cao hơn hạng hiện tại");
            }
            customer.setCustomerLevel(newLevel);
            customer.setAutoDowngrade(false);
            LOG.info("Customer level raised by the manager: customerId={}, level={}", customerId, newLevel);
            return null;
        });
    }

    /** FR-28 step 3: turning automatic downgrade back on re-evaluates the level at once (Q53). */
    public void setAutoDowngrade(int customerId, boolean value, Long version) {
        JpaUtil.inTransaction(em -> {
            Customer customer = find(em, customerId);
            ConflictException.check(customer.getVersion(), version);
            customer.setAutoDowngrade(value);
            if (value) {
                evaluateLevel(em, customerId);
            }
            return null;
        });
    }

    /** FR-42 step 1: customers whose name, email or phone contains {@code query} (all customers when empty). */
    public List<CustomerRow> search(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return JpaUtil.read(em -> em.createQuery("select c from Customer c where lower(c.fullName) like :q escape '!'"
                        + " or lower(c.email) like :q escape '!' or c.phone like :q escape '!' order by c.createdAt desc", Customer.class)
                .setParameter("q", "%" + q.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%")
                .setMaxResults(SEARCH_LIMIT)
                .getResultList().stream()
                .map(c -> new CustomerRow(c.getId(), c.getUserName(), c.getFullName(), c.getEmail(), c.getPhone(),
                        c.getCustomerLevel(), c.getCreatedAt()))
                .toList());
    }

    /** FR-42 step 2: information, level, paid total, orders and return requests of one customer. */
    public CustomerDetail detail(int customerId) {
        return JpaUtil.read(em -> {
            Customer c = find(em, customerId);
            List<ReturnRequest> returns = em.createQuery("select r from ReturnRequest r join fetch r.order o"
                    + " where o.customer.id = :c order by r.requestedAt desc", ReturnRequest.class)
                    .setParameter("c", customerId).getResultList();
            return new CustomerDetail(c.getId(), c.getUserName(), c.getFullName(), c.getEmail(), c.getPhone(), c.getGender(),
                    c.getDob(), c.getCreatedAt(), c.getCustomerLevel(), c.isAutoDowngrade(), c.getVersion(),
                    paidTotal(em, customerId), new OrderDao(em).findVisibleByCustomer(customerId),
                    returns.stream().map(r -> new CustomerDetail.ReturnItem(r.getId(), r.getOrder().getId(), r.getRequestedAt(),
                            r.getReason(), r.getStatus(), r.getHandledAt())).toList());
        });
    }

    private static Customer find(EntityManager em, int customerId) {
        Customer customer = em.find(Customer.class, customerId);
        if (customer == null) {
            throw new BusinessException("Khách hàng không tồn tại");
        }
        return customer;
    }
}
