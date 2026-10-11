package com.clothingshop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clothingshop.entity.Gender;
import com.clothingshop.service.PendingRegistrationStore.PendingRegistration;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class PendingRegistrationStoreTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-10T08:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private final PendingRegistrationStore store = new PendingRegistrationStore(clock);

    private PendingRegistration registration(Duration validFor) {
        return new PendingRegistration("an", "hash", "an@example.com", "An", Gender.MALE, LocalDate.of(2000, 1, 1),
                "0901234567", clock.instant().plus(validFor));
    }

    @Test
    void aRegistrationCanBeTakenOnlyOnce() {
        String id = store.add(registration(Duration.ofHours(24)));
        assertEquals("an", store.take(id).orElseThrow().userName());
        assertTrue(store.take(id).isEmpty());
    }

    @Test
    void expiredRegistrationsAreNotReturnedAndAreCleanedUp() {
        String id = store.add(registration(Duration.ofHours(24)));
        clock.advance(Duration.ofHours(25));
        assertTrue(store.take(id).isEmpty());
        store.add(registration(Duration.ofHours(1)));
        String old = store.add(registration(Duration.ofMinutes(1)));
        clock.advance(Duration.ofMinutes(2));
        store.add(registration(Duration.ofHours(1)));
        assertEquals(2, store.size(), "expired entries are removed when new ones are added");
        assertTrue(store.take(old).isEmpty());
    }
}
