package com.clothingshop.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SignedTokenTest {

    private final Instant now = Instant.parse("2026-10-10T08:00:00Z");

    @Test
    void returnsTheSubjectOfAValidToken() {
        String token = SignedToken.create("verify-email", "abc123", now.plus(Duration.ofHours(24)));
        assertEquals(Optional.of("abc123"), SignedToken.verify("verify-email", token, now));
    }

    @Test
    void rejectsExpiredTokens() {
        String token = SignedToken.create("verify-email", "abc123", now.plus(Duration.ofHours(24)));
        assertTrue(SignedToken.verify("verify-email", token, now.plus(Duration.ofHours(25))).isEmpty());
    }

    @Test
    void rejectsATokenMadeForAnotherPurpose() {
        String token = SignedToken.create("reset-password", "abc123", now.plusSeconds(60));
        assertTrue(SignedToken.verify("verify-email", token, now).isEmpty());
    }

    @Test
    void rejectsTamperedOrMalformedTokens() {
        String token = SignedToken.create("verify-email", "abc123", now.plusSeconds(60));
        String forged = SignedToken.create("verify-email", "someone-else", now.plusSeconds(60));
        String tampered = forged.substring(0, forged.indexOf('.')) + token.substring(token.indexOf('.'));
        assertTrue(SignedToken.verify("verify-email", tampered, now).isEmpty());
        assertTrue(SignedToken.verify("verify-email", "not-a-token", now).isEmpty());
        assertTrue(SignedToken.verify("verify-email", "a.b.c", now).isEmpty());
        assertTrue(SignedToken.verify("verify-email", "%%%.%%%", now).isEmpty());
        assertTrue(SignedToken.verify("verify-email", null, now).isEmpty());
    }

    @Test
    void refusesSeparatorInsideValues() {
        assertThrows(IllegalArgumentException.class, () -> SignedToken.create("verify-email", "a|b", now));
    }
}
