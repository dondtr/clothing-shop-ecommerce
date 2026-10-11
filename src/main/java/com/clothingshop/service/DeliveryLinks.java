package com.clothingshop.service;

import com.clothingshop.util.SignedToken;
import java.time.Instant;
import java.util.Optional;

/**
 * Buttons in the delivery emails E1 and E2 (ActionLink, FR-19, FR-20): signed links that work without signing in and do
 * not expire (Q24, BR-23); the signature (HMAC with {@code app.secret}) makes them impossible to forge.
 */
public class DeliveryLinks {

    /** What the button does. */
    public enum Action {
        /** "Đã nhận hàng" / "Đã nhận được hàng". */
        RECEIVED,
        /** "Chưa thấy hàng đâu". */
        NOT_RECEIVED
    }

    /** A verified link. */
    public record Link(int orderId, Action action) {
    }

    static final String PURPOSE = "order-action";
    /** BR-23: the links do not expire. */
    private static final Instant NEVER = Instant.parse("9999-12-31T23:59:59Z");

    private final String baseUrl;

    /** @param baseUrl absolute address of the application, for example {@code https://shop.example/clothing-shop} */
    public DeliveryLinks(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public String received(int orderId) {
        return url(orderId, Action.RECEIVED);
    }

    public String notReceived(int orderId) {
        return url(orderId, Action.NOT_RECEIVED);
    }

    /** The order and action of a link, or empty when the link was not made by this shop. */
    public Optional<Link> verify(String token) {
        return SignedToken.verify(PURPOSE, token, Instant.now()).flatMap(subject -> {
            int dot = subject.indexOf('.');
            try {
                return Optional.of(new Link(Integer.parseInt(subject.substring(0, dot)), Action.valueOf(subject.substring(dot + 1))));
            } catch (RuntimeException e) {
                return Optional.empty();
            }
        });
    }

    private String url(int orderId, Action action) {
        return baseUrl + "/order-action?token=" + SignedToken.create(PURPOSE, orderId + "." + action.name(), NEVER);
    }
}
