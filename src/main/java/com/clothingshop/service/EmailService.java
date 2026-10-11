package com.clothingshop.service;

import com.clothingshop.util.AppConfig;

/**
 * Sends the emails of the specification (section 8). Two implementations: SMTP, and a log-only one for development
 * when no SMTP server is configured.
 */
@FunctionalInterface
public interface EmailService {

    /** Sends a plain-text email; throws {@link IllegalStateException} when it cannot be sent. */
    void send(String to, String subject, String body);

    /** SMTP when {@code mail.smtp.host} is configured, otherwise the log-only implementation. */
    static EmailService fromConfig() {
        return AppConfig.get("mail.smtp.host").isPresent() ? new SmtpEmailService() : new LogEmailService();
    }
}
