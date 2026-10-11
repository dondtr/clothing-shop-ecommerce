package com.clothingshop.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Protection against cross-site request forgery: every POST must carry the session's token in the form field
 * {@value #PARAMETER}. Pages put it in their forms with {@code ${sessionScope.csrfToken}}.
 * Static files under {@code /assets/} are skipped so that they do not create sessions.
 */
@WebFilter("/*")
public class CsrfFilter extends HttpFilter {

    private static final long serialVersionUID = 1L;

    public static final String SESSION_KEY = "csrfToken";
    public static final String PARAMETER = "_csrf";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/assets/")) {
            chain.doFilter(request, response);
            return;
        }
        HttpSession session = request.getSession();
        String token = (String) session.getAttribute(SESSION_KEY);
        if (token == null) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(SESSION_KEY, token);
        }
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            String sent = request.getParameter(PARAMETER);
            if (sent == null || !MessageDigest.isEqual(sent.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên làm việc đã hết hạn, vui lòng tải lại trang và thử lại.");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
