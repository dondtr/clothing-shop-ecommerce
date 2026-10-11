package com.clothingshop.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tamper-proof tokens for links sent by email (specification section 10): a purpose, a subject and an expiry time,
 * signed with HMAC-SHA256 using the server secret {@code app.secret}.
 *
 * <p>Token format: {@code base64url(purpose|subject|expiryEpochSeconds) + "." + base64url(signature)}.
 * When {@code app.secret} is not configured a random key is generated at start-up, so links stop working after a
 * restart; this is acceptable only during development.
 */
public final class SignedToken {

    private static final Logger LOG = LoggerFactory.getLogger(SignedToken.class);
    private static final String HMAC = "HmacSHA256";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private static volatile byte[] key;

    private SignedToken() {
    }

    public static String create(String purpose, String subject, Instant expiresAt) {
        if (purpose.contains("|") || subject.contains("|")) {
            throw new IllegalArgumentException("purpose and subject must not contain '|'");
        }
        byte[] payload = (purpose + "|" + subject + "|" + expiresAt.getEpochSecond()).getBytes(StandardCharsets.UTF_8);
        return ENCODER.encodeToString(payload) + "." + ENCODER.encodeToString(sign(payload));
    }

    /** Returns the subject when the token is well formed, correctly signed, made for {@code purpose} and not expired. */
    public static Optional<String> verify(String purpose, String token, Instant now) {
        if (token == null) {
            return Optional.empty();
        }
        int dot = token.indexOf('.');
        if (dot <= 0 || dot != token.lastIndexOf('.')) {
            return Optional.empty();
        }
        try {
            byte[] payload = DECODER.decode(token.substring(0, dot));
            byte[] signature = DECODER.decode(token.substring(dot + 1));
            if (!MessageDigest.isEqual(signature, sign(payload))) {
                return Optional.empty();
            }
            String[] parts = new String(payload, StandardCharsets.UTF_8).split("\\|", -1);
            if (parts.length != 3 || !parts[0].equals(purpose) || now.getEpochSecond() > Long.parseLong(parts[2])) {
                return Optional.empty();
            }
            return Optional.of(parts[1]);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static byte[] sign(byte[] payload) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(key(), HMAC));
            return mac.doFinal(payload);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }

    private static byte[] key() {
        byte[] k = key;
        if (k == null) {
            synchronized (SignedToken.class) {
                k = key;
                if (k == null) {
                    k = AppConfig.get("app.secret").map(s -> s.getBytes(StandardCharsets.UTF_8)).orElse(null);
                    if (k == null || k.length < 32) {
                        LOG.warn("app.secret is missing or shorter than 32 characters; using a random key that changes on every restart");
                        k = new byte[32];
                        new SecureRandom().nextBytes(k);
                    }
                    key = k;
                }
            }
        }
        return k;
    }
}
