package com.clothingshop.controller;

import com.clothingshop.dto.AuthUser;
import com.clothingshop.dto.FlashMessage;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.service.ReturnService;
import com.clothingshop.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Return requests for staff (FR-26): the requests waiting for a decision, approve or reject. */
@WebServlet({"/staff/returns", "/staff/returns/approve", "/staff/returns/reject"})
public class StaffReturnServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ReturnService returns = AppContextListener.service(getServletContext(), ReturnService.class);
        request.setAttribute("pending", returns.list(true));
        request.setAttribute("handled", returns.list(false));
        request.getRequestDispatcher("/WEB-INF/views/staff/returns.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AuthUser user = WebUtil.currentUser(request);
        Integer id = WebUtil.parseInt(request.getParameter("id"));
        ReturnService returns = AppContextListener.service(getServletContext(), ReturnService.class);
        try {
            if (id == null) {
                throw new BusinessException("Yêu cầu không tồn tại");
            }
            if ("/staff/returns/approve".equals(request.getServletPath())) {
                returns.approve(id, user.getId(), user.getUserName());
                FlashMessage.put(request.getSession(), "success", "Đã duyệt yêu cầu trả hàng; đơn đã hủy, tồn kho và voucher đã hoàn, khách hàng nhận email E7.");
            } else if ("/staff/returns/reject".equals(request.getServletPath())) {
                returns.reject(id, user.getId());
                FlashMessage.put(request.getSession(), "success", "Đã từ chối yêu cầu trả hàng; khách hàng nhận email E7.");
            } else {
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                return;
            }
        } catch (BusinessException e) {
            FlashMessage.put(request.getSession(), "danger", e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/staff/returns");
    }
}
