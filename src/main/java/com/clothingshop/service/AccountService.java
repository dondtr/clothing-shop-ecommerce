package com.clothingshop.service;

import com.clothingshop.dao.UserDao;
import com.clothingshop.dto.AuthUser;
import com.clothingshop.dto.RegistrationForm;
import com.clothingshop.entity.Customer;
import com.clothingshop.entity.Employee;
import com.clothingshop.entity.EmployeeRole;
import com.clothingshop.entity.Gender;
import com.clothingshop.entity.User;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.service.PendingRegistrationStore.PendingRegistration;
import com.clothingshop.util.JpaUtil;
import com.clothingshop.util.PasswordHasher;
import com.clothingshop.util.SignedToken;
import jakarta.persistence.PersistenceException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customer registration with email verification (FR-01) and sign-in (FR-02).
 */
public class AccountService {

    private static final Logger LOG = LoggerFactory.getLogger(AccountService.class);

    /** Validity of the verification link and of the pending registration (Q50). */
    static final Duration VERIFY_LINK_TTL = Duration.ofHours(24);
    static final String VERIFY_PURPOSE = "verify-email";

    static final String MSG_BAD_CREDENTIALS = "Tên đăng nhập/email hoặc mật khẩu không đúng";
    static final String MSG_LOCKED = "Tài khoản nhân viên đã bị khóa";
    static final String MSG_LINK_INVALID = "Link xác thực không hợp lệ, đã hết hạn hoặc đã được dùng. Vui lòng đăng ký lại.";
    static final String MSG_TAKEN = "Tên đăng nhập hoặc email đã được tài khoản khác sử dụng. Vui lòng đăng ký lại.";
    static final String MSG_MAIL_FAILED = "Không gửi được email xác thực. Vui lòng thử lại sau.";


    /** Compared when the account does not exist, so the response time does not reveal which accounts exist. */
    private static final String DUMMY_HASH = PasswordHasher.hash("account-does-not-exist");

    private final EmailService emailService;
    private final PendingRegistrationStore pendingRegistrations;
    private final Clock clock;

    public AccountService(EmailService emailService, PendingRegistrationStore pendingRegistrations, Clock clock) {
        this.emailService = emailService;
        this.pendingRegistrations = pendingRegistrations;
        this.clock = clock;
    }

    /**
     * FR-01 steps 1 – 3: validates the form, keeps the registration in memory and emails a verification link (E8).
     *
     * @param verifyUrl absolute URL of the verification page; the signed token is appended as {@code ?token=}
     * @return the normalized email address the link was sent to
     */
    public String register(RegistrationForm form, String verifyUrl) {
        Map<String, String> errors = new LinkedHashMap<>();
        String userName = UserRules.userName(form.userName(), errors);
        String fullName = UserRules.fullName(form.fullName(), errors);
        String email = UserRules.email(form.email(), errors);
        String phone = UserRules.phone(form.phone(), errors);
        Gender gender = UserRules.gender(form.gender(), errors);
        LocalDate dob = UserRules.dob(form.dob(), LocalDate.now(clock), errors);
        String password = UserRules.newPassword(form.password(), form.confirmPassword(), "password", "confirmPassword", errors);
        UserRules.checkUnique(userName, email, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        Instant expiresAt = clock.instant().plus(VERIFY_LINK_TTL);
        String id = pendingRegistrations.add(new PendingRegistration(
                userName, PasswordHasher.hash(password), email, fullName, gender, dob, phone, expiresAt));
        String link = verifyUrl + "?token=" + SignedToken.create(VERIFY_PURPOSE, id, expiresAt);
        try {
            Emails.Message message = Emails.verification(fullName, link);
            emailService.send(email, message.subject(), message.body());
        } catch (IllegalStateException e) {
            LOG.error("Verification email could not be sent to {}", email, e);
            throw new BusinessException(MSG_MAIL_FAILED);
        }
        LOG.info("Registration pending email verification: userName={}", userName);
        return email;
    }

    /** FR-01 steps 4 – 5: creates the User and Customer rows from a verification link, in one transaction. */
    public Customer verifyEmail(String token) {
        String id = SignedToken.verify(VERIFY_PURPOSE, token, clock.instant())
                .orElseThrow(() -> new BusinessException(MSG_LINK_INVALID));
        PendingRegistration r = pendingRegistrations.take(id).orElseThrow(() -> new BusinessException(MSG_LINK_INVALID));
        try {
            Customer customer = JpaUtil.inTransaction(em -> {
                UserDao users = new UserDao(em);
                if (users.existsByUserName(r.userName()) || users.existsByEmail(r.email())) {
                    throw new BusinessException(MSG_TAKEN);
                }
                Customer c = new Customer(r.userName(), r.passwordHash(), r.fullName(), r.gender(), r.dob(), r.email(),
                        r.phone(), LocalDateTime.now(clock));
                users.persist(c);
                return c;
            });
            LOG.info("Customer account created: id={}, userName={}", customer.getId(), customer.getUserName());
            return customer;
        } catch (PersistenceException e) {
            // Another verification took the same userName or email between the check and the commit
            LOG.warn("Account creation failed for userName={}", r.userName(), e);
            throw new BusinessException(MSG_TAKEN);
        }
    }

    /**
     * FR-02: signs in with a user name or an email (a value containing @ is an email) and a password.
     * Wrong credentials always give the same message; a locked employee account is refused (BR-18).
     */
    public AuthUser login(String login, String password) {
        String value = UserRules.trim(login);
        if (value.isEmpty() || password == null || password.isEmpty()) {
            throw new BusinessException(MSG_BAD_CREDENTIALS);
        }
        Optional<User> found = JpaUtil.read(em -> {
            UserDao users = new UserDao(em);
            return value.contains("@") ? users.findByEmail(value) : users.findByUserName(value);
        });
        if (found.isEmpty()) {
            PasswordHasher.verify(password, DUMMY_HASH);
            throw new BusinessException(MSG_BAD_CREDENTIALS);
        }
        User user = found.get();
        if (!PasswordHasher.verify(password, user.getPassword())) {
            throw new BusinessException(MSG_BAD_CREDENTIALS);
        }
        if (user instanceof Employee employee) {
            if (!employee.isActive()) {
                throw new BusinessException(MSG_LOCKED);
            }
            AuthUser.Role role = employee.getRole() == EmployeeRole.ADMIN ? AuthUser.Role.ADMIN : AuthUser.Role.STAFF;
            return new AuthUser(employee.getId(), employee.getUserName(), employee.getFullName(), role);
        }
        return new AuthUser(user.getId(), user.getUserName(), user.getFullName(), AuthUser.Role.CUSTOMER);
    }
}
