package com.clothingshop.filter;

import com.clothingshop.dto.AuthUser;
import com.clothingshop.dto.FlashMessage;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Pages for any signed-in account, customer or employee: own profile (FR-04) and password change (FR-05).
 * A visitor who is not signed in is sent to the sign-in page and brought back afterwards (GET only).
 */
@WebFilter({"/account/profile", "/account/password"})
public class SignedInFilter extends HttpFilter {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpSession session = request.getSession();
        if (session.getAttribute(AuthUser.SESSION_KEY) == null) {
            String login = request.getContextPath() + "/login";
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                String path = request.getRequestURI().substring(request.getContextPath().length());
                login += "?next=" + URLEncoder.encode(path, StandardCharsets.UTF_8);
            }
            FlashMessage.put(session, "info", "Vui lòng đăng nhập để tiếp tục.");
            response.sendRedirect(login);
            return;
        }
        chain.doFilter(request, response);
    }
}
