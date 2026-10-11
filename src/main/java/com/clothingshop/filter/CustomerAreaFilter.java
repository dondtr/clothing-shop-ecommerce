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
 * Pages only a signed-in customer can use: cart, wishlist, checkout, orders, addresses and vouchers (BR-25, Q40).
 * A visitor who is not signed in is sent to the sign-in page and brought back afterwards (GET only); an employee gets
 * 403, because employee accounts do not buy (BR-27). Profile and password pages are for every account
 * ({@link SignedInFilter}).
 */
@WebFilter({"/cart", "/cart/*", "/wishlist", "/wishlist/*", "/checkout", "/checkout/*", "/orders", "/orders/*",
        "/account/addresses", "/account/addresses/*", "/vouchers", "/reviews/*"})
public class CustomerAreaFilter extends HttpFilter {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpSession session = request.getSession();
        AuthUser user = (AuthUser) session.getAttribute(AuthUser.SESSION_KEY);
        if (user == null) {
            String login = request.getContextPath() + "/login";
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                login += "?next=" + URLEncoder.encode(pathWithQuery(request), StandardCharsets.UTF_8);
            }
            FlashMessage.put(session, "info", "Vui lòng đăng nhập bằng tài khoản khách hàng để tiếp tục.");
            response.sendRedirect(login);
            return;
        }
        if (!user.isCustomer()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chức năng này chỉ dành cho tài khoản khách hàng.");
            return;
        }
        chain.doFilter(request, response);
    }

    private static String pathWithQuery(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return request.getQueryString() == null ? path : path + "?" + request.getQueryString();
    }
}
