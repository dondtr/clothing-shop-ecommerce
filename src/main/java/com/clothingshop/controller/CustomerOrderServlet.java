package com.clothingshop.controller;

import com.clothingshop.dto.AuthUser;
import com.clothingshop.dto.FlashMessage;
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
 * The customer's orders (FR-16): {@code /orders} lists them, {@code /orders/detail?id=} shows one with its buttons;
 * POST {@code /orders/cancel} (FR-17) and {@code /orders/received} (FR-19). {@code CustomerAreaFilter} lets only
 * signed-in customers in.
 */
@WebServlet({"/orders", "/orders/detail", "/orders/cancel", "/orders/received"})
public class CustomerOrderServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int customerId = WebUtil.currentUser(request).getId();
        OrderQueries queries = AppContextListener.service(getServletContext(), OrderQueries.class);
        if ("/orders/detail".equals(request.getServletPath())) {
            Integer id = WebUtil.parseInt(request.getParameter("id"));
            try {
                request.setAttribute("order", queries.forCustomer(customerId, id == null ? -1 : id));
            } catch (BusinessException e) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.getRequestDispatcher("/WEB-INF/views/orders/detail.jsp").forward(request, response);
            return;
        }
        if (!"/orders".equals(request.getServletPath())) {
            response.sendRedirect(request.getContextPath() + "/orders");
            return;
        }
        request.setAttribute("orders", queries.forCustomer(customerId));
        request.getRequestDispatcher("/WEB-INF/views/orders/list.jsp").forward(request, response);
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
                case "/orders/cancel" -> {
                    orders.cancelByCustomer(user.getId(), user.getUserName(), id);
                    FlashMessage.put(request.getSession(), "success", "Đã hủy đơn hàng #" + id + ".");
                }
                case "/orders/received" -> {
                    OrderService.ReceivedResult r = orders.confirmReceivedByCustomer(user.getId(), id);
                    FlashMessage.put(request.getSession(), r.completedNow() ? "success" : "info", r.completedNow()
                            ? "Cảm ơn bạn! Đơn hàng đã giao thành công; bạn có thể đánh giá sản phẩm."
                            : "Đơn hàng hiện ở trạng thái \"" + r.status().getLabel() + "\".");
                }
                default -> {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    return;
                }
            }
        } catch (BusinessException e) {
            FlashMessage.put(request.getSession(), "danger", e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/orders/detail?id=" + id);
    }
}
