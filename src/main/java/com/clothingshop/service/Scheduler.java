package com.clothingshop.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the automatic tasks J1 – J7 (section 7) on one background thread, in Vietnam time: some every minute (J7), the
 * others every day at 00:00. Every run is logged (section 10). Tasks that check before acting (J2, J3, J5, J6) can also
 * run once shortly after start-up, to catch up a midnight missed while the server was off; J1 and J4 must run exactly
 * once a day, so they never catch up. Disabled with {@code scheduler.enabled=false} (for example on a second server).
 */
public class Scheduler implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);
    private static final Duration CATCH_UP_DELAY = Duration.ofSeconds(30);

    private final Clock clock;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "scheduled-tasks");
        thread.setDaemon(true);
        return thread;
    });

    public Scheduler(Clock clock) {
        this.clock = clock;
    }

    /** Runs {@code task} every minute, starting one minute from now. */
    public void everyMinute(String name, Runnable task) {
        executor.scheduleWithFixedDelay(() -> run(name, task), 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Runs {@code task} every day at 00:00 Vietnam time.
     *
     * @param catchUp also run it once shortly after start-up (only for tasks that check before acting)
     */
    public void daily(String name, Runnable task, boolean catchUp) {
        if (catchUp) {
            executor.schedule(() -> run(name, task), CATCH_UP_DELAY.toSeconds(), TimeUnit.SECONDS);
        }
        scheduleAtMidnight(name, task);
    }

    private void scheduleAtMidnight(String name, Runnable task) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        ZonedDateTime midnight = LocalDate.now(clock).plusDays(1).atStartOfDay(clock.getZone());
        long delay = Duration.between(now, midnight).toMillis();
        executor.schedule(() -> {
            run(name, task);
            scheduleAtMidnight(name, task);   // the next midnight, computed again to avoid drift
        }, delay, TimeUnit.MILLISECONDS);
    }

    private static void run(String name, Runnable task) {
        long start = System.nanoTime();
        try {
            task.run();
            LOG.info("Task {} finished in {} ms", name, (System.nanoTime() - start) / 1_000_000);
        } catch (RuntimeException e) {
            // A failed run must not stop later runs
            LOG.error("Task {} failed", name, e);
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
