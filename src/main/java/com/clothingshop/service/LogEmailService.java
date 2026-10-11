package com.clothingshop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development replacement for SMTP: writes the email to the application log instead of sending it, so that links
 * (for example the verification link of FR-01) can be opened from the log. Used when {@code mail.smtp.host} is empty.
 */
public class LogEmailService implements EmailService {

    private static final Logger LOG = LoggerFactory.getLogger(LogEmailService.class);

    public LogEmailService() {
        LOG.warn("mail.smtp.host is not configured: emails are written to this log instead of being sent");
    }

    @Override
    public void send(String to, String subject, String body) {
        LOG.info("EMAIL (not sent, no SMTP configured)\nTo: {}\nSubject: {}\n\n{}", to, subject, body);
    }
}
