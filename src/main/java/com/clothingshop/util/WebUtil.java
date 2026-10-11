package com.clothingshop.util;

import jakarta.servlet.http.HttpServletRequest;

/** Small helpers shared by the controllers. */
public final class WebUtil {

    private WebUtil() {
    }

    /**
     * Absolute URL of {@code path} inside this application, for links sent by email. Uses {@code app.baseUrl} when it
     * is configured (required in production, because the request host can be forged); otherwise derives it from the request.
     */
    public static String absoluteUrl(HttpServletRequest request, String path) {
        String base = AppConfig.get("app.baseUrl").orElseGet(() -> {
            String scheme = request.getScheme();
            int port = request.getServerPort();
            boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
            return scheme + "://" + request.getServerName() + (defaultPort ? "" : ":" + port) + request.getContextPath();
        });
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + path;
    }

    /**
     * Returns {@code next} when it is a path inside this application (starts with a single "/"), so that a link such
     * as {@code /login?next=https://evil.example} cannot send the user to another site; otherwise {@code fallback}.
     */
    public static String safeRedirectPath(String next, String fallback) {
        if (next == null || next.isBlank() || !next.startsWith("/") || next.startsWith("//") || next.contains("\\")
                || next.contains("\r") || next.contains("\n")) {
            return fallback;
        }
        return next;
    }
}
