package com.clothingshop.service;

import com.clothingshop.dao.ProductDao;
import com.clothingshop.dto.ProductAdminView;
import com.clothingshop.entity.Discount;
import com.clothingshop.entity.Product;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.util.JpaUtil;
import com.clothingshop.util.ViewFunctions;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discount campaigns of a product, manager only (FR-34, BR-08, Q68, SD14). A campaign is one Discount row per variant
 * row, all with the same percentage and period; a product never has two campaigns at the same time, so a campaign is
 * identified by product + start date. A campaign can be edited until it starts; none is ever deleted (Q13).
 */
public class DiscountAdminService {

    private static final Logger LOG = LoggerFactory.getLogger(DiscountAdminService.class);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** Values of the campaign form, already parsed. */
    public record Campaign(BigDecimal discountValue, LocalDateTime startDate, LocalDateTime endDate) {
    }

    private final Clock clock;

    public DiscountAdminService(Clock clock) {
        this.clock = clock;
    }

    /**
     * FR-34 step 2: 0 < value ≤ 100 (two decimals at most), start before end, no overlap with another campaign of the
     * product (Discount.overlaps()); {@code editedStart} is the campaign being edited, left out of the overlap check.
     * Used before showing the confirmation and again when saving.
     */
    public Campaign validate(int rowId, String value, String start, String end, LocalDateTime editedStart) {
        Map<String, String> errors = new LinkedHashMap<>();
        BigDecimal percent = null;
        try {
            percent = new BigDecimal(value == null ? "" : value.trim());
            if (percent.signum() <= 0 || percent.compareTo(HUNDRED) > 0 || percent.stripTrailingZeros().scale() > 2) {
                errors.put("discountValue", "Mức giảm phải lớn hơn 0 và không quá 100 (%), tối đa 2 chữ số thập phân");
            }
        } catch (NumberFormatException e) {
            errors.put("discountValue", "Mức giảm phải là số");
        }
        LocalDateTime from = parse(start, "startDate", errors);
        LocalDateTime to = parse(end, "endDate", errors);
        if (from != null && to != null && !from.isBefore(to)) {
            errors.put("endDate", "Ngày kết thúc phải sau ngày bắt đầu");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        Campaign campaign = new Campaign(percent, from, to);
        JpaUtil.read(em -> {
            requireNoOverlap(campaigns(em, rows(em, rowId), LocalDateTime.now(clock)), campaign, editedStart);
            return null;
        });
        return campaign;
    }

    /** Discount.overlaps(): the periods share at least one moment (both ends included). */
    private static void requireNoOverlap(List<ProductAdminView.Campaign> existing, Campaign c, LocalDateTime editedStart) {
        for (ProductAdminView.Campaign other : existing) {
            boolean self = editedStart != null && other.startDate().equals(editedStart);
            if (!self && !c.startDate().isAfter(other.endDate()) && !other.startDate().isAfter(c.endDate())) {
                throw new ValidationException(Map.of("startDate", "Trùng thời gian với đợt giảm giá "
                        + other.discountValue().stripTrailingZeros().toPlainString() + "% từ "
                        + ViewFunctions.dateTime(other.startDate()) + " đến "
                        + ViewFunctions.dateTime(other.endDate())));
            }
        }
    }

    /** FR-34 step 4 (Product.addDiscount()): after the confirmation, one Discount per row of the product, in one transaction. */
    public void create(int rowId, String value, String start, String end) {
        Campaign c = validate(rowId, value, start, end, null);
        LocalDateTime now = LocalDateTime.now(clock);
        JpaUtil.inTransaction(em -> {
            List<Product> rows = new ProductDao(em).lockByName(rows(em, rowId).get(0).getName());
            requireNoOverlap(campaigns(em, rows, now), c, null);   // again, now that the product is locked
            for (Product p : rows) {
                em.persist(new Discount(p, c.discountValue(), c.startDate(), c.endDate()));
            }
            return null;
        });
        LOG.info("Discount campaign created: rowId={}, {}% from {} to {}", rowId, c.discountValue(), c.startDate(), c.endDate());
    }

    /**
     * FR-34 step 6 (Discount.isEditable(), Discount.update()): only a campaign that has not started; checked again when
     * saving, so a form opened before the start but saved after it is refused.
     */
    public void update(int rowId, LocalDateTime campaignStart, String value, String start, String end) {
        Campaign c = validate(rowId, value, start, end, campaignStart);
        LocalDateTime now = LocalDateTime.now(clock);
        JpaUtil.inTransaction(em -> {
            List<Product> rows = new ProductDao(em).lockByName(rows(em, rowId).get(0).getName());
            List<Discount> campaign = em.createQuery("select d from Discount d where d.product in :rows and d.startDate = :start",
                    Discount.class).setParameter("rows", rows).setParameter("start", campaignStart).getResultList();
            if (campaign.isEmpty()) {
                throw new BusinessException("Đợt giảm giá không tồn tại");
            }
            if (!now.isBefore(campaignStart)) {
                throw new BusinessException("Đợt giảm giá đã bắt đầu, không sửa được (Q68)");
            }
            requireNoOverlap(campaigns(em, rows, now), c, campaignStart);
            for (Discount d : campaign) {
                d.setDiscountValue(c.discountValue());
                d.setStartDate(c.startDate());
                d.setEndDate(c.endDate());
            }
            return null;
        });
        LOG.info("Discount campaign updated: rowId={}, start {} -> {}", rowId, campaignStart, c.startDate());
    }

    /** FR-34 step 5: campaigns of the product with their state, newest first. */
    static List<ProductAdminView.Campaign> campaigns(EntityManager em, List<Product> rows, LocalDateTime now) {
        Map<LocalDateTime, Discount> byStart = new LinkedHashMap<>();
        for (Discount d : discountsOf(em, rows)) {
            byStart.putIfAbsent(d.getStartDate(), d);
        }
        List<ProductAdminView.Campaign> result = new ArrayList<>();
        for (Discount d : byStart.values()) {
            String state = now.isBefore(d.getStartDate()) ? "Chưa đến ngày bắt đầu"
                    : now.isAfter(d.getEndDate()) ? "Đã kết thúc" : "Đang hiệu lực";
            result.add(new ProductAdminView.Campaign(d.getDiscountValue(), d.getStartDate(), d.getEndDate(), state,
                    now.isBefore(d.getStartDate())));
        }
        result.sort(Comparator.comparing(ProductAdminView.Campaign::startDate).reversed());
        return result;
    }

    /** One Discount per campaign that has not ended, used as a template for a new variant row (FR-31 step 2). */
    static List<Discount> openCampaignTemplates(EntityManager em, List<Product> rows, LocalDateTime now) {
        Map<LocalDateTime, Discount> byStart = new LinkedHashMap<>();
        for (Discount d : discountsOf(em, rows)) {
            if (!d.getEndDate().isBefore(now)) {
                byStart.putIfAbsent(d.getStartDate(), d);
            }
        }
        return List.copyOf(byStart.values());
    }

    private static List<Discount> discountsOf(EntityManager em, List<Product> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        return em.createQuery("select d from Discount d where d.product in :rows order by d.startDate desc, d.id", Discount.class)
                .setParameter("rows", rows).getResultList();
    }

    private static List<Product> rows(EntityManager em, int rowId) {
        Product row = em.find(Product.class, rowId);
        if (row == null) {
            throw new BusinessException("Mặt hàng không tồn tại");
        }
        return new ProductDao(em).findByName(row.getName());
    }

    /** A value of an HTML datetime-local input (yyyy-MM-ddTHH:mm). */
    private static LocalDateTime parse(String value, String field, Map<String, String> errors) {
        try {
            return LocalDateTime.parse(value == null ? "" : value.trim());
        } catch (DateTimeParseException e) {
            errors.put(field, "Vui lòng chọn ngày giờ");
            return null;
        }
    }
}
