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
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
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

    private static final int PASSWORD_MIN = 8;      // BR-20
    private static final int PASSWORD_MAX = 128;    // giới hạn kỹ thuật, tránh chuỗi quá dài khi băm
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{8,14}$");
    private static final Pattern WHITESPACE = Pattern.compile("\\s");
    private static final LocalDate MIN_DOB = LocalDate.of(1900, 1, 1);

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
        String userName = trim(form.userName());
        String fullName = trim(form.fullName());
        String email = trim(form.email()).toLowerCase(Locale.ROOT);
        String phone = trim(form.phone());
        String password = form.password() == null ? "" : form.password();

        if (userName.isEmpty()) {
            errors.put("userName", "Vui lòng nhập tên đăng nhập");
        } else if (userName.length() > 50) {
            errors.put("userName", "Tên đăng nhập tối đa 50 ký tự");
        } else if (userName.contains("@") || WHITESPACE.matcher(userName).find()) {
            errors.put("userName", "Tên đăng nhập không được chứa @ hoặc khoảng trắng");
        }
        if (fullName.isEmpty()) {
            errors.put("fullName", "Vui lòng nhập họ và tên");
        } else if (fullName.length() > 100) {
            errors.put("fullName", "Họ và tên tối đa 100 ký tự");
        }
        if (email.isEmpty()) {
            errors.put("email", "Vui lòng nhập email");
        } else if (email.length() > 100) {
            errors.put("email", "Email tối đa 100 ký tự");
        } else if (!EMAIL.matcher(email).matches()) {
            errors.put("email", "Email không hợp lệ");
        }
        if (phone.isEmpty()) {
            errors.put("phone", "Vui lòng nhập số điện thoại");
        } else if (!PHONE.matcher(phone).matches()) {
            errors.put("phone", "Số điện thoại gồm 8 – 14 chữ số, có thể bắt đầu bằng +");
        }
        Gender gender = parseGender(form.gender());
        if (gender == null) {
            errors.put("gender", "Vui lòng chọn giới tính");
        }
        LocalDate dob = null;
        String dobText = trim(form.dob());
        if (dobText.isEmpty()) {
            errors.put("dob", "Vui lòng nhập ngày sinh");
        } else {
            try {
                dob = LocalDate.parse(dobText);
                if (!dob.isBefore(LocalDate.now(clock)) || dob.isBefore(MIN_DOB)) {
                    errors.put("dob", "Ngày sinh không hợp lệ");
                }
            } catch (DateTimeParseException e) {
                errors.put("dob", "Ngày sinh không hợp lệ");
            }
        }
        if (password.length() < PASSWORD_MIN) {
            errors.put("password", "Mật khẩu phải có ít nhất 8 ký tự");
        } else if (password.length() > PASSWORD_MAX) {
            errors.put("password", "Mật khẩu tối đa 128 ký tự");
        } else if (!password.equals(form.confirmPassword())) {
            errors.put("confirmPassword", "Mật khẩu nhập lại không khớp");
        }
        if (!errors.containsKey("userName") || !errors.containsKey("email")) {
            JpaUtil.read(em -> {
                UserDao users = new UserDao(em);
                if (!errors.containsKey("userName") && users.existsByUserName(userName)) {
                    errors.put("userName", "Tên đăng nhập đã được sử dụng");
                }
                if (!errors.containsKey("email") && users.existsByEmail(email)) {
                    errors.put("email", "Email đã được sử dụng");
                }
                return null;
            });
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        Instant expiresAt = clock.instant().plus(VERIFY_LINK_TTL);
        String id = pendingRegistrations.add(new PendingRegistration(
                userName, PasswordHasher.hash(password), email, fullName, gender, dob, phone, expiresAt));
        String link = verifyUrl + "?token=" + SignedToken.create(VERIFY_PURPOSE, id, expiresAt);
        try {
            emailService.send(email, "Xác thực email đăng ký tài khoản Shoppers", verificationEmail(fullName, link));
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
        String value = trim(login);
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

    private static String verificationEmail(String fullName, String link) {
        return "Xin chào " + fullName + ",\n\n"
                + "Cảm ơn bạn đã đăng ký tài khoản tại Shoppers. Bấm vào link dưới đây để xác thực email và hoàn tất đăng ký:\n\n"
                + link + "\n\n"
                + "Link có hiệu lực trong 24 giờ và chỉ dùng được một lần. Nếu bạn không đăng ký, hãy bỏ qua email này.\n\n"
                + "Shoppers";
    }

    private static Gender parseGender(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Gender.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
