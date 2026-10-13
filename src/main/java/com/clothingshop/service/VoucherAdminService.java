package com.clothingshop.service;

import com.clothingshop.dao.VoucherDao;
import com.clothingshop.dto.VoucherRow;
import com.clothingshop.dto.VoucherView;
import com.clothingshop.entity.DiscountType;
import com.clothingshop.entity.EligibleCustomer;
import com.clothingshop.entity.Voucher;
import com.clothingshop.entity.VoucherPolicy;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.util.JpaUtil;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manual vouchers, manager only (FR-35, FR-36, BR-22): created after a confirmation with every value (Q27), editable
 * until their start date (Q60), never deleted (Q56); the code never changes (Q63). Automatic vouchers are issued by
 * {@link AutoVoucherService} and only listed here.
 */
public class VoucherAdminService {

    private static final Logger LOG = LoggerFactory.getLogger(VoucherAdminService.class);

    static final Pattern CODE = Pattern.compile("^[A-Z0-9-]{1,50}$");
    static final Pattern HOLIDAY_CODE = Pattern.compile("^(TET|GIOTO|GPMN|QK)[0-9]{2}$");
    static final Pattern MONTHLY_CODE = Pattern.compile("^(CASUAL|REGULAR|LOYAL)-(0[1-9]|1[0-2])[0-9]{4}$");
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);
    private static final BigDecimal MONEY_MAX = new BigDecimal("9999999999");

    /** Values of the voucher form, exactly as submitted. */
    public record Form(String code, String discountType, String discountValue, String quantity, String eligibleCustomer,
                       String minOrderValue, String maxDiscount, String startDate, String endDate) {
    }

    /** Checked values, shown in the confirmation dialog. */
    public record Values(String code, DiscountType discountType, BigDecimal discountValue, int quantity,
                         EligibleCustomer eligibleCustomer, BigDecimal minOrderValue, BigDecimal maxDiscount,
                         LocalDateTime startDate, LocalDateTime endDate) {
    }

    private final Clock clock;

    public VoucherAdminService(Clock clock) {
        this.clock = clock;
    }

    /** Monthly and holiday vouchers are recognised by their code (FR-36 step 2). */
    public static boolean isAutoIssued(String code) {
        return HOLIDAY_CODE.matcher(code).matches() || MONTHLY_CODE.matcher(code).matches();
    }

    /**
     * FR-36 step 1 (Voucher.getIssuedVouchers()): vouchers with policy, uses left and state, filtered by code, eligible
     * customers and state (UPCOMING, ACTIVE, USED_UP, EXPIRED); newest start first.
     */
    public List<VoucherRow> list(String codeQuery, EligibleCustomer eligible, String state) {
        LocalDateTime now = LocalDateTime.now(clock);
        String q = codeQuery == null ? "" : codeQuery.trim().toUpperCase(Locale.ROOT);
        return JpaUtil.read(em -> new VoucherDao(em).findAll().stream()
                .filter(v -> v.getCode().contains(q))
                .filter(v -> eligible == null || v.getPolicy().getEligibleCustomer() == eligible)
                .map(v -> row(v, now))
                .filter(r -> state == null || state.isBlank() || stateKey(r.voucher().startDate(), r.voucher().endDate(),
                        r.voucher().quantity(), now).equals(state))
                .toList());
    }

    public VoucherRow get(String code) {
        LocalDateTime now = LocalDateTime.now(clock);
        return JpaUtil.read(em -> {
            Voucher v = new VoucherDao(em).findByCode(VoucherService.normalizeCode(code)).orElse(null);
            if (v == null) {
                throw new BusinessException("Voucher không tồn tại");
            }
            return row(v, now);
        });
    }

    /**
     * FR-35 step 2: letters without accents, digits and '-', at most 50, stored in capitals; new code not used and not in
     * the form of holiday or monthly codes (Q27, Q55); PERCENT 1 – 100, FIXED_AMOUNT above 0; amounts multiples of 1.000
     * (BR-21); maxDiscount only for PERCENT; start before end; minimum ≥ 0; quantity ≥ 0.
     *
     * @param editedCode the voucher being edited (its code cannot change), or {@code null} when creating
     */
    public Values validate(Form form, String editedCode) {
        Map<String, String> errors = new LinkedHashMap<>();
        String code = editedCode != null ? editedCode : VoucherService.normalizeCode(form.code());
        if (editedCode == null) {
            if (!CODE.matcher(code).matches()) {
                errors.put("code", "Mã gồm chữ cái không dấu, chữ số, dấu gạch ngang, tối đa 50 ký tự");
            } else if (HOLIDAY_CODE.matcher(code).matches() || MONTHLY_CODE.matcher(code).matches()) {
                errors.put("code", "Mã trùng dạng mã voucher ngày lễ hoặc hằng tháng do hệ thống tự phát hành");
            } else if (JpaUtil.read(em -> new VoucherDao(em).findByCode(code).isPresent())) {
                errors.put("code", "Mã voucher đã tồn tại");
            }
        }
        DiscountType type = parseEnum(DiscountType.class, form.discountType(), "discountType", "loại giảm giá", errors);
        BigDecimal value = discountValue(type, form.discountValue(), errors);
        int quantity = 0;
        try {
            quantity = Integer.parseInt(form.quantity() == null ? "" : form.quantity().trim());
            if (quantity < 0) {
                errors.put("quantity", "Số lượng không được âm");
            }
        } catch (NumberFormatException e) {
            errors.put("quantity", "Số lượng phải là số nguyên");
        }
        EligibleCustomer eligible = parseEnum(EligibleCustomer.class, form.eligibleCustomer(), "eligibleCustomer",
                "đối tượng khách hàng", errors);
        BigDecimal min = money(form.minOrderValue(), "minOrderValue", true, errors);
        BigDecimal max = null;
        if (form.maxDiscount() != null && !form.maxDiscount().isBlank()) {
            if (type == DiscountType.FIXED_AMOUNT) {
                errors.put("maxDiscount", "Voucher giảm số tiền cố định không có mức giảm tối đa");
            } else {
                max = money(form.maxDiscount(), "maxDiscount", false, errors);
            }
        }
        LocalDateTime start = dateTime(form.startDate(), "startDate", errors);
        LocalDateTime end = dateTime(form.endDate(), "endDate", errors);
        if (start != null && end != null && !start.isBefore(end)) {
            errors.put("endDate", "Ngày kết thúc phải sau ngày bắt đầu");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return new Values(code, type, value, quantity, eligible, min, max, start, end);
    }

    /** FR-35 step 4 (Voucher.createManual()): after the confirmation, Voucher and VoucherPolicy in one transaction. */
    public void create(Form form) {
        Values v = validate(form, null);
        JpaUtil.inTransaction(em -> {
            if (new VoucherDao(em).findByCode(v.code()).isPresent()) {
                throw new ValidationException(Map.of("code", "Mã voucher đã tồn tại"));
            }
            persist(em, v.code(), v.discountType(), v.discountValue(), v.quantity(), v.eligibleCustomer(), v.minOrderValue(),
                    v.maxDiscount(), v.startDate(), v.endDate());
            return null;
        });
        LOG.info("Manual voucher created: code={}", v.code());
    }

    /**
     * FR-36 steps 2 – 4 (Voucher.isEditable(), Voucher.update()): only a manual voucher whose start date has not come,
     * checked again when saving (BR-22); the code stays.
     */
    public void update(String code, Form form) {
        String normalized = VoucherService.normalizeCode(code);
        Values v = validate(form, normalized);
        LocalDateTime now = LocalDateTime.now(clock);
        JpaUtil.inTransaction(em -> {
            Voucher voucher = new VoucherDao(em).lock(normalized).orElse(null);
            if (voucher == null) {
                throw new BusinessException("Voucher không tồn tại");
            }
            if (isAutoIssued(normalized)) {
                throw new BusinessException("Voucher do hệ thống tự phát hành không sửa trực tiếp; hãy sửa thiết lập (Q64)");
            }
            if (!now.isBefore(voucher.getPolicy().getStartDate())) {
                throw new BusinessException("Voucher đã bắt đầu hiệu lực, không sửa được (BR-22)");
            }
            voucher.setDiscountType(v.discountType());
            voucher.setDiscountValue(v.discountValue());
            voucher.setQuantity(v.quantity());
            VoucherPolicy p = voucher.getPolicy();
            p.setEligibleCustomer(v.eligibleCustomer());
            p.setMinOrderValue(v.minOrderValue());
            p.setMaxDiscount(v.maxDiscount());
            p.setStartDate(v.startDate());
            p.setEndDate(v.endDate());
            return null;
        });
        LOG.info("Manual voucher updated: code={}", normalized);
    }

    /** Creates a voucher with its policy in the caller's transaction (manual, monthly or holiday). */
    static Voucher persist(EntityManager em, String code, DiscountType type, BigDecimal value, int quantity,
                           EligibleCustomer eligible, BigDecimal min, BigDecimal max, LocalDateTime start, LocalDateTime end) {
        Voucher v = new Voucher(code);
        v.setDiscountType(type);
        v.setDiscountValue(value);
        v.setQuantity(quantity);
        VoucherPolicy p = new VoucherPolicy();
        p.setEligibleCustomer(eligible);
        p.setMinOrderValue(min);
        p.setMaxDiscount(max);
        p.setStartDate(start);
        p.setEndDate(end);
        v.setPolicy(p);
        new VoucherDao(em).persist(v);
        return v;
    }

    /** BR-07: PERCENT 1 – 100 (two decimals at most); FIXED_AMOUNT above 0 and a multiple of 1.000 (BR-21). */
    static BigDecimal discountValue(DiscountType type, String raw, Map<String, String> errors) {
        try {
            BigDecimal value = new BigDecimal(raw == null ? "" : raw.trim());
            if (type == DiscountType.PERCENT && (value.compareTo(BigDecimal.ONE) < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0
                    || value.stripTrailingZeros().scale() > 2)) {
                errors.put("discountValue", "Giảm theo phần trăm: từ 1 đến 100");
            } else if (type == DiscountType.FIXED_AMOUNT && (value.signum() <= 0 || value.compareTo(MONEY_MAX) > 0
                    || value.remainder(THOUSAND).signum() != 0)) {
                errors.put("discountValue", "Số tiền giảm phải lớn hơn 0 và là bội số của 1.000");
            }
            return value;
        } catch (NumberFormatException e) {
            errors.put("discountValue", "Giá trị giảm phải là số");
            return null;
        }
    }

    /** A whole amount, a multiple of 1.000 (BR-21); {@code zeroAllowed} for the minimum order value. */
    static BigDecimal money(String raw, String field, boolean zeroAllowed, Map<String, String> errors) {
        try {
            BigDecimal value = new BigDecimal(raw == null || raw.isBlank() ? (zeroAllowed ? "0" : "") : raw.trim());
            if (value.signum() < 0 || (!zeroAllowed && value.signum() == 0) || value.compareTo(MONEY_MAX) > 0) {
                errors.put(field, zeroAllowed ? "Số tiền không được âm" : "Số tiền phải lớn hơn 0");
            } else if (value.remainder(THOUSAND).signum() != 0) {
                errors.put(field, "Số tiền phải là bội số của 1.000");
            }
            return value;
        } catch (NumberFormatException e) {
            errors.put(field, "Số tiền không hợp lệ");
            return null;
        }
    }

    static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, String field, String label, Map<String, String> errors) {
        try {
            return Enum.valueOf(type, raw == null ? "" : raw.trim());
        } catch (IllegalArgumentException e) {
            errors.put(field, "Vui lòng chọn " + label);
            return null;
        }
    }

    private static LocalDateTime dateTime(String raw, String field, Map<String, String> errors) {
        try {
            return LocalDateTime.parse(raw == null ? "" : raw.trim());
        } catch (DateTimeParseException e) {
            errors.put(field, "Vui lòng chọn ngày giờ");
            return null;
        }
    }

    private static VoucherRow row(Voucher v, LocalDateTime now) {
        VoucherView view = VoucherService.view(v);
        String key = stateKey(view.startDate(), view.endDate(), view.quantity(), now);
        String state = switch (key) {
            case "UPCOMING" -> "Chưa đến hạn";
            case "EXPIRED" -> "Hết hạn";
            case "USED_UP" -> "Hết lượt";
            default -> "Đang hiệu lực";
        };
        boolean auto = isAutoIssued(v.getCode());
        return new VoucherRow(view, state, auto, !auto && now.isBefore(view.startDate()));
    }

    private static String stateKey(LocalDateTime start, LocalDateTime end, int quantity, LocalDateTime now) {
        if (now.isBefore(start)) {
            return "UPCOMING";
        }
        if (now.isAfter(end)) {
            return "EXPIRED";
        }
        return quantity <= 0 ? "USED_UP" : "ACTIVE";
    }
}
