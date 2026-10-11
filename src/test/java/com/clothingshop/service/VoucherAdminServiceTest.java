package com.clothingshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clothingshop.TestDatabase;
import com.clothingshop.entity.CustomerLevel;
import com.clothingshop.entity.DiscountType;
import com.clothingshop.entity.EligibleCustomer;
import com.clothingshop.entity.Voucher;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.util.JpaUtil;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** FR-35 – FR-38: manual vouchers, settings file, monthly (J5) and holiday (J6) vouchers. */
class VoucherAdminServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @TempDir
    Path dir;
    private MutableClock clock;
    private VoucherAdminService admin;

    @BeforeAll
    static void database() {
        TestDatabase.start();
    }

    @BeforeEach
    void setUp() {
        clock = new MutableClock(TestData.NOW.atZone(ZONE).toInstant(), ZONE);
        admin = new VoucherAdminService(clock);
    }

    private static VoucherAdminService.Form form(String code, String type, String value, String max, String start, String end) {
        return new VoucherAdminService.Form(code, type, value, "10", "ALL", "100000", max, start, end);
    }

    private Voucher voucher(String code) {
        return JpaUtil.read(em -> em.find(Voucher.class, code));
    }

    private void setNow(LocalDateTime t) {
        clock.advance(Duration.between(LocalDateTime.ofInstant(clock.instant(), ZONE), t));
    }

    @Test
    void manualVouchersFollowTheCodeAndAmountRules() {
        String code = TestData.unique("km-").toLowerCase();
        admin.create(form(code, "PERCENT", "15", "50000", "2026-10-20T00:00", "2026-10-30T00:00"));
        Voucher v = voucher(code.toUpperCase());
        assertEquals(new BigDecimal("15.00"), v.getDiscountValue());
        assertEquals(new BigDecimal("50000.00"), v.getPolicy().getMaxDiscount());
        assertThrows(ValidationException.class, () -> admin.create(form(code, "PERCENT", "15", null, "2026-10-20T00:00", "2026-10-30T00:00")),
                "the code already exists, letter case ignored");

        Map<String, String> e = assertThrows(ValidationException.class, () -> admin.validate(form("Mã có dấu", "PERCENT", "101", null,
                "2026-10-30T00:00", "2026-10-20T00:00"), null)).getErrors();
        assertTrue(e.containsKey("code"));
        assertTrue(e.containsKey("discountValue"));
        assertTrue(e.containsKey("endDate"));
        for (String reserved : List.of("TET03", "gioto12", "LOYAL-102026", "casual-012027")) {
            assertTrue(assertThrows(ValidationException.class, () -> admin.validate(form(reserved, "PERCENT", "10", null,
                    "2026-10-20T00:00", "2026-10-30T00:00"), null)).getErrors().get("code").contains("tự phát hành"), reserved);
        }
        Map<String, String> fixed = assertThrows(ValidationException.class, () -> admin.validate(form("GIAMTIEN", "FIXED_AMOUNT",
                "25500", "10000", "2026-10-20T00:00", "2026-10-30T00:00"), null)).getErrors();
        assertTrue(fixed.get("discountValue").contains("1.000"));
        assertTrue(fixed.get("maxDiscount").contains("cố định"), "no maximum for a fixed amount");
    }

    @Test
    void aManualVoucherCanBeEditedOnlyBeforeItStartsAndNeverChangesCode() {
        String code = TestData.unique("SUA-").toUpperCase();
        admin.create(form(code, "PERCENT", "10", null, "2026-10-12T00:00", "2026-10-20T00:00"));
        assertTrue(admin.get(code).editable());
        admin.update(code, form("KHAC", "FIXED_AMOUNT", "30000", null, "2026-10-13T00:00", "2026-10-21T00:00"));
        Voucher v = voucher(code);
        assertEquals(DiscountType.FIXED_AMOUNT, v.getDiscountType());
        assertNull(voucher("KHAC"), "the code in the form is ignored when editing (Q63)");
        setNow(LocalDateTime.parse("2026-10-13T00:00"));
        assertFalse(admin.get(code).editable());
        assertThrows(BusinessException.class, () -> admin.update(code, form(null, "PERCENT", "5", null, "2026-10-14T00:00", "2026-10-21T00:00")),
                "BR-22: from the start date on it can only be viewed");
        assertEquals("Đang hiệu lực", admin.get(code).state());
    }

    @Test
    void settingsAreSavedAtomicallyAndADefaultFileIsCreated() throws IOException {
        Path file = dir.resolve("voucher-settings.json");
        VoucherSettingsStore store = new VoucherSettingsStore(file, clock);
        assertTrue(Files.exists(file), "created with the values of the specification (Q49)");
        assertTrue(store.get().monthly().get(CustomerLevel.LOYAL).enabled());
        assertFalse(store.get().holidays().get(Holiday.TET).enabled());
        AutoVoucherService auto = new AutoVoucherService(store, clock);
        auto.saveHoliday(Holiday.GPMN, new AutoVoucherService.Form(true, "PERCENT", "20", "50", "0", "300000", "ALL", null, null, "0", "5"));
        VoucherSettingsStore reloaded = new VoucherSettingsStore(file, clock);
        VoucherSettings.Campaign gpmn = reloaded.get().holidays().get(Holiday.GPMN);
        assertTrue(gpmn.enabled());
        assertEquals(TestData.NOW, gpmn.enabledAt(), "enabledAt recorded when turned on (Q69)");
        assertEquals(50, gpmn.quantity());
        try (var files = Files.list(dir)) {
            assertEquals(1, files.count(), "no temporary file left behind");
        }
        Files.writeString(file, "{ not json");
        VoucherSettingsStore broken = new VoucherSettingsStore(file, clock);
        assertTrue(broken.get().monthly().get(CustomerLevel.LOYAL).enabled(), "defaults in memory");
        assertEquals("{ not json", Files.readString(file), "the broken file is kept for the manager");
    }

    @Test
    void campaignsCannotBeTurnedOnWithoutTypeAndValueAndTetNeverStartsEarly() {
        AutoVoucherService auto = new AutoVoucherService(new VoucherSettingsStore(dir.resolve("s.json"), clock), clock);
        Map<String, String> e = assertThrows(ValidationException.class, () -> auto.saveHoliday(Holiday.TET,
                new AutoVoucherService.Form(true, "", "", "20", "0", "500000", "ALL", null, null, "-1", "5"))).getErrors();
        assertTrue(e.containsKey("discountType"), "Q49");
        assertTrue(e.containsKey("startOffsetDays"), "not before the 1st day of Tết");
        assertThrows(ValidationException.class, () -> auto.saveMonthly(CustomerLevel.CASUAL,
                new AutoVoucherService.Form(true, "PERCENT", "10", "100", "0", null, null, "10", "5", null, null)), "end before start");
    }

    @Test
    void j5IssuesTheMonthlyVoucherOfLevelsTurnedOnBeforeTheMonthOnce() {
        VoucherSettingsStore store = new VoucherSettingsStore(dir.resolve("s.json"), clock);   // LOYAL, REGULAR on since 10/10
        AutoVoucherService auto = new AutoVoucherService(store, clock);
        auto.issueMonthlyVouchers();
        assertNull(voucher("LOYAL-102026"), "turned on after 01/10 00:00: from next month (Q69)");
        auto.saveMonthly(CustomerLevel.CASUAL, new AutoVoucherService.Form(true, "FIXED_AMOUNT", "20000", "500", "200000", null,
                null, "5", "31", null, null));
        setNow(LocalDateTime.parse("2026-11-01T00:00"));
        auto.issueMonthlyVouchers();
        auto.issueMonthlyVouchers();
        Voucher loyal = voucher("LOYAL-112026");
        assertEquals(EligibleCustomer.LOYAL, loyal.getPolicy().getEligibleCustomer());
        assertEquals(10000, loyal.getQuantity(), "Q67");
        assertEquals(LocalDateTime.parse("2026-11-01T00:00"), loyal.getPolicy().getStartDate());
        assertEquals(LocalDateTime.parse("2026-11-30T23:59:59"), loyal.getPolicy().getEndDate());
        Voucher casual = voucher("CASUAL-112026");
        assertEquals(LocalDateTime.parse("2026-11-05T00:00"), casual.getPolicy().getStartDate());
        assertEquals(LocalDateTime.parse("2026-11-30T23:59:59"), casual.getPolicy().getEndDate(), "day 31 in November means its last day");
        assertEquals(500, casual.getQuantity());
        assertNull(casual.getPolicy().getMaxDiscount());
    }

    @Test
    void j6CreatesHolidayVouchersNumberedAcrossYears() {
        VoucherSettingsStore store = new VoucherSettingsStore(dir.resolve("s.json"), clock);
        AutoVoucherService auto = new AutoVoucherService(store, clock);
        long before = JpaUtil.read(em -> em.createQuery("select count(v) from Voucher v where v.code like 'QK%'", Long.class).getSingleResult());
        auto.saveHoliday(Holiday.QK, new AutoVoucherService.Form(true, "PERCENT", "20", "20", "0", "500000", "ALL", null, null, "0", "5"));
        setNow(LocalDateTime.parse("2027-09-01T23:00"));
        auto.createHolidayVouchers();
        assertEquals(before, (long) JpaUtil.<Long>read(em -> em.createQuery("select count(v) from Voucher v where v.code like 'QK%'",
                Long.class).getSingleResult()), "not yet 02/09");
        setNow(LocalDateTime.parse("2027-09-03T10:00"));
        auto.createHolidayVouchers();
        auto.createHolidayVouchers();
        List<Voucher> qk = JpaUtil.read(em -> em.createQuery("select v from Voucher v where v.code like 'QK%' order by v.code", Voucher.class)
                .getResultList());
        assertEquals(before + 1, qk.size(), "once per year");
        Voucher created = qk.get(qk.size() - 1);
        assertEquals(LocalDateTime.parse("2027-09-02T00:00"), created.getPolicy().getStartDate());
        assertEquals(LocalDateTime.parse("2027-09-07T00:00"), created.getPolicy().getEndDate(), "5 × 24 hours (Q49)");
        assertEquals("QK" + String.format("%02d", before + 1), created.getCode(), "prefix + next number");
        setNow(LocalDateTime.parse("2028-09-02T00:00"));
        auto.createHolidayVouchers();
        assertEquals("QK" + String.format("%02d", before + 2), JpaUtil.read(em -> em.createQuery("select max(v.code) from Voucher v"
                + " where v.code like 'QK%'", String.class).getSingleResult()), "counted across the years (BR-19)");
    }

    @Test
    void j6UsesTheLunarDateOfTet() {
        VoucherSettingsStore store = new VoucherSettingsStore(dir.resolve("s.json"), clock);
        AutoVoucherService auto = new AutoVoucherService(store, clock);
        auto.saveHoliday(Holiday.TET, new AutoVoucherService.Form(true, "PERCENT", "20", "20", "0", "500000", "ALL", null, null, "0", "5"));
        setNow(LocalDateTime.parse("2027-02-06T00:00"));   // mùng 1 Tết Đinh Mùi
        auto.createHolidayVouchers();
        assertEquals(LocalDateTime.parse("2027-02-06T00:00"), JpaUtil.read(em -> em.createQuery("select v.policy.startDate from Voucher v"
                + " where v.code like 'TET%' order by v.policy.startDate desc", LocalDateTime.class).setMaxResults(1).getSingleResult()));
    }
}
