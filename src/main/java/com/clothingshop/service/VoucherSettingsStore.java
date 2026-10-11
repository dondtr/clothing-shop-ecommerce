package com.clothingshop.service;

import com.clothingshop.util.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VoucherSettings.load() / save() (section 3.5, Q48): the JSON file is read once at start-up and kept in memory; each
 * save writes a temporary file and renames it over the old one, so a crash while writing never leaves a broken file.
 * A missing file is created with the default values (Q49). File: {@code voucher.settingsFile}, default
 * {@code ~/.clothingshop/voucher-settings.json}.
 */
public class VoucherSettingsStore {

    private static final Logger LOG = LoggerFactory.getLogger(VoucherSettingsStore.class);

    private final Path file;
    private final Clock clock;
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).enable(SerializationFeature.INDENT_OUTPUT);
    private volatile VoucherSettings current;

    public VoucherSettingsStore(Path file, Clock clock) {
        this.file = file;
        this.clock = clock;
        this.current = load();
    }

    public static Path configuredFile() {
        return AppConfig.get("voucher.settingsFile").map(Path::of)
                .orElseGet(() -> Path.of(System.getProperty("user.home"), ".clothingshop", "voucher-settings.json"));
    }

    public VoucherSettings get() {
        return current;
    }

    public Path file() {
        return file;
    }

    /** Writes the settings (temporary file, then rename) and only then uses them. */
    public synchronized void save(VoucherSettings settings) {
        try {
            Path dir = file.toAbsolutePath().getParent();
            Files.createDirectories(dir);
            Path tmp = Files.createTempFile(dir, "voucher-settings", ".tmp");
            try {
                json.writeValue(tmp.toFile(), settings);
                try {
                    Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(tmp);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write voucher settings to " + file, e);
        }
        current = settings;
        LOG.info("Voucher settings saved to {}", file);
    }

    private VoucherSettings load() {
        VoucherSettings defaults = VoucherSettings.defaults(LocalDateTime.now(clock));
        if (!Files.exists(file)) {
            try {
                save(defaults);
            } catch (UncheckedIOException e) {
                LOG.error("Voucher settings file {} could not be created; using the default values in memory", file, e);
            }
            return defaults;
        }
        try {
            return json.readValue(file.toFile(), VoucherSettings.class).completedWith(defaults);
        } catch (IOException e) {
            // Keep the broken file for the manager to inspect; the next save from the settings page replaces it
            LOG.error("Voucher settings file {} cannot be read; using the default values until the settings are saved again", file, e);
            return defaults;
        }
    }
}
