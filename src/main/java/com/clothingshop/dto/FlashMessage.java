package com.clothingshop.dto;

import jakarta.servlet.http.HttpSession;
import java.io.Serializable;

/**
 * One-time message shown on the next page after a redirect (stored in the session under {@code flash};
 * the shared header displays and removes it).
 *
 * @param type Bootstrap alert type: success, info, warning or danger
 */
public record FlashMessage(String type, String message) implements Serializable {

    public static final String SESSION_KEY = "flash";

    public static void put(HttpSession session, String type, String message) {
        session.setAttribute(SESSION_KEY, new FlashMessage(type, message));
    }

    /** Bean-style accessors so the JSP expression language can read the record. */
    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
