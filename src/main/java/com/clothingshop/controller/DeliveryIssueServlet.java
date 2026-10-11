package com.clothingshop.controller;

import com.clothingshop.dto.FlashMessage;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.service.DeliveryService;
import com.clothingshop.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * "Chưa thấy hàng đâu" reports for staff (FR-42 step 3, FR-21): the open issues, and handling one by reshipping or by
 * recording that it was resolved, with the reason and solution sent to the customer.
 */
@WebServlet({"/staff/delivery-issues", "/staff/delivery-issues/resolve"})
public class DeliveryIssueServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        show(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer issueId = WebUtil.parseInt(request.getParameter("issueId"));
        try {
            if (issueId == null) {
                throw new BusinessException("Sự cố không tồn tại");
            }
            boolean reship = "RESHIP".equals(request.getParameter("resolution"));
            AppContextListener.service(getServletContext(), DeliveryService.class).resolve(issueId,
                    WebUtil.currentUser(request).getId(), reship, request.getParameter("reason"), request.getParameter("solution"));
            FlashMessage.put(request.getSession(), "success", reship
                    ? "Đã xử lý: giao lại hàng (đã trừ tồn kho); khách hàng nhận email E3."
                    : "Đã xử lý: đã giải quyết; khách hàng nhận email E4.");
        } catch (ValidationException e) {
            request.setAttribute("errors", e.getErrors());
            request.setAttribute("formIssueId", issueId);
            show(request, response);
            return;
        } catch (BusinessException e) {
            FlashMessage.put(request.getSession(), "danger", e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/staff/delivery-issues");
    }

    private void show(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("issues", AppContextListener.service(getServletContext(), DeliveryService.class).openIssues());
        request.getRequestDispatcher("/WEB-INF/views/staff/delivery-issues.jsp").forward(request, response);
    }
}
