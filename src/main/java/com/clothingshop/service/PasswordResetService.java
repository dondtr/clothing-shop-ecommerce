package com.clothingshop.service;

import com.clothingshop.dao.UserDao;
import com.clothingshop.entity.User;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.util.JpaUtil;
import com.clothingshop.util.PasswordHasher;
import com.clothingshop.util.SignedToken;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forgotten password (FR-03): a signed link by email (E9), valid 15 minutes (Q50). The link carries the user id and a
 * fingerprint of the current password hash, so it stops working once the password has changed; nothing is stored.
 */
public class PasswordResetService {

    private static final Logger LOG = LoggerFactory.getLogger(PasswordResetService.class);

    static final Duration LINK_TTL = Duration.ofMinutes(15);
    static final String PURPOSE = "reset-password";
    public static final String MSG_SENT = "Nếu email tồn tại, hệ thống đã gửi hướng dẫn đặt lại mật khẩu vào email đó.";
    static final String MSG_LINK_INVALID = "Link đặt lại mật khẩu không hợp lệ, đã hết hạn hoặc đã được dùng. Vui lòng yêu cầu lại.";

    private final EmailService emailService;
    private final Clock clock;

    public PasswordResetService(EmailService emailService, Clock clock) {
        this.emailService = emailService;
        this.clock = clock;
    }

    /**
     * FR-03 steps 1 – 2: emails a reset link when {@code email} belongs to an account. The page always shows
     * {@link #MSG_SENT}, so it does not reveal which emails have accounts.
     *
     * @param resetUrl absolute URL of the reset page; the signed token is appended as {@code ?token=}
     */
    public void requestReset(String email, String resetUrl) {
        String value = UserRules.trim(email).toLowerCase(Locale.ROOT);
        if (value.isEmpty() || value.length() > 100) {
            return;
        }
        Optional<User> found = JpaUtil.read(em -> new UserDao(em).findByEmail(value));
        found.ifPresent(user -> {
            String subject = user.getId() + "." + fingerprint(user.getPassword());
            String link = resetUrl + "?token=" + SignedToken.create(PURPOSE, subject, clock.instant().plus(LINK_TTL));
            Emails.Message message = Emails.passwordReset(user.getFullName(), link);
            emailService.send(user.getEmail(), message.subject(), message.body());
            LOG.info("Password reset link sent: userId={}", user.getId());
        });
    }

    /** Whether the link can still be used, to show the form or an error first. */
    public boolean isUsable(String token) {
        return JpaUtil.read(em -> findUser(new UserDao(em), token).isPresent());
    }

    /** FR-03 step 3: sets the new password (BR-20); the link becomes unusable because the fingerprint changes. */
    public void resetPassword(String token, String password, String confirmPassword) {
        JpaUtil.inTransaction(em -> {
            User user = findUser(new UserDao(em), token).orElseThrow(() -> new BusinessException(MSG_LINK_INVALID));
            Map<String, String> errors = new LinkedHashMap<>();
            String value = UserRules.newPassword(password, confirmPassword, "password", "confirmPassword", errors);
            if (!errors.isEmpty()) {
                throw new ValidationException(errors);
            }
            user.setPassword(PasswordHasher.hash(value));
            LOG.info("Password reset: userId={}", user.getId());
            return null;
        });
    }

    private Optional<User> findUser(UserDao users, String token) {
        Optional<String> subject = SignedToken.verify(PURPOSE, token, clock.instant());
        if (subject.isEmpty()) {
            return Optional.empty();
        }
        int dot = subject.get().indexOf('.');
        if (dot <= 0) {
            return Optional.empty();
        }
        String fingerprint = subject.get().substring(dot + 1);
        try {
            int userId = Integer.parseInt(subject.get().substring(0, dot));
            return users.findById(userId).filter(u -> MessageDigest.isEqual(
                    fingerprint(u.getPassword()).getBytes(StandardCharsets.UTF_8), fingerprint.getBytes(StandardCharsets.UTF_8)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** A short digest of the stored password hash; changes whenever the password changes. */
    static String fingerprint(String passwordHash) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(passwordHash.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest).substring(0, 22);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
