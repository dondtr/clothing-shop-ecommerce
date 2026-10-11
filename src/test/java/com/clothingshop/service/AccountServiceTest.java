package com.clothingshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clothingshop.TestDatabase;
import com.clothingshop.dao.UserDao;
import com.clothingshop.dto.AuthUser;
import com.clothingshop.dto.RegistrationForm;
import com.clothingshop.entity.Customer;
import com.clothingshop.entity.CustomerLevel;
import com.clothingshop.entity.Employee;
import com.clothingshop.entity.EmployeeRole;
import com.clothingshop.entity.Gender;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.util.JpaUtil;
import com.clothingshop.util.PasswordHasher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.RollbackException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** FR-01 (registration with email verification) and FR-02 (sign-in), against an in-memory database. */
class AccountServiceTest {

    private static final String VERIFY_URL = "http://localhost:8080/clothing-shop/register/verify";
    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+)");

    private record Email(String to, String subject, String body) {
    }

    private final List<Email> outbox = new ArrayList<>();
    private MutableClock clock;
    private AccountService service;
    private String suffix;

    @BeforeAll
    static void database() {
        TestDatabase.start();
    }

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.now(), ZoneId.of("Asia/Ho_Chi_Minh"));
        service = new AccountService((to, subject, body) -> outbox.add(new Email(to, subject, body)),
                new PendingRegistrationStore(clock), clock);
        suffix = UUID.randomUUID().toString().substring(0, 8);
    }

    private RegistrationForm form(String userName, String email) {
        return new RegistrationForm(userName, "matkhau123", "matkhau123", email, "Nguyễn Văn An", "MALE", "2001-05-20", "0901234567");
    }

    private String tokenFromLastEmail() {
        Matcher m = TOKEN.matcher(outbox.get(outbox.size() - 1).body());
        assertTrue(m.find(), "the email must contain the verification link");
        return m.group(1);
    }

    private boolean userExists(String userName) {
        return JpaUtil.read(em -> new UserDao(em).existsByUserName(userName));
    }

    @Test
    void registrationSendsALinkAndCreatesNoAccountBeforeVerification() {
        String userName = "an" + suffix;
        String sentTo = service.register(form(userName, "An." + suffix + "@Example.COM"), VERIFY_URL);

        assertEquals(("an." + suffix + "@example.com"), sentTo, "email is stored in lower case (BR-17)");
        assertEquals(1, outbox.size());
        assertEquals(sentTo, outbox.get(0).to());
        assertTrue(outbox.get(0).body().contains(VERIFY_URL + "?token="));
        assertFalse(userExists(userName), "the account is written only after verification (Q35)");
    }

    @Test
    void verificationCreatesTheCustomerWhoCanThenSignInWithUserNameOrEmail() {
        String userName = "binh" + suffix;
        String email = "binh." + suffix + "@example.com";
        service.register(form(userName, email), VERIFY_URL);

        Customer customer = service.verifyEmail(tokenFromLastEmail());
        assertEquals(CustomerLevel.CASUAL, customer.getCustomerLevel());
        assertTrue(customer.isAutoDowngrade());
        assertEquals(0L, customer.getVersion());
        assertTrue(userExists(userName));

        AuthUser byName = service.login(userName.toUpperCase(Locale.ROOT), "matkhau123");
        assertEquals(AuthUser.Role.CUSTOMER, byName.getRole());
        assertEquals("Nguyễn Văn An", byName.getFullName());
        AuthUser byEmail = service.login(email.toUpperCase(Locale.ROOT), "matkhau123");
        assertEquals(byName.getId(), byEmail.getId());
    }

    @Test
    void aVerificationLinkWorksOnlyOnce() {
        service.register(form("chi" + suffix, "chi." + suffix + "@example.com"), VERIFY_URL);
        String token = tokenFromLastEmail();
        service.verifyEmail(token);
        BusinessException again = assertThrows(BusinessException.class, () -> service.verifyEmail(token));
        assertEquals(AccountService.MSG_LINK_INVALID, again.getMessage());
    }

    @Test
    void anExpiredOrTamperedLinkIsRefused() {
        String userName = "dung" + suffix;
        service.register(form(userName, "dung." + suffix + "@example.com"), VERIFY_URL);
        String token = tokenFromLastEmail();

        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");
        assertThrows(BusinessException.class, () -> service.verifyEmail(tampered));

        clock.advance(Duration.ofHours(24).plusMinutes(1));
        assertThrows(BusinessException.class, () -> service.verifyEmail(token));
        assertFalse(userExists(userName));
    }

    @Test
    void invalidInputIsReportedFieldByField() {
        ValidationException blank = assertThrows(ValidationException.class,
                () -> service.register(new RegistrationForm("", "", "", "", "", "", "", ""), VERIFY_URL));
        assertEquals(List.of("userName", "fullName", "email", "phone", "gender", "dob", "password"),
                List.copyOf(blank.getErrors().keySet()));

        ValidationException bad = assertThrows(ValidationException.class, () -> service.register(new RegistrationForm(
                "co @ cach", "1234567", "1234567", "khong-phai-email", "X", "UNKNOWN", "2999-01-01", "09-12"), VERIFY_URL));
        Map<String, String> errors = bad.getErrors();
        assertEquals("Tên đăng nhập không được chứa @ hoặc khoảng trắng", errors.get("userName"));
        assertEquals("Email không hợp lệ", errors.get("email"));
        assertEquals("Mật khẩu phải có ít nhất 8 ký tự", errors.get("password"));
        assertTrue(errors.containsKey("gender") && errors.containsKey("dob") && errors.containsKey("phone"));

        ValidationException mismatch = assertThrows(ValidationException.class, () -> service.register(
                new RegistrationForm("em" + suffix, "matkhau123", "matkhau124", "em" + suffix + "@example.com", "Em", "FEMALE",
                        "2000-01-01", "0901234567"), VERIFY_URL));
        assertEquals(Map.of("confirmPassword", "Mật khẩu nhập lại không khớp"), mismatch.getErrors());
        assertTrue(outbox.isEmpty());
    }

    @Test
    void userNameAndEmailMustBeUniqueIgnoringCase() {
        String userName = "giang" + suffix;
        String email = "giang." + suffix + "@example.com";
        service.register(form(userName, email), VERIFY_URL);
        service.verifyEmail(tokenFromLastEmail());

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.register(form(userName.toUpperCase(Locale.ROOT), email.toUpperCase(Locale.ROOT)), VERIFY_URL));
        assertEquals("Tên đăng nhập đã được sử dụng", e.getErrors().get("userName"));
        assertEquals("Email đã được sử dụng", e.getErrors().get("email"));
    }

    @Test
    void verificationFailsWhenTheUserNameWasTakenMeanwhile() {
        String userName = "hoa" + suffix;
        service.register(form(userName, "hoa." + suffix + "@example.com"), VERIFY_URL);
        String token = tokenFromLastEmail();
        JpaUtil.inTransaction(em -> {
            em.persist(new Customer(userName, PasswordHasher.hash("khac12345"), "Người khác", Gender.FEMALE,
                    LocalDate.of(1999, 9, 9), "khac." + suffix + "@example.com", "0909999999", LocalDateTime.now(clock)));
            return null;
        });
        BusinessException e = assertThrows(BusinessException.class, () -> service.verifyEmail(token));
        assertEquals(AccountService.MSG_TAKEN, e.getMessage());
    }

    @Test
    void wrongCredentialsGiveTheSameMessage() {
        String userName = "khanh" + suffix;
        service.register(form(userName, "khanh." + suffix + "@example.com"), VERIFY_URL);
        service.verifyEmail(tokenFromLastEmail());

        assertEquals(AccountService.MSG_BAD_CREDENTIALS,
                assertThrows(BusinessException.class, () -> service.login(userName, "sai-mat-khau")).getMessage());
        assertEquals(AccountService.MSG_BAD_CREDENTIALS,
                assertThrows(BusinessException.class, () -> service.login("khong-ton-tai" + suffix, "matkhau123")).getMessage());
        assertEquals(AccountService.MSG_BAD_CREDENTIALS,
                assertThrows(BusinessException.class, () -> service.login("", "")).getMessage());
    }

    @Test
    void employeesSignInWithTheirRoleAndLockedOnesAreRefused() {
        String admin = "quanly" + suffix;
        String locked = "nv" + suffix;
        JpaUtil.inTransaction(em -> {
            em.persist(new Employee(admin, PasswordHasher.hash("quanly123"), "Quản Lý", Gender.FEMALE, LocalDate.of(1990, 1, 1),
                    admin + "@shop.vn", "0911111111", LocalDate.now(clock), EmployeeRole.ADMIN));
            Employee staff = new Employee(locked, PasswordHasher.hash("nhanvien123"), "Nhân Viên", Gender.MALE,
                    LocalDate.of(1995, 1, 1), locked + "@shop.vn", "0922222222", LocalDate.now(clock), EmployeeRole.STAFF);
            staff.setActive(false);
            em.persist(staff);
            return null;
        });
        assertEquals(AuthUser.Role.ADMIN, service.login(admin, "quanly123").getRole());
        assertEquals(AccountService.MSG_LOCKED,
                assertThrows(BusinessException.class, () -> service.login(locked, "nhanvien123")).getMessage());
    }

    @Test
    void concurrentUpdatesOfTheSameRowAreDetectedByVersion() {
        String userName = "lan" + suffix;
        service.register(form(userName, "lan." + suffix + "@example.com"), VERIFY_URL);
        Integer id = service.verifyEmail(tokenFromLastEmail()).getId();

        EntityManager first = JpaUtil.getEntityManagerFactory().createEntityManager();
        EntityManager second = JpaUtil.getEntityManagerFactory().createEntityManager();
        try {
            first.getTransaction().begin();
            second.getTransaction().begin();
            first.find(Customer.class, id).setPhone("0900000001");
            second.find(Customer.class, id).setPhone("0900000002");
            first.getTransaction().commit();
            assertThrows(RollbackException.class, () -> second.getTransaction().commit(),
                    "the second save is based on a stale version and must not overwrite the first");
        } finally {
            first.close();
            second.close();
        }
        assertEquals("0900000001", JpaUtil.read(em -> em.find(Customer.class, id).getPhone()));
        assertEquals(Long.valueOf(1L), JpaUtil.<Long>read(em -> em.find(Customer.class, id).getVersion()));
    }
}
