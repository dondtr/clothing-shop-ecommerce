package com.clothingshop.filter;

import com.clothingshop.dto.AuthUser;
import com.clothingshop.dto.FlashMessage;
import com.clothingshop.service.EmployeeService;
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
 * Staff area (section 2.2): {@code /staff/*} for employees (STAFF and ADMIN), {@code /admin/*} for the manager only.
 * The account's active flag is read on every request, so locking an employee (FR-41) takes effect at once and ends the
 * session (BR-18). Customers get 403.
 */
@WebFilter({"/staff", "/staff/*", "/admin", "/admin/*"})
public class StaffAreaFilter extends HttpFilter {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpSession session = request.getSession();
        AuthUser user = (AuthUser) session.getAttribute(AuthUser.SESSION_KEY);
        if (user == null) {
            String login = request.getContextPath() + "/login";
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                String path = request.getRequestURI().substring(request.getContextPath().length());
                String query = request.getQueryString();
                login += "?next=" + URLEncoder.encode(query == null ? path : path + "?" + query, StandardCharsets.UTF_8);
            }
            FlashMessage.put(session, "info", "Vui lòng đăng nhập bằng tài khoản nhân viên.");
            response.sendRedirect(login);
            return;
        }
        if (!user.isEmployee()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Khu quản lý chỉ dành cho nhân viên cửa hàng.");
            return;
        }
        EmployeeService employees = (EmployeeService) request.getServletContext().getAttribute(EmployeeService.class.getName());
        if (!employees.isActive(user.getId())) {
            session.invalidate();
            HttpSession fresh = request.getSession(true);
            FlashMessage.put(fresh, "danger", "Tài khoản nhân viên đã bị khóa.");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        if (request.getServletPath().startsWith("/admin") && !user.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chức năng này chỉ dành cho quản lý.");
            return;
        }
        chain.doFilter(request, response);
    }
}
