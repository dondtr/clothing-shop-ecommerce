package com.clothingshop.controller;

import com.clothingshop.exception.BusinessException;
import com.clothingshop.service.DeliveryLinks;
import com.clothingshop.service.DeliveryService;
import com.clothingshop.service.OrderService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Buttons of emails E1 and E2 (FR-19, FR-20): {@code /order-action?token=} works without signing in, applies the action
 * at once when the order is still being delivered, and otherwise only shows the order's current state (Q24, BR-23).
 */
@WebServlet("/order-action")
public class OrderActionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Optional<DeliveryLinks.Link> link = AppContextListener.service(getServletContext(), DeliveryLinks.class)
                .verify(request.getParameter("token"));
        String title;
        String message;
        if (link.isEmpty()) {
            title = "Link không hợp lệ";
            message = "Link này không do cửa hàng tạo ra hoặc đã bị thay đổi.";
        } else {
            int orderId = link.get().orderId();
            request.setAttribute("orderId", orderId);
            try {
                if (link.get().action() == DeliveryLinks.Action.RECEIVED) {
                    OrderService.ReceivedResult r = AppContextListener.service(getServletContext(), OrderService.class)
                            .confirmReceived(orderId);
                    title = r.completedNow() ? "Đơn hàng đã giao thành công" : "Đơn hàng #" + orderId;
                    message = r.completedNow()
                            ? "Cảm ơn bạn đã xác nhận. Bạn có thể đánh giá sản phẩm và gửi yêu cầu trả hàng trong 15 ngày nếu cần."
                            : "Đơn hàng hiện ở trạng thái \"" + r.status().getLabel() + "\"; không có gì thay đổi.";
                } else {
                    DeliveryService.ReportResult r = AppContextListener.service(getServletContext(), DeliveryService.class)
                            .reportNotReceived(orderId);
                    title = "Cửa hàng đã ghi nhận";
                    message = switch (r) {
                        case CREATED -> "Cảm ơn bạn đã báo. Nhân viên sẽ liên hệ đơn vị vận chuyển và xử lý sớm.";
                        case ALREADY_REPORTED -> "Cửa hàng đã ghi nhận thông báo của bạn về đơn hàng này trước đó.";
                        case NOT_SHIPPING -> "Đơn hàng không còn ở trạng thái đang giao; không có gì thay đổi.";
                    };
                }
            } catch (BusinessException e) {
                title = "Không tìm thấy đơn hàng";
                message = e.getMessage();
            }
        }
        request.setAttribute("title", title);
        request.setAttribute("message", message);
        request.getRequestDispatcher("/WEB-INF/views/orders/action-result.jsp").forward(request, response);
    }
}
