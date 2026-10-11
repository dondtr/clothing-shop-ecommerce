package com.clothingshop.service;

import com.clothingshop.dao.UserDao;
import com.clothingshop.dto.ProfileForm;
import com.clothingshop.dto.ProfileView;
import com.clothingshop.entity.Customer;
import com.clothingshop.entity.Employee;
import com.clothingshop.entity.EmployeeRole;
import com.clothingshop.entity.Gender;
import com.clothingshop.entity.User;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ConflictException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.util.JpaUtil;
import com.clothingshop.util.PasswordHasher;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The signed-in user's own information (FR-04) and password change (FR-05), for customers and employees alike.
 */
public class ProfileService {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileService.class);

    static final String MSG_WRONG_PASSWORD = "Mật khẩu hiện tại không đúng";

    private final Clock clock;

    public ProfileService(Clock clock) {
        this.clock = clock;
    }

    /** FR-04 step 1. */
    public ProfileView get(int userId) {
        return JpaUtil.read(em -> view(load(new UserDao(em), userId)));
    }

    /**
     * FR-04 step 2: saves full name, gender, date of birth and phone. User name, email, level and role cannot be changed
     * here (Q35, Q50). A form opened before someone else saved the account is refused (section 3.1).
     */
    public ProfileView update(int userId, ProfileForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        String fullName = UserRules.fullName(form.fullName(), errors);
        Gender gender = UserRules.gender(form.gender(), errors);
        LocalDate dob = UserRules.dob(form.dob(), LocalDate.now(clock), errors);
        String phone = UserRules.phone(form.phone(), errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return JpaUtil.inTransaction(em -> {
            User user = load(new UserDao(em), userId);
            ConflictException.check(user.getVersion(), form.version());
            user.setFullName(fullName);
            user.setGender(gender);
            user.setDob(dob);
            user.setPhone(phone);
            em.flush();   // increments the version before it is shown again
            return view(user);
        });
    }

    /** FR-05: checks the current password, then saves the new one (BR-20) as a hash. */
    public void changePassword(int userId, String currentPassword, String newPassword, String confirmPassword) {
        Map<String, String> errors = new LinkedHashMap<>();
        String value = UserRules.newPassword(newPassword, confirmPassword, "newPassword", "confirmPassword", errors);
        JpaUtil.inTransaction(em -> {
            User user = load(new UserDao(em), userId);
            if (currentPassword == null || !PasswordHasher.verify(currentPassword, user.getPassword())) {
                errors.put("currentPassword", MSG_WRONG_PASSWORD);
            }
            if (!errors.isEmpty()) {
                throw new ValidationException(errors);
            }
            user.setPassword(PasswordHasher.hash(value));
            return null;
        });
        LOG.info("Password changed: userId={}", userId);
    }

    private static User load(UserDao users, int userId) {
        return users.findById(userId).orElseThrow(() -> new BusinessException("Tài khoản không tồn tại"));
    }

    private static ProfileView view(User user) {
        String role = "Khách hàng";
        if (user instanceof Employee employee) {
            role = employee.getRole() == EmployeeRole.ADMIN ? "Quản lý" : "Nhân viên";
        }
        return new ProfileView(user.getId(), user.getUserName(), user.getEmail(), user.getFullName(), user.getGender(),
                user.getDob(), user.getPhone(), role, user instanceof Customer c ? c.getCustomerLevel() : null, user.getVersion());
    }
}
