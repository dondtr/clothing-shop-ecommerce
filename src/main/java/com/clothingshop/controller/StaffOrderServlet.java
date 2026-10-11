package com.clothingshop.controller;

import com.clothingshop.dto.AuthUser;
import com.clothingshop.dto.FlashMessage;
import com.clothingshop.entity.OrderStatus;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.service.OrderQueries;
import com.clothingshop.service.OrderService;
import com.clothingshop.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Orders for staff: list by status, order page, confirm and hand over to the carrier (FR-18), cancel after a failed
 * delivery (FR-23). Orders waiting for a VNPAY payment are not shown (BR-26).
 */
@WebServlet({"/staff/orders", "/staff/orders/detail", "/staff/orders/confirm", "/staff/orders/ship", "/staff/orders/failed"})
public class StaffOrderServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        OrderQueries queries = AppContextListener.service(getServletContext(), OrderQueries.class);
        if ("/staff/orders/detail".equals(request.getServletPath())) {
            Integer id = WebUtil.parseInt(request.getParameter("id"));
            try {
                request.setAttribute("order", queries.forStaff(id == null ? -1 : id));
            } catch (BusinessException e) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.getRequestDispatcher("/WEB-INF/views/staff/order-detail.jsp").forward(request, response);
            return;
        }
        if (!"/staff/orders".equals(request.getServletPath())) {
            response.sendRedirect(request.getContextPath() + "/staff/orders");
            return;
        }
        OrderStatus status = null;
        try {
            String s = request.getParameter("status");
            status = s == null || s.isBlank() ? null : OrderStatus.valueOf(s);
        } catch (IllegalArgumentException e) {
            status = null;
        }
        request.setAttribute("status", status);
        request.setAttribute("statuses", OrderStatus.values());
        request.setAttribute("orderRows", queries.forStaff(status));
        request.getRequestDispatcher("/WEB-INF/views/staff/orders.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AuthUser user = WebUtil.currentUser(request);
        Integer id = WebUtil.parseInt(request.getParameter("id"));
        OrderService orders = AppContextListener.service(getServletContext(), OrderService.class);
        try {
            if (id == null) {
                throw new BusinessException("Đơn hàng không tồn tại");
            }
            switch (request.getServletPath()) {
                case "/staff/orders/confirm" -> {
                    orders.confirm(id);
                    FlashMessage.put(request.getSession(), "success", "Đã xác nhận đơn #" + id + ".");
                }
                case "/staff/orders/ship" -> {
                    orders.ship(id);
                    FlashMessage.put(request.getSession(), "success", "Đơn #" + id + " đã chuyển sang đang giao; khách hàng nhận email E1.");
                }
                case "/staff/orders/failed" -> {
                    orders.cancelFailedDelivery(id, user.getId(), user.getUserName());
                    FlashMessage.put(request.getSession(), "success", "Đã hủy đơn #" + id + " do giao hàng không thành công.");
                }
                default -> {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    return;
                }
            }
        } catch (BusinessException e) {
            FlashMessage.put(request.getSession(), "danger", e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/staff/orders/detail?id=" + id);
    }
}
