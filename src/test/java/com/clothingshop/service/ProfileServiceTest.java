package com.clothingshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clothingshop.TestDatabase;
import com.clothingshop.dto.ProfileForm;
import com.clothingshop.dto.ProfileView;
import com.clothingshop.entity.Customer;
import com.clothingshop.entity.CustomerLevel;
import com.clothingshop.entity.Employee;
import com.clothingshop.entity.EmployeeRole;
import com.clothingshop.entity.Gender;
import com.clothingshop.entity.User;
import com.clothingshop.exception.ConflictException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.util.JpaUtil;
import com.clothingshop.util.PasswordHasher;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** FR-04 profile and FR-05 password change. */
class ProfileServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final ProfileService service = new ProfileService(new MutableClock(TestData.NOW.atZone(ZONE).toInstant(), ZONE));

    @BeforeAll
    static void database() {
        TestDatabase.start();
    }

    @Test
    void savesTheEditableFieldsOnly() {
        Customer customer = TestData.customer("matkhau-123");
        ProfileView before = service.get(customer.getId());
        assertEquals(CustomerLevel.CASUAL, before.level());
        assertEquals("Khách hàng", before.role());

        ProfileView after = service.update(customer.getId(),
                new ProfileForm("  Trần Thị Bình ", "OTHER", "1999-12-31", "+84901234567", before.version()));
        assertEquals("Trần Thị Bình", after.fullName());
        assertEquals(Gender.OTHER, after.gender());
        assertEquals(LocalDate.of(1999, 12, 31), after.dob());
        assertEquals(before.version() + 1, after.version());
        assertEquals(before.userName(), after.userName());
        assertEquals(before.email(), after.email());
    }

    @Test
    void aFormOpenedBeforeAnotherSaveIsRefused() {
        Customer customer = TestData.customer("matkhau-123");
        long opened = service.get(customer.getId()).version();
        service.update(customer.getId(), new ProfileForm("Lần một", "MALE", "2000-01-01", "0900000001", opened));
        assertThrows(ConflictException.class, () ->
                service.update(customer.getId(), new ProfileForm("Lần hai", "MALE", "2000-01-01", "0900000002", opened)));
        assertEquals("Lần một", service.get(customer.getId()).fullName());
    }

    @Test
    void invalidValuesAreReportedPerField() {
        Customer customer = TestData.customer("matkhau-123");
        long version = service.get(customer.getId()).version();
        ValidationException e = assertThrows(ValidationException.class, () ->
                service.update(customer.getId(), new ProfileForm("", "X", "2030-01-01", "abc", version)));
        assertEquals(java.util.Set.of("fullName", "gender", "dob", "phone"), e.getErrors().keySet());
    }

    @Test
    void employeesHaveAProfileWithoutLevel() {
        Employee staff = TestData.employee(EmployeeRole.STAFF, "matkhau-123");
        ProfileView view = service.get(staff.getId());
        assertEquals("Nhân viên", view.role());
        assertNull(view.level());
    }

    @Test
    void changingThePasswordNeedsTheCurrentOne() {
        Customer customer = TestData.customer("matkhau-123");
        ValidationException wrong = assertThrows(ValidationException.class, () ->
                service.changePassword(customer.getId(), "sai-mat-khau", "matkhau-moi-456", "matkhau-moi-456"));
        assertTrue(wrong.getErrors().containsKey("currentPassword"));
        ValidationException mismatch = assertThrows(ValidationException.class, () ->
                service.changePassword(customer.getId(), "matkhau-123", "matkhau-moi-456", "khac"));
        assertTrue(mismatch.getErrors().containsKey("confirmPassword"));

        service.changePassword(customer.getId(), "matkhau-123", "matkhau-moi-456", "matkhau-moi-456");
        String hash = JpaUtil.read(em -> em.find(User.class, customer.getId()).getPassword());
        assertTrue(PasswordHasher.verify("matkhau-moi-456", hash));
    }
}
