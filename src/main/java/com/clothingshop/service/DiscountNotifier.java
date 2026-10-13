package com.clothingshop.service;

import com.clothingshop.dao.DiscountDao;
import com.clothingshop.dao.WishlistDao;
import com.clothingshop.util.JpaUtil;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task J4 (FR-11, Q11): at 00:00 every day, emails E5 to the customers whose wishlist holds a product whose discount
 * starts that day; one email per customer and product; products not on sale are skipped. J4 runs exactly once a day and
 * stores nothing, so each discount is announced once.
 */
public class DiscountNotifier {

    private static final Logger LOG = LoggerFactory.getLogger(DiscountNotifier.class);

    private final EmailService mail;
    private final String baseUrl;
    private final Clock clock;

    public DiscountNotifier(EmailService mail, String baseUrl, Clock clock) {
        this.mail = mail;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.clock = clock;
    }

    /** @return number of emails sent */
    public int notifyWishlists() {
        LocalDateTime today = LocalDate.now(clock).atStartOfDay();
        int sent = JpaUtil.read(em -> {
            List<Object[]> starting = new DiscountDao(em).findStartingBetween(today, today.plusDays(1));
            int count = 0;
            for (Object[] c : starting) {
                String name = (String) c[0];
                List<Object[]> customers = new WishlistDao(em).findCustomersWatching(name);
                for (Object[] cu : customers) {
                    Emails.Message m = Emails.wishlistDiscount((String) cu[1], name, (BigDecimal) c[1], (LocalDateTime) c[2],
                            (LocalDateTime) c[3], baseUrl + "/product?id=" + c[4]);
                    mail.send((String) cu[0], m.subject(), m.body());
                    count++;
                }
            }
            return count;
        });
        LOG.info("J4: {} wishlist discount emails (E5) sent", sent);
        return sent;
    }
}
