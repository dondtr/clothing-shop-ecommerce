package com.clothingshop.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends emails on a background thread, so a slow or unavailable mail server neither delays requests and scheduled jobs
 * nor undoes a business change that is already saved. Failures are logged. Used for the notifications E1 – E7 and E9;
 * registration (E8) sends directly because the customer must know when the link could not be sent.
 */
public class AsyncEmailService implements EmailService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncEmailService.class);

    private final EmailService delegate;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "email-sender");
        thread.setDaemon(true);
        return thread;
    });

    public AsyncEmailService(EmailService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void send(String to, String subject, String body) {
        executor.submit(() -> {
            try {
                delegate.send(to, subject, body);
            } catch (RuntimeException e) {
                LOG.error("Email \"{}\" could not be sent to {}", subject, to, e);
            }
        });
    }

    /** Sends what is queued (waiting up to 10 seconds), then stops the thread. */
    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                LOG.warn("Emails still queued at shutdown were not sent");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
