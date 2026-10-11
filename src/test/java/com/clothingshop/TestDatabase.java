package com.clothingshop;

import com.clothingshop.util.JpaUtil;
import java.util.HashMap;
import java.util.Map;

/**
 * Points the persistence unit at an in-memory H2 database for automated tests. Only here is the schema generated
 * by Hibernate; the application never generates or changes the schema of a real database.
 */
public final class TestDatabase {

    private static boolean started;

    private TestDatabase() {
    }

    public static synchronized void start() {
        if (started) {
            return;
        }
        Map<String, Object> settings = new HashMap<>();
        settings.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        settings.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:clothingshop;DB_CLOSE_DELAY=-1");
        settings.put("jakarta.persistence.jdbc.user", "sa");
        settings.put("jakarta.persistence.jdbc.password", "");
        settings.put("jakarta.persistence.schema-generation.database.action", "drop-and-create");
        JpaUtil.initialize(settings);
        started = true;
    }
}
