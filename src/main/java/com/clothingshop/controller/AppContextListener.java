package com.clothingshop.controller;

import com.clothingshop.service.AccountService;
import com.clothingshop.service.AddressService;
import com.clothingshop.service.AsyncEmailService;
import com.clothingshop.service.CartService;
import com.clothingshop.service.CatalogService;
import com.clothingshop.service.CheckoutService;
import com.clothingshop.service.CustomerService;
import com.clothingshop.service.DashboardService;
import com.clothingshop.service.DeliveryLinks;
import com.clothingshop.service.DeliveryService;
import com.clothingshop.service.EmailService;
import com.clothingshop.service.EmployeeService;
import com.clothingshop.service.InMemoryCartStore;
import com.clothingshop.service.OrderQueries;
import com.clothingshop.service.OrderService;
import com.clothingshop.service.PasswordResetService;
import com.clothingshop.service.PaymentService;
import com.clothingshop.service.PendingRegistrationStore;
import com.clothingshop.service.ProfileService;
import com.clothingshop.service.Scheduler;
import com.clothingshop.service.VnpayClient;
import com.clothingshop.service.VnpayGateway;
import com.clothingshop.service.VoucherService;
import com.clothingshop.service.WishlistService;
import com.clothingshop.util.AppConfig;
import com.clothingshop.util.JpaUtil;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the shared services when the application starts and releases resources when it stops.
 * Services are stored as servlet context attributes, keyed by class name, so every servlet uses the same instances
 * (pending registrations and carts are kept in memory and must be shared by every request).
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(AppContextListener.class);

    /** Business time is Vietnam time (specification section 3.1). */
    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private AsyncEmailService notifications;
    private Scheduler scheduler;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        Clock clock = Clock.system(ZONE);
        ServletContext context = event.getServletContext();
        EmailService mail = EmailService.fromConfig();
        notifications = new AsyncEmailService(mail);

        register(context, new AccountService(mail, new PendingRegistrationStore(clock), clock));
        register(context, new PasswordResetService(notifications, clock));
        register(context, new ProfileService(clock));
        AddressService addresses = new AddressService();
        register(context, addresses);
        register(context, new CatalogService(clock));
        CartService cart = new CartService(new InMemoryCartStore(clock), clock);
        register(context, cart);
        register(context, new VoucherService(clock));
        VnpayGateway vnpay = VnpayClient.fromConfig(clock).orElse(null);
        PaymentService payments = new PaymentService(vnpay, cart, clock,
                Boolean.parseBoolean(AppConfig.get("vnpay.processReturnUrl", "false")));
        register(context, payments);
        register(context, new CheckoutService(cart, addresses, clock, vnpay != null));
        if (vnpay == null) {
            LOG.info("VNPAY is not configured (vnpay.tmnCode, vnpay.hashSecret): checkout offers COD only");
        }
        DeliveryLinks links = new DeliveryLinks(baseUrl(context));
        register(context, links);
        register(context, new OrderQueries(clock));
        OrderService orders = new OrderService(payments, notifications, links, clock);
        register(context, orders);
        DeliveryService delivery = new DeliveryService(notifications, links, clock);
        register(context, delivery);
        register(context, new WishlistService(clock));
        register(context, new CustomerService());
        EmployeeService employees = new EmployeeService(clock);
        register(context, employees);
        register(context, new DashboardService());

        if (Boolean.parseBoolean(AppConfig.get("scheduler.enabled", "true"))) {
            scheduler = new Scheduler(clock);
            scheduler.everyMinute("J7 VNPAY reconciliation", payments::reconcilePending);
            scheduler.daily("J1 reminder after 3 days", delivery::sendReminders, false);
            scheduler.daily("J2 automatic completion", orders::autoComplete, true);
            scheduler.daily("J3 COD orders not shipped in 15 days", orders::autoCancelCod, true);
        } else {
            LOG.warn("Scheduled tasks J1 - J7 are disabled (scheduler.enabled=false)");
        }

        try {
            employees.ensureAdmin();
        } catch (RuntimeException e) {
            // The shop can still start (for example while the database is down); the account is created on a later start
            LOG.error("Could not check or create the manager account at start-up", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (scheduler != null) {
            scheduler.close();
        }
        if (notifications != null) {
            notifications.close();
        }
        JpaUtil.close();
        releaseJdbcDriver();
    }

    /**
     * Lets Tomcat unload the application cleanly when it is redeployed: stops the MySQL driver's clean-up thread and
     * deregisters the JDBC drivers loaded by this application, which would otherwise keep the old class loader alive.
     */
    private static void releaseJdbcDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.AbandonedConnectionCleanupThread").getMethod("checkedShutdown").invoke(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            LOG.debug("MySQL clean-up thread not stopped", e);
        }
        ClassLoader own = AppContextListener.class.getClassLoader();
        for (Driver driver : Collections.list(DriverManager.getDrivers())) {
            if (driver.getClass().getClassLoader() == own) {
                try {
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException e) {
                    LOG.warn("Could not deregister JDBC driver {}", driver, e);
                }
            }
        }
    }

    /**
     * Absolute address used in links sent by email from scheduled tasks, where there is no request: {@code app.baseUrl},
     * or http://localhost:8080 with the context path during development.
     */
    private static String baseUrl(ServletContext context) {
        return AppConfig.get("app.baseUrl").orElseGet(() -> {
            String fallback = "http://localhost:8080" + context.getContextPath();
            LOG.warn("app.baseUrl is not set; links in emails use {}", fallback);
            return fallback;
        });
    }

    private static void register(ServletContext context, Object service) {
        context.setAttribute(service.getClass().getName(), service);
    }

    static <T> T service(ServletContext context, Class<T> type) {
        return type.cast(context.getAttribute(type.getName()));
    }

    static AccountService accountService(ServletContext context) {
        return service(context, AccountService.class);
    }

    static PasswordResetService passwordResetService(ServletContext context) {
        return service(context, PasswordResetService.class);
    }

    static ProfileService profileService(ServletContext context) {
        return service(context, ProfileService.class);
    }

    static AddressService addressService(ServletContext context) {
        return service(context, AddressService.class);
    }

    static CatalogService catalogService(ServletContext context) {
        return service(context, CatalogService.class);
    }

    static CartService cartService(ServletContext context) {
        return service(context, CartService.class);
    }

    static WishlistService wishlistService(ServletContext context) {
        return service(context, WishlistService.class);
    }
}
