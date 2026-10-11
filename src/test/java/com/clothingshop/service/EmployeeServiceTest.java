package com.clothingshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clothingshop.TestDatabase;
import com.clothingshop.dto.EmployeeForm;
import com.clothingshop.entity.Customer;
import com.clothingshop.entity.Employee;
import com.clothingshop.entity.EmployeeRole;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.util.JpaUtil;
import com.clothingshop.util.PasswordHasher;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** FR-40 staff accounts, FR-41 lock and unlock, BR-24 manager account created from the configuration. */
class EmployeeServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final EmployeeService service = new EmployeeService(new MutableClock(TestData.NOW.atZone(ZONE).toInstant(), ZONE));

    @BeforeAll
    static void database() {
        TestDatabase.start();
    }

    private static EmployeeForm form(String userName, String email) {
        return new EmployeeForm(userName, email, "Trần Nhân Viên", "MALE", "1995-05-05", "0912345678", "matkhau-123", "matkhau-123");
    }

    @Test
    void createsActiveStaffAccountsOnly() {
        String name = TestData.unique("nv");
        int id = service.create(form(name, name + "@example.com"));
        Employee e = JpaUtil.read(em -> em.find(Employee.class, id));
        assertEquals(EmployeeRole.STAFF, e.getRole());
        assertTrue(e.isActive());
        assertEquals(TestData.NOW.toLocalDate(), e.getHireDate());
        assertTrue(PasswordHasher.verify("matkhau-123", e.getPassword()));
    }

    @Test
    void userNameAndEmailMustNotBelongToAnyAccountIncludingCustomers() {
        Customer customer = TestData.customer("matkhau-123");
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(form(customer.getUserName().toUpperCase(), customer.getEmail())));
        assertEquals(java.util.Set.of("userName", "email"), e.getErrors().keySet());
    }

    @Test
    void staffCanBeLockedAndUnlockedButTheManagerCannot() {
        Employee staff = TestData.employee(EmployeeRole.STAFF, "matkhau-123");
        service.setActive(staff.getId(), false);
        assertFalse(service.isActive(staff.getId()));
        service.setActive(staff.getId(), true);
        assertTrue(service.isActive(staff.getId()));

        Employee admin = TestData.employee(EmployeeRole.ADMIN, "matkhau-123");
        assertThrows(BusinessException.class, () -> service.setActive(admin.getId(), false));
    }

    @Test
    void theManagerAccountIsCreatedOnceFromTheConfiguration() {
        long before = admins();
        String name = TestData.unique("ql");
        System.setProperty("clothingshop.admin.userName", name);
        System.setProperty("clothingshop.admin.email", name + "@example.com");
        System.setProperty("clothingshop.admin.password", "matkhau-quan-ly");
        System.setProperty("clothingshop.admin.fullName", "Quản Lý Cửa Hàng");
        System.setProperty("clothingshop.admin.phone", "0909090909");
        try {
            service.ensureAdmin();
            service.ensureAdmin();
        } finally {
            for (String key : new String[] {"userName", "email", "password", "fullName", "phone"}) {
                System.clearProperty("clothingshop.admin." + key);
            }
        }
        // Another test may already have an ADMIN row in the shared database; then nothing is created (BR-24)
        assertEquals(before == 0 ? 1 : before, admins());
    }

    private static long admins() {
        return JpaUtil.read(em -> em.createQuery("select count(e) from Employee e where e.role = :r", Long.class)
                .setParameter("r", EmployeeRole.ADMIN).getSingleResult());
    }
}
