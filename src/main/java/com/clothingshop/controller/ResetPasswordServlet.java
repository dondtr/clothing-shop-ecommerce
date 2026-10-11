package com.clothingshop.controller;

import com.clothingshop.dto.FlashMessage;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.service.PasswordResetService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Link of the reset email (FR-03 step 3): sets a new password while the link is still valid. */
@WebServlet("/reset-password")
public class ResetPasswordServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static final String VIEW = "/WEB-INF/views/auth/reset-password.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("usable", service().isUsable(request.getParameter("token")));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            service().resetPassword(request.getParameter("token"), request.getParameter("password"),
                    request.getParameter("confirmPassword"));
            FlashMessage.put(request.getSession(), "success", "Đã đặt mật khẩu mới. Vui lòng đăng nhập.");
            response.sendRedirect(request.getContextPath() + "/login");
        } catch (ValidationException e) {
            request.setAttribute("usable", true);
            request.setAttribute("errors", e.getErrors());
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (BusinessException e) {
            request.setAttribute("usable", false);
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }

    private PasswordResetService service() {
        return AppContextListener.passwordResetService(getServletContext());
    }
}
