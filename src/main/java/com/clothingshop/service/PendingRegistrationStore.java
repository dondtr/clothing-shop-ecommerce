package com.clothingshop.service;

import com.clothingshop.entity.Gender;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registrations waiting for email verification (FR-01). Kept in server memory, not in the database: an account is
 * written only after verification (Q35), and entries are lost when the server restarts (accepted in Q50).
 */
public class PendingRegistrationStore {

    /** Upper bound so that mass sign-ups cannot exhaust memory. */
    static final int MAX_ENTRIES = 10_000;

    /** Registration data, with the password already hashed. */
    public record PendingRegistration(String userName, String passwordHash, String email, String fullName,
                                      Gender gender, LocalDate dob, String phone, Instant expiresAt) {
    }

    private final Map<String, PendingRegistration> entries = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public PendingRegistrationStore(Clock clock) {
        this.clock = clock;
    }

    /** Stores the registration and returns its random id (used as the subject of the verification link). */
    public String add(PendingRegistration registration) {
        removeExpired();
        if (entries.size() >= MAX_ENTRIES) {
            throw new IllegalStateException("Too many pending registrations");
        }
        byte[] bytes = new byte[18];
        random.nextBytes(bytes);
        String id = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        entries.put(id, registration);
        return id;
    }

    /** Removes and returns the registration if it exists and has not expired; a link can be used only once. */
    public Optional<PendingRegistration> take(String id) {
        PendingRegistration registration = entries.remove(id);
        if (registration == null || clock.instant().isAfter(registration.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(registration);
    }

    int size() {
        return entries.size();
    }

    private void removeExpired() {
        Instant now = clock.instant();
        entries.values().removeIf(r -> now.isAfter(r.expiresAt()));
    }
}
