package com.clothingshop.service;

import com.clothingshop.dto.VoucherView;
import com.clothingshop.entity.Customer;
import com.clothingshop.entity.CustomerLevel;
import com.clothingshop.entity.EligibleCustomer;
import com.clothingshop.entity.Voucher;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.util.JpaUtil;
import com.clothingshop.util.ViewFunctions;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Vouchers seen by customers: checking a code at checkout (FR-14) and the list of vouchers a customer can use (FR-39).
 * The manager's side (create, edit, automatic issuing) is in {@code VoucherAdminService}.
 */
public class VoucherService {

    static final String MSG_NOT_FOUND = "Mã voucher không tồn tại";
    static final String MSG_USED_UP = "Voucher đã hết lượt sử dụng";

    private final Clock clock;

    public VoucherService(Clock clock) {
        this.clock = clock;
    }

    /** Codes are stored in capitals and typed without regard to letter case (BR-22). */
    public static String normalizeCode(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * FR-14 steps 1 – 2: the voucher {@code code} when this customer can use it on an order whose product amount is
     * {@code productAmount}; otherwise a {@link BusinessException} that says which rule failed.
     */
    static Voucher checkUsable(EntityManager em, String code, CustomerLevel level, BigDecimal productAmount, LocalDateTime now) {
        Voucher voucher = em.find(Voucher.class, normalizeCode(code));
        if (voucher == null) {
            throw new BusinessException(MSG_NOT_FOUND);
        }
        if (voucher.getQuantity() <= 0) {
            throw new BusinessException(MSG_USED_UP);
        }
        if (now.isBefore(voucher.getPolicy().getStartDate())) {
            throw new BusinessException("Voucher chưa đến thời gian áp dụng (từ "
                    + ViewFunctions.dateTime(voucher.getPolicy().getStartDate()) + ")");
        }
        if (now.isAfter(voucher.getPolicy().getEndDate())) {
            throw new BusinessException("Voucher đã hết hạn");
        }
        if (!voucher.getPolicy().isFor(level)) {
            throw new BusinessException("Voucher chỉ dành cho " + voucher.getPolicy().getEligibleCustomer().getLabel().toLowerCase(Locale.ROOT));
        }
        if (productAmount.compareTo(voucher.getPolicy().getMinOrderValue()) < 0) {
            throw new BusinessException("Tiền sản phẩm phải từ " + ViewFunctions.vnd(voucher.getPolicy().getMinOrderValue())
                    + " để dùng voucher này");
        }
        return voucher;
    }

    /** FR-39: vouchers with uses left, valid now, for every customer or for the customer's level (BR-06). */
    public List<VoucherView> usableBy(int customerId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return JpaUtil.read(em -> {
            CustomerLevel level = em.find(Customer.class, customerId).getCustomerLevel();
            return em.createQuery("select v from Voucher v where v.quantity > 0 and v.policy.startDate <= :now"
                            + " and v.policy.endDate >= :now and v.policy.eligibleCustomer in :eligible"
                            + " order by v.policy.endDate, v.code", Voucher.class)
                    .setParameter("now", now)
                    .setParameter("eligible", List.of(EligibleCustomer.ALL, EligibleCustomer.valueOf(level.name())))
                    .getResultList().stream().map(VoucherService::view).toList();
        });
    }

    static VoucherView view(Voucher v) {
        return new VoucherView(v.getCode(), v.getDiscountType(), v.getDiscountValue(), v.getQuantity(),
                v.getPolicy().getEligibleCustomer(), v.getPolicy().getMinOrderValue(), v.getPolicy().getMaxDiscount(),
                v.getPolicy().getStartDate(), v.getPolicy().getEndDate());
    }
}
