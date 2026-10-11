package com.clothingshop.controller;

import com.clothingshop.dto.FlashMessage;
import com.clothingshop.entity.CustomerLevel;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.service.CustomerService;
import com.clothingshop.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Customer lookup (FR-42): employees search and view customers ({@code /staff/customers}); the manager raises the level
 * and turns automatic downgrade on or off ({@code /admin/customers/*}, FR-28).
 */
@WebServlet({"/staff/customers", "/staff/customers/detail", "/admin/customers/upgrade", "/admin/customers/auto-downgrade"})
public class CustomerAdminServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        CustomerService customers = service();
        if ("/staff/customers/detail".equals(request.getServletPath())) {
            Integer id = WebUtil.parseInt(request.getParameter("id"));
            try {
                request.setAttribute("customer", customers.detail(id == null ? -1 : id));
            } catch (BusinessException e) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("levels", CustomerLevel.values());
            request.getRequestDispatcher("/WEB-INF/views/staff/customer-detail.jsp").forward(request, response);
            return;
        }
        if (!"/staff/customers".equals(request.getServletPath())) {
            response.sendRedirect(request.getContextPath() + "/staff/customers");
            return;
        }
        request.setAttribute("customers", customers.search(request.getParameter("q")));
        request.getRequestDispatcher("/WEB-INF/views/staff/customers.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer id = WebUtil.parseInt(request.getParameter("id"));
        Long version = WebUtil.parseLong(request.getParameter("version"));
        try {
            if (id == null) {
                throw new BusinessException("Khách hàng không tồn tại");
            }
            switch (request.getServletPath()) {
                case "/admin/customers/upgrade" -> {
                    service().upgradeLevel(id, parseLevel(request.getParameter("level")), version);
                    FlashMessage.put(request.getSession(), "success", "Đã nâng hạng; chế độ tự hạ hạng đã tắt.");
                }
                case "/admin/customers/auto-downgrade" -> {
                    boolean on = "true".equals(request.getParameter("value"));
                    service().setAutoDowngrade(id, on, version);
                    FlashMessage.put(request.getSession(), "success",
                            on ? "Đã bật tự hạ hạng; hạng đã được xét lại theo tổng tiền đã thanh toán." : "Đã tắt tự hạ hạng.");
                }
                default -> {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    return;
                }
            }
        } catch (BusinessException e) {
            FlashMessage.put(request.getSession(), "danger", e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/staff/customers/detail?id=" + id);
    }

    private static CustomerLevel parseLevel(String value) {
        try {
            return value == null ? null : CustomerLevel.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private CustomerService service() {
        return AppContextListener.service(getServletContext(), CustomerService.class);
    }
}
