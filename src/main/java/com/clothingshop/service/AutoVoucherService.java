package com.clothingshop.service;

import com.clothingshop.dao.VoucherDao;
import com.clothingshop.entity.CustomerLevel;
import com.clothingshop.entity.DiscountType;
import com.clothingshop.entity.EligibleCustomer;
import com.clothingshop.entity.Voucher;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.util.JpaUtil;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Automatic vouchers (FR-37, FR-38, SD11): the manager edits the settings (JSON file, section 3.5); task J5 issues the
 * monthly voucher of each level turned on and J6 creates holiday vouchers. Both check before creating, so running again
 * or catching up never creates twice. Issued vouchers are never edited; new settings apply from the next issue (Q64).
 */
public class AutoVoucherService {

    private static final Logger LOG = LoggerFactory.getLogger(AutoVoucherService.class);

    /** Values of a settings form (a level or a campaign), exactly as submitted. */
    public record Form(boolean enabled, String discountType, String discountValue, String quantity, String minOrderValue,
                       String maxDiscount, String eligibleCustomer, String startDay, String endDay, String startOffsetDays,
                       String durationDays) {
    }

    private final VoucherSettingsStore store;
    private final Clock clock;

    public AutoVoucherService(VoucherSettingsStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    public VoucherSettings settings() {
        return store.get();
    }

    /** Start and end of a campaign in the year of its next occurrence (today's if it is still running), for the settings page. */
    public Map<Holiday, LocalDateTime[]> nextOccurrences() {
        LocalDateTime now = LocalDateTime.now(clock);
        Map<Holiday, LocalDateTime[]> result = new EnumMap<>(Holiday.class);
        store.get().holidays().forEach((h, c) -> {
            for (int year = now.getYear(); year <= now.getYear() + 1; year++) {
                LocalDateTime start = h.dateIn(year).plusDays(c.startOffsetDays()).atStartOfDay();
                LocalDateTime end = start.plusDays(c.durationDays());
                if (end.isAfter(now)) {
                    result.put(h, new LocalDateTime[] {start, end});
                    break;
                }
            }
        });
        return result;
    }

    /**
     * FR-37 step 1 (VoucherSettings.save()): the settings of one level. Turning a level on records enabledAt = now (Q69);
     * a level cannot be on without type and value. Values are checked like a manual voucher (BR-07, BR-21).
     */
    public void saveMonthly(CustomerLevel level, Form form) {
        Map<String, String> errors = new LinkedHashMap<>();
        Common c = common(form, errors);
        int startDay = intIn(form.startDay(), 1, 31, "startDay", "Ngày bắt đầu trong tháng từ 1 đến 31", errors);
        Integer endDay = form.endDay() == null || form.endDay().isBlank() ? null
                : intIn(form.endDay(), 1, 31, "endDay", "Ngày kết thúc trong tháng từ 1 đến 31 hoặc để trống", errors);
        if (endDay != null && endDay < startDay) {
            errors.put("endDay", "Ngày kết thúc không được trước ngày bắt đầu");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        synchronized (store) {
            VoucherSettings s = store.get();
            VoucherSettings.Monthly old = s.monthly().get(level);
            Map<CustomerLevel, VoucherSettings.Monthly> monthly = new EnumMap<>(s.monthly());
            monthly.put(level, new VoucherSettings.Monthly(form.enabled(), enabledAt(form.enabled(), old.enabled(), old.enabledAt()),
                    c.type(), c.value(), c.min(), c.max(), c.quantity(), startDay, endDay));
            store.save(new VoucherSettings(monthly, s.holidays()));
        }
        LOG.info("Monthly voucher settings saved: level={}, enabled={}", level, form.enabled());
    }

    /**
     * FR-38 step 2: the settings of one campaign; no default type or value, so one cannot be turned on without them
     * (Q49); Tết never starts before the 1st day of the lunar year (Q49).
     */
    public void saveHoliday(Holiday holiday, Form form) {
        Map<String, String> errors = new LinkedHashMap<>();
        Common c = common(form, errors);
        EligibleCustomer eligible = VoucherAdminService.parseEnum(EligibleCustomer.class, form.eligibleCustomer(),
                "eligibleCustomer", "đối tượng khách hàng", errors);
        int offset = intIn(form.startOffsetDays(), holiday == Holiday.TET ? 0 : -60, 60, "startOffsetDays",
                holiday == Holiday.TET ? "Không bắt đầu trước mùng 1 Tết: từ 0 đến 60 ngày" : "Số ngày lệch từ −60 đến 60", errors);
        int duration = intIn(form.durationDays(), 1, 60, "durationDays", "Số ngày hiệu lực từ 1 đến 60", errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        synchronized (store) {
            VoucherSettings s = store.get();
            VoucherSettings.Campaign old = s.holidays().get(holiday);
            Map<Holiday, VoucherSettings.Campaign> holidays = new EnumMap<>(s.holidays());
            holidays.put(holiday, new VoucherSettings.Campaign(form.enabled(), enabledAt(form.enabled(), old.enabled(), old.enabledAt()),
                    c.type(), c.value(), c.quantity(), c.min(), c.max(), eligible, offset, duration));
            store.save(new VoucherSettings(s.monthly(), holidays));
        }
        LOG.info("Holiday voucher settings saved: campaign={}, enabled={}", holiday, form.enabled());
    }

    /**
     * Task J5 (FR-37 step 3, daily): for each level turned on before 00:00 on the 1st of this month (Q69), issues
     * {@code <LEVEL>-<MMYYYY>} if it does not exist yet and today is not past the end day of the settings.
     *
     * @return number of vouchers issued
     */
    public int issueMonthlyVouchers() {
        LocalDateTime now = LocalDateTime.now(clock);
        YearMonth month = YearMonth.from(now);
        LocalDateTime monthStart = month.atDay(1).atStartOfDay();
        int issued = 0;
        for (Map.Entry<CustomerLevel, VoucherSettings.Monthly> e : store.get().monthly().entrySet()) {
            VoucherSettings.Monthly m = e.getValue();
            if (!m.enabled() || m.discountType() == null || m.discountValue() == null
                    || m.enabledAt() == null || !m.enabledAt().isBefore(monthStart)) {
                continue;
            }
            int last = month.lengthOfMonth();
            int startDay = Math.min(m.startDay() == null ? 1 : m.startDay(), last);
            int endDay = m.endDay() == null ? last : Math.min(m.endDay(), last);
            if (now.getDayOfMonth() > endDay) {
                continue;
            }
            String code = e.getKey().name() + "-" + String.format("%02d%04d", month.getMonthValue(), month.getYear());
            boolean created = JpaUtil.inTransaction(em -> {
                if (new VoucherDao(em).findByCode(code).isPresent()) {
                    return false;
                }
                VoucherAdminService.persist(em, code, m.discountType(), m.discountValue(), m.quantity() == null ? 0 : m.quantity(),
                        EligibleCustomer.valueOf(e.getKey().name()), m.minOrderValue() == null ? BigDecimal.ZERO : m.minOrderValue(),
                        m.discountType() == DiscountType.PERCENT ? m.maxDiscount() : null,
                        month.atDay(startDay).atStartOfDay(), month.atDay(endDay).atTime(23, 59, 59));
                return true;
            });
            if (created) {
                issued++;
                LOG.info("J5: monthly voucher {} issued", code);
            }
        }
        LOG.info("J5: {} monthly vouchers issued", issued);
        return issued;
    }

    /**
     * Task J6 (FR-38 step 3, daily): for each campaign on, when today is within this year's period of the holiday
     * (computed from the settings), the campaign was turned on before the period started (Q69) and no voucher of the
     * campaign starts then yet, creates one with code prefix + next number (VoucherSettings.createHolidayVoucher()).
     *
     * @return number of vouchers created
     */
    public int createHolidayVouchers() {
        LocalDateTime now = LocalDateTime.now(clock);
        int created = 0;
        for (Map.Entry<Holiday, VoucherSettings.Campaign> e : store.get().holidays().entrySet()) {
            Holiday h = e.getKey();
            VoucherSettings.Campaign c = e.getValue();
            if (!c.enabled() || c.discountType() == null || c.discountValue() == null || c.enabledAt() == null) {
                continue;
            }
            for (int year = now.getYear() - 1; year <= now.getYear(); year++) {
                LocalDateTime start = h.dateIn(year).plusDays(c.startOffsetDays()).atStartOfDay();
                LocalDateTime end = start.plusDays(c.durationDays());
                if (now.isBefore(start) || !now.isBefore(end) || !c.enabledAt().isBefore(start)) {
                    continue;
                }
                String code = JpaUtil.inTransaction(em -> {
                    List<Voucher> previous = campaignVouchers(em, h);
                    if (previous.stream().anyMatch(v -> v.getPolicy().getStartDate().equals(start))) {
                        return null;
                    }
                    String next = nextHolidayCode(h, previous);
                    VoucherAdminService.persist(em, next, c.discountType(), c.discountValue(), c.quantity() == null ? 0 : c.quantity(),
                            c.eligibleCustomer() == null ? EligibleCustomer.ALL : c.eligibleCustomer(),
                            c.minOrderValue() == null ? BigDecimal.ZERO : c.minOrderValue(),
                            c.discountType() == DiscountType.PERCENT ? c.maxDiscount() : null, start, end);
                    return next;
                });
                if (code != null) {
                    created++;
                    LOG.info("J6: holiday voucher {} created for {} {}", code, h, year);
                }
            }
        }
        LOG.info("J6: {} holiday vouchers created", created);
        return created;
    }

    /** Vouchers of a campaign: codes prefix + 2 digits (manual codes cannot take that form, Q27). */
    private static List<Voucher> campaignVouchers(EntityManager em, Holiday h) {
        Pattern own = Pattern.compile("^" + h.name() + "[0-9]{2}$");
        return new VoucherDao(em).findByCodePrefix(h.name()).stream()
                .filter(v -> own.matcher(v.getCode()).matches()).toList();
    }

    /** VoucherSettings.nextHolidayCode() (BR-19, Q26): highest number of the prefix + 1, counting across the years. */
    static String nextHolidayCode(Holiday h, List<Voucher> previous) {
        int max = 0;
        Pattern number = Pattern.compile("^" + h.name() + "([0-9]{2})$");
        for (Voucher v : previous) {
            Matcher m = number.matcher(v.getCode());
            if (m.matches()) {
                max = Math.max(max, Integer.parseInt(m.group(1)));
            }
        }
        return h.name() + String.format("%02d", max + 1);
    }

    private LocalDateTime enabledAt(boolean enabled, boolean wasEnabled, LocalDateTime previous) {
        if (!enabled) {
            return null;
        }
        return wasEnabled && previous != null ? previous : LocalDateTime.now(clock).withNano(0);
    }

    private record Common(DiscountType type, BigDecimal value, Integer quantity, BigDecimal min, BigDecimal max) {
    }

    /** Type, value, quantity, minimum and maximum, checked like a manual voucher; type and value needed to turn it on. */
    private static Common common(Form form, Map<String, String> errors) {
        boolean typed = form.discountType() != null && !form.discountType().isBlank();
        DiscountType type = null;
        BigDecimal value = null;
        if (typed || form.enabled()) {
            type = VoucherAdminService.parseEnum(DiscountType.class, form.discountType(), "discountType",
                    "loại giảm giá (bắt buộc trước khi bật)", errors);
        }
        if (type != null) {
            value = VoucherAdminService.discountValue(type, form.discountValue(), errors);
        }
        int quantity = intIn(form.quantity(), 0, 1_000_000, "quantity", "Số lượng từ 0 trở lên", errors);
        BigDecimal min = VoucherAdminService.money(form.minOrderValue(), "minOrderValue", true, errors);
        BigDecimal max = null;
        if (form.maxDiscount() != null && !form.maxDiscount().isBlank()) {
            if (type == DiscountType.FIXED_AMOUNT) {
                errors.put("maxDiscount", "Voucher giảm số tiền cố định không có mức giảm tối đa");
            } else {
                max = VoucherAdminService.money(form.maxDiscount(), "maxDiscount", false, errors);
            }
        }
        return new Common(type, value, quantity, min, max);
    }

    private static int intIn(String raw, int min, int max, String field, String message, Map<String, String> errors) {
        try {
            int value = Integer.parseInt(raw == null ? "" : raw.trim());
            if (value < min || value > max) {
                errors.put(field, message);
            }
            return value;
        } catch (NumberFormatException e) {
            errors.put(field, message);
            return min;
        }
    }
}
