package com.clothingshop.controller;

import com.clothingshop.service.AccountService;
import com.clothingshop.service.EmailService;
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
 * (the pending registrations must be shared between the register and verify pages).
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    static final String ACCOUNT_SERVICE = "accountService";

    /** Business time is Vietnam time (specification section 3.1). */
    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Override
    public void contextInitialized(ServletContextEvent event) {
        Clock clock = Clock.system(ZONE);
        ServletContext context = event.getServletContext();
        context.setAttribute(ACCOUNT_SERVICE,
                new AccountService(EmailService.fromConfig(), new PendingRegistrationStore(clock), clock));
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        JpaUtil.close();
    }

    static AccountService accountService(ServletContext context) {
        return (AccountService) context.getAttribute(ACCOUNT_SERVICE);
    }
}
