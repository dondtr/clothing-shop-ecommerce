package com.clothingshop.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Holds the application's single {@link EntityManagerFactory} for the persistence unit {@value #PERSISTENCE_UNIT}
 * and runs units of work for the service layer.
 *
 * <p>Connection settings are not stored in the repository; they are read through {@link AppConfig}:
 * <ul>
 *     <li>{@code db.url} (required), {@code db.username} (required), {@code db.password} (optional);</li>
 *     <li>{@code db.pool.maxSize} (optional, default 10 from persistence.xml).</li>
 * </ul>
 */
public final class JpaUtil {

    public static final String PERSISTENCE_UNIT = "clothingShopPU";

    private static volatile EntityManagerFactory entityManagerFactory;

    private JpaUtil() {
    }

    /** Returns the shared factory, creating it from {@link AppConfig} on first use. */
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

    /**
     * Creates the shared factory with explicit settings instead of {@link AppConfig}. Used by automated tests to
     * point the persistence unit at an in-memory database; the application itself never calls it.
     */
    public static synchronized void initialize(Map<String, Object> settings) {
        close();
        entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, settings);
    }

    /** Closes the shared factory; called when the web application stops. */
    public static synchronized void close() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
        entityManagerFactory = null;
    }

    /** Runs {@code work} in one transaction: commits when it returns, rolls back when it throws. */
    public static <T> T inTransaction(Function<EntityManager, T> work) {
        EntityManager em = getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T result = work.apply(em);
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    /** Runs read-only {@code work} with a new entity manager, without a transaction. */
    public static <T> T read(Function<EntityManager, T> work) {
        EntityManager em = getEntityManagerFactory().createEntityManager();
        try {
            return work.apply(em);
        } finally {
            em.close();
        }
    }

    static Map<String, Object> connectionSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("jakarta.persistence.jdbc.url", AppConfig.require("db.url"));
        settings.put("jakarta.persistence.jdbc.user", AppConfig.require("db.username"));
        settings.put("jakarta.persistence.jdbc.password", AppConfig.get("db.password", ""));
        AppConfig.get("db.pool.maxSize").ifPresent(size -> settings.put("hibernate.hikari.maximumPoolSize", size));
        return settings;
    }
}
