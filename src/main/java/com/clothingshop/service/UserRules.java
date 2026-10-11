package com.clothingshop.service;

import com.clothingshop.dao.UserDao;
import com.clothingshop.entity.Gender;
import com.clothingshop.util.JpaUtil;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Rules for account fields, shared by registration (FR-01), password reset (FR-03), profile (FR-04), password change
 * (FR-05) and employee accounts (FR-40). Lengths follow the ERD; each method adds a Vietnamese message under the form
 * field name when the value breaks a rule and returns the normalized value.
 */
final class UserRules {

    static final int PASSWORD_MIN = 8;      // BR-20
    static final int PASSWORD_MAX = 128;    // giới hạn kỹ thuật, tránh chuỗi quá dài khi băm

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{8,14}$");
    private static final Pattern WHITESPACE = Pattern.compile("\\s");
    private static final LocalDate MIN_DOB = LocalDate.of(1900, 1, 1);

    private UserRules() {
    }

    /** BR-17: at most 50 characters, without @ or spaces so it cannot be mistaken for an email when signing in. */
    static String userName(String raw, Map<String, String> errors) {
        String value = trim(raw);
        if (value.isEmpty()) {
            errors.put("userName", "Vui lòng nhập tên đăng nhập");
        } else if (value.length() > 50) {
            errors.put("userName", "Tên đăng nhập tối đa 50 ký tự");
        } else if (value.contains("@") || WHITESPACE.matcher(value).find()) {
            errors.put("userName", "Tên đăng nhập không được chứa @ hoặc khoảng trắng");
        }
        return value;
    }

    /** Stored in lower case (BR-17). */
    static String email(String raw, Map<String, String> errors) {
        String value = trim(raw).toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            errors.put("email", "Vui lòng nhập email");
        } else if (value.length() > 100) {
            errors.put("email", "Email tối đa 100 ký tự");
        } else if (!EMAIL.matcher(value).matches()) {
            errors.put("email", "Email không hợp lệ");
        }
        return value;
    }

    static String fullName(String raw, Map<String, String> errors) {
        String value = trim(raw);
        if (value.isEmpty()) {
            errors.put("fullName", "Vui lòng nhập họ và tên");
        } else if (value.length() > 100) {
            errors.put("fullName", "Họ và tên tối đa 100 ký tự");
        }
        return value;
    }

    static String phone(String raw, Map<String, String> errors) {
        String value = trim(raw);
        if (value.isEmpty()) {
            errors.put("phone", "Vui lòng nhập số điện thoại");
        } else if (!PHONE.matcher(value).matches()) {
            errors.put("phone", "Số điện thoại gồm 8 – 14 chữ số, có thể bắt đầu bằng +");
        }
        return value;
    }

    static Gender gender(String raw, Map<String, String> errors) {
        try {
            return Gender.valueOf(trim(raw));
        } catch (IllegalArgumentException e) {
            errors.put("gender", "Vui lòng chọn giới tính");
            return null;
        }
    }

    /** A date in ISO format (as sent by an HTML date input), before today and not before 1900. */
    static LocalDate dob(String raw, LocalDate today, Map<String, String> errors) {
        String value = trim(raw);
        if (value.isEmpty()) {
            errors.put("dob", "Vui lòng nhập ngày sinh");
            return null;
        }
        try {
            LocalDate dob = LocalDate.parse(value);
            if (!dob.isBefore(today) || dob.isBefore(MIN_DOB)) {
                errors.put("dob", "Ngày sinh không hợp lệ");
            }
            return dob;
        } catch (DateTimeParseException e) {
            errors.put("dob", "Ngày sinh không hợp lệ");
            return null;
        }
    }

    /** BR-20: a new password of 8 – 128 characters, typed twice. */
    static String newPassword(String password, String confirm, String field, String confirmField, Map<String, String> errors) {
        String value = password == null ? "" : password;
        if (value.length() < PASSWORD_MIN) {
            errors.put(field, "Mật khẩu phải có ít nhất 8 ký tự");
        } else if (value.length() > PASSWORD_MAX) {
            errors.put(field, "Mật khẩu tối đa 128 ký tự");
        } else if (!value.equals(confirm)) {
            errors.put(confirmField, "Mật khẩu nhập lại không khớp");
        }
        return value;
    }

    /** BR-17, BR-27: user name and email are unique over every account, customers and employees alike. */
    static void checkUnique(String userName, String email, Map<String, String> errors) {
        if (errors.containsKey("userName") && errors.containsKey("email")) {
            return;
        }
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

    static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
