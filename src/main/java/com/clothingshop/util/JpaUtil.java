package com.clothingshop.util;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds the application's single {@link EntityManagerFactory} for the persistence unit {@value #PERSISTENCE_UNIT}.
 *
 * <p>Database connection settings are not stored in the repository. They are read, when the factory is first
 * created, from environment variables or, if a variable is absent, from JVM system properties:
 * <ul>
 *     <li>{@code CLOTHINGSHOP_DB_URL} / {@code clothingshop.db.url} (required)</li>
 *     <li>{@code CLOTHINGSHOP_DB_USERNAME} / {@code clothingshop.db.username} (required)</li>
 *     <li>{@code CLOTHINGSHOP_DB_PASSWORD} / {@code clothingshop.db.password} (optional, empty if absent)</li>
 * </ul>
 */
public final class JpaUtil {

    public static final String PERSISTENCE_UNIT = "clothingShopPU";

    private static volatile EntityManagerFactory entityManagerFactory;

    private JpaUtil() {
    }

    /** Returns the shared factory, creating it on first use. */
    public static EntityManagerFactory getEntityManagerFactory() {
        EntityManagerFactory factory = entityManagerFactory;
        if (factory == null) {
            synchronized (JpaUtil.class) {
                factory = entityManagerFactory;
                if (factory == null) {
                    factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, connectionSettings());
                    entityManagerFactory = factory;
                }
            }
        }
        return factory;
    }

    /** Closes the shared factory; intended to be called when the web application stops. */
    public static synchronized void close() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
        entityManagerFactory = null;
    }

    static Map<String, Object> connectionSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("jakarta.persistence.jdbc.url", read("CLOTHINGSHOP_DB_URL", "clothingshop.db.url", true));
        settings.put("jakarta.persistence.jdbc.user", read("CLOTHINGSHOP_DB_USERNAME", "clothingshop.db.username", true));
        settings.put("jakarta.persistence.jdbc.password", read("CLOTHINGSHOP_DB_PASSWORD", "clothingshop.db.password", false));
        return settings;
    }

    private static String read(String environmentVariable, String systemProperty, boolean required) {
        String value = System.getenv(environmentVariable);
        if (value == null || value.isBlank()) {
            value = System.getProperty(systemProperty);
        }
        if (value == null || value.isBlank()) {
            if (required) {
                throw new IllegalStateException("Missing database setting: set environment variable "
                        + environmentVariable + " or system property " + systemProperty);
            }
            return "";
        }
        return value;
    }
}
