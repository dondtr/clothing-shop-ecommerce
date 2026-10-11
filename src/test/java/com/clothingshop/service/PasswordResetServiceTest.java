package com.clothingshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clothingshop.TestDatabase;
import com.clothingshop.entity.Customer;
import com.clothingshop.entity.User;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.util.JpaUtil;
import com.clothingshop.util.PasswordHasher;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** FR-03 forgotten password. */
class PasswordResetServiceTest {

    private static final String RESET_URL = "http://localhost/shop/reset-password";
    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+)");

    private final List<String[]> outbox = new ArrayList<>();
    private MutableClock clock;
    private PasswordResetService service;

    @BeforeAll
    static void database() {
        TestDatabase.start();
    }

    @BeforeEach
    void setUp() {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        clock = new MutableClock(TestData.NOW.atZone(zone).toInstant(), zone);
        service = new PasswordResetService((to, subject, body) -> outbox.add(new String[] {to, subject, body}), clock);
    }

    private String tokenOfLastEmail() {
        Matcher m = TOKEN.matcher(outbox.get(outbox.size() - 1)[2]);
        assertTrue(m.find(), "the email contains the reset link");
        return m.group(1);
    }

    private String passwordHash(int userId) {
        return JpaUtil.read(em -> em.find(User.class, userId).getPassword());
    }

    @Test
    void unknownEmailsSendNothing() {
        service.requestReset("khong-ton-tai@example.com", RESET_URL);
        service.requestReset("", RESET_URL);
        assertTrue(outbox.isEmpty());
    }

    @Test
    void theLinkSetsANewPasswordOnce() {
        Customer customer = TestData.customer("matkhau-cu-123");
        service.requestReset(customer.getEmail().toUpperCase(), RESET_URL);
        assertEquals(1, outbox.size());
        assertEquals(customer.getEmail(), outbox.get(0)[0]);
        String token = tokenOfLastEmail();
        assertTrue(service.isUsable(token));

        ValidationException tooShort = assertThrows(ValidationException.class, () -> service.resetPassword(token, "ngan", "ngan"));
        assertTrue(tooShort.getErrors().containsKey("password"));

        service.resetPassword(token, "matkhau-moi-456", "matkhau-moi-456");
        assertTrue(PasswordHasher.verify("matkhau-moi-456", passwordHash(customer.getId())));
        assertFalse(service.isUsable(token), "the password changed, so the link no longer works");
        assertThrows(BusinessException.class, () -> service.resetPassword(token, "matkhau-khac-789", "matkhau-khac-789"));
    }

    @Test
    void theLinkExpiresAfter15Minutes() {
        Customer customer = TestData.customer("matkhau-cu-123");
        service.requestReset(customer.getEmail(), RESET_URL);
        String token = tokenOfLastEmail();
        clock.advance(Duration.ofMinutes(16));
        assertFalse(service.isUsable(token));
        assertThrows(BusinessException.class, () -> service.resetPassword(token, "matkhau-moi-456", "matkhau-moi-456"));
        assertTrue(PasswordHasher.verify("matkhau-cu-123", passwordHash(customer.getId())));
    }
}
