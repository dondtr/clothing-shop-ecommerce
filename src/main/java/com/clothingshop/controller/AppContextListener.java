package com.clothingshop.controller;

import com.clothingshop.service.AccountService;
import com.clothingshop.service.CartService;
import com.clothingshop.service.CatalogService;
import com.clothingshop.service.EmailService;
import com.clothingshop.service.InMemoryCartStore;
import com.clothingshop.service.PendingRegistrationStore;
import com.clothingshop.util.JpaUtil;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.time.Clock;
import java.time.ZoneId;

/**
 * Creates the shared services when the application starts and closes the EntityManagerFactory when it stops.
 * Services are stored as servlet context attributes so every servlet uses the same instances
 * (pending registrations and carts are kept in memory and must be shared by every request).
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    static final String ACCOUNT_SERVICE = "accountService";
    static final String CATALOG_SERVICE = "catalogService";
    static final String CART_SERVICE = "cartService";

    /** Business time is Vietnam time (specification section 3.1). */
    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Override
    public void contextInitialized(ServletContextEvent event) {
        Clock clock = Clock.system(ZONE);
        ServletContext context = event.getServletContext();
        context.setAttribute(ACCOUNT_SERVICE,
                new AccountService(EmailService.fromConfig(), new PendingRegistrationStore(clock), clock));
        context.setAttribute(CATALOG_SERVICE, new CatalogService(clock));
        context.setAttribute(CART_SERVICE, new CartService(new InMemoryCartStore(clock), clock));
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        JpaUtil.close();
    }

    static AccountService accountService(ServletContext context) {
        return (AccountService) context.getAttribute(ACCOUNT_SERVICE);
    }

    static CatalogService catalogService(ServletContext context) {
        return (CatalogService) context.getAttribute(CATALOG_SERVICE);
    }

    static CartService cartService(ServletContext context) {
        return (CartService) context.getAttribute(CART_SERVICE);
    }
}
