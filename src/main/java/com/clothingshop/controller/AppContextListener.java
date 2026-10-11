package com.clothingshop.controller;

import com.clothingshop.service.AccountService;
import com.clothingshop.service.AddressService;
import com.clothingshop.service.AsyncEmailService;
import com.clothingshop.service.CartService;
import com.clothingshop.service.CatalogService;
import com.clothingshop.service.EmailService;
import com.clothingshop.service.InMemoryCartStore;
import com.clothingshop.service.PasswordResetService;
import com.clothingshop.service.PendingRegistrationStore;
import com.clothingshop.service.ProfileService;
import com.clothingshop.service.WishlistService;
import com.clothingshop.util.JpaUtil;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.time.Clock;
import java.time.ZoneId;

/**
 * Creates the shared services when the application starts and releases resources when it stops.
 * Services are stored as servlet context attributes, keyed by class name, so every servlet uses the same instances
 * (pending registrations and carts are kept in memory and must be shared by every request).
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    /** Business time is Vietnam time (specification section 3.1). */
    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private AsyncEmailService notifications;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        Clock clock = Clock.system(ZONE);
        ServletContext context = event.getServletContext();
        EmailService mail = EmailService.fromConfig();
        notifications = new AsyncEmailService(mail);

        register(context, new AccountService(mail, new PendingRegistrationStore(clock), clock));
        register(context, new PasswordResetService(notifications, clock));
        register(context, new ProfileService(clock));
        register(context, new AddressService());
        register(context, new CatalogService(clock));
        register(context, new CartService(new InMemoryCartStore(clock), clock));
        register(context, new WishlistService(clock));
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (notifications != null) {
            notifications.close();
        }
        JpaUtil.close();
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
