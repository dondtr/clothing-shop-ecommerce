package com.clothingshop.util;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application settings kept outside the repository (database, mail, secret key).
 *
 * <p>A key such as {@code db.url} is looked up in this order:
 * <ol>
 *     <li>environment variable {@code CLOTHINGSHOP_DB_URL};</li>
 *     <li>JVM system property {@code clothingshop.db.url};</li>
 *     <li>the properties file named by {@code CLOTHINGSHOP_CONFIG} / {@code clothingshop.config}, or
 *         {@code ~/.clothingshop/clothingshop.properties} when that file exists.</li>
 * </ol>
 * Templates for local MySQL and Aiven are in {@code config/*.properties.example}.
 */
public final class AppConfig {

    private static final Logger LOG = LoggerFactory.getLogger(AppConfig.class);
    private static final String ENV_PREFIX = "CLOTHINGSHOP_";
    private static final String PROPERTY_PREFIX = "clothingshop.";

    private static volatile Properties fileProperties;

    private AppConfig() {
    }

    /** Returns the trimmed value of {@code key}, or empty when it is not set anywhere. */
    public static Optional<String> get(String key) {
        String value = System.getenv(ENV_PREFIX + key.toUpperCase().replace('.', '_'));
        if (isBlank(value)) {
            value = System.getProperty(PROPERTY_PREFIX + key);
        }
        if (isBlank(value)) {
            value = fileProperties().getProperty(key);
        }
        return isBlank(value) ? Optional.empty() : Optional.of(value.trim());
    }

    public static String get(String key, String defaultValue) {
        return get(key).orElse(defaultValue);
    }

    /** Returns the value of {@code key}; fails with a message naming every place the setting can come from. */
    public static String require(String key) {
        return get(key).orElseThrow(() -> new IllegalStateException("Missing setting '" + key + "': set environment variable "
                + ENV_PREFIX + key.toUpperCase().replace('.', '_') + ", system property " + PROPERTY_PREFIX + key
                + ", or add it to the configuration file (see config/clothingshop.properties.example)"));
    }

    private static Properties fileProperties() {
        Properties properties = fileProperties;
        if (properties == null) {
            synchronized (AppConfig.class) {
                properties = fileProperties;
                if (properties == null) {
                    properties = loadFile();
                    fileProperties = properties;
                }
            }
        }
        return properties;
    }

    private static Properties loadFile() {
        Properties properties = new Properties();
        String configured = System.getenv(ENV_PREFIX + "CONFIG");
        if (isBlank(configured)) {
            configured = System.getProperty(PROPERTY_PREFIX + "config");
        }
        Path path = isBlank(configured)
                ? Path.of(System.getProperty("user.home"), ".clothingshop", "clothingshop.properties")
                : Path.of(configured.trim());
        if (!Files.isRegularFile(path)) {
            if (!isBlank(configured)) {
                throw new IllegalStateException("Configuration file not found: " + path);
            }
            return properties;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
            LOG.info("Loaded configuration file {}", path);
            return properties;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read configuration file " + path, e);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
