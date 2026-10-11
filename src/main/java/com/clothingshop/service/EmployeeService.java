package com.clothingshop.service;

import com.clothingshop.dto.EmployeeForm;
import com.clothingshop.dto.EmployeeRow;
import com.clothingshop.entity.Employee;
import com.clothingshop.entity.EmployeeRole;
import com.clothingshop.entity.Gender;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.util.AppConfig;
import com.clothingshop.util.JpaUtil;
import com.clothingshop.util.PasswordHasher;
import jakarta.persistence.PersistenceException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Employee accounts: the manager creates STAFF accounts (FR-40) and locks or unlocks them (FR-41). The single manager
 * account is created at deployment from the configuration (BR-24, {@link #ensureAdmin()}).
 */
public class EmployeeService {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeService.class);

    private final Clock clock;

    public EmployeeService(Clock clock) {
        this.clock = clock;
    }

    public List<EmployeeRow> list() {
        return JpaUtil.read(em -> em.createQuery("select e from Employee e order by e.role desc, e.hireDate desc, e.id desc",
                        Employee.class).getResultList().stream()
                .map(e -> new EmployeeRow(e.getId(), e.getUserName(), e.getFullName(), e.getEmail(), e.getPhone(), e.getRole(),
                        e.isActive(), e.getHireDate()))
                .toList());
    }

    /**
     * FR-40: creates a STAFF account (never another manager, BR-24) with the same checks as registration; user name and
     * email must not be used by any account, customer or employee (BR-17, BR-27).
     */
    public int create(EmployeeForm form) {
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
        try {
            int id = JpaUtil.inTransaction(em -> {
                Employee e = new Employee(userName, PasswordHasher.hash(password), fullName, gender, dob, email, phone,
                        LocalDate.now(clock), EmployeeRole.STAFF);
                em.persist(e);
                return e.getId();
            });
            LOG.info("Staff account created: id={}, userName={}", id, userName);
            return id;
        } catch (PersistenceException e) {
            throw new BusinessException("Tên đăng nhập hoặc email vừa được tài khoản khác sử dụng");
        }
    }

    /** FR-41: locks (active = FALSE) or unlocks a STAFF account; the manager account cannot be locked. */
    public void setActive(int employeeId, boolean active) {
        JpaUtil.inTransaction(em -> {
            Employee e = em.find(Employee.class, employeeId);
            if (e == null) {
                throw new BusinessException("Nhân viên không tồn tại");
            }
            if (e.getRole() != EmployeeRole.STAFF) {
                throw new BusinessException("Không khóa được tài khoản quản lý");
            }
            e.setActive(active);
            LOG.info("Staff account {}: id={}", active ? "unlocked" : "locked", employeeId);
            return null;
        });
    }

    /** Whether the employee may still use the staff area (a locked account loses access at once, BR-18). */
    public boolean isActive(int employeeId) {
        return JpaUtil.read(em -> Optional.ofNullable(em.find(Employee.class, employeeId)).map(Employee::isActive).orElse(false));
    }

    /**
     * BR-24: creates the single manager account at deployment when there is none yet, from {@code admin.userName},
     * {@code admin.email}, {@code admin.password}, {@code admin.fullName}, {@code admin.phone} and optionally
     * {@code admin.gender} (default OTHER) and {@code admin.dob} (default 1990-01-01). Passwords are never in the code.
     */
    public void ensureAdmin() {
        boolean exists = JpaUtil.read(em -> em.createQuery("select count(e) from Employee e where e.role = :r", Long.class)
                .setParameter("r", EmployeeRole.ADMIN).getSingleResult() > 0);
        if (exists) {
            return;
        }
        Optional<String> userName = AppConfig.get("admin.userName");
        if (userName.isEmpty()) {
            LOG.warn("No manager account yet: set admin.userName, admin.email, admin.password, admin.fullName and admin.phone"
                    + " in the configuration and restart to create it");
            return;
        }
        Map<String, String> errors = new LinkedHashMap<>();
        String name = UserRules.userName(userName.get(), errors);
        String email = UserRules.email(AppConfig.get("admin.email", ""), errors);
        String fullName = UserRules.fullName(AppConfig.get("admin.fullName", ""), errors);
        String phone = UserRules.phone(AppConfig.get("admin.phone", ""), errors);
        Gender gender = UserRules.gender(AppConfig.get("admin.gender", "OTHER"), errors);
        LocalDate dob = UserRules.dob(AppConfig.get("admin.dob", "1990-01-01"), LocalDate.now(clock), errors);
        String password = AppConfig.get("admin.password", "");
        UserRules.newPassword(password, password, "admin.password", "admin.password", errors);
        UserRules.checkUnique(name, email, errors);
        if (!errors.isEmpty()) {
            LOG.error("Manager account not created, invalid admin.* configuration: {}", errors);
            return;
        }
        JpaUtil.inTransaction(em -> {
            em.persist(new Employee(name, PasswordHasher.hash(password), fullName, gender, dob, email, phone,
                    LocalDate.now(clock), EmployeeRole.ADMIN));
            return null;
        });
        LOG.info("Manager account created: userName={}", name);
    }
}
