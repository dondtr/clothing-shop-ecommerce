package com.clothingshop.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    @Test
    void verifiesTheRightPasswordOnly() {
        String stored = PasswordHasher.hash("matkhau-123");
        assertTrue(stored.startsWith("pbkdf2-sha256$600000$"));
        assertTrue(stored.length() <= 255, "must fit User.password VARCHAR(255)");
        assertTrue(PasswordHasher.verify("matkhau-123", stored));
        assertFalse(PasswordHasher.verify("matkhau-124", stored));
        assertFalse(PasswordHasher.verify("", stored));
    }

    @Test
    void usesARandomSalt() {
        assertNotEquals(PasswordHasher.hash("cung-mat-khau"), PasswordHasher.hash("cung-mat-khau"));
    }

    @Test
    void malformedStoredValuesNeverMatch() {
        assertFalse(PasswordHasher.verify("x", null));
        assertFalse(PasswordHasher.verify(null, "pbkdf2-sha256$1$AA==$AA=="));
        assertFalse(PasswordHasher.verify("x", "plain-text"));
        assertFalse(PasswordHasher.verify("x", "pbkdf2-sha256$abc$%%$%%"));
    }
}
