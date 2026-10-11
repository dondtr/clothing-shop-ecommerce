package com.clothingshop.controller;

import com.clothingshop.dto.FlashMessage;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Password change of any signed-in account (FR-05). */
@WebServlet("/account/password")
public class ChangePasswordServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static final String VIEW = "/WEB-INF/views/account/password.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            AppContextListener.profileService(getServletContext()).changePassword(WebUtil.currentUser(request).getId(),
                    request.getParameter("currentPassword"), request.getParameter("newPassword"),
                    request.getParameter("confirmPassword"));
            FlashMessage.put(request.getSession(), "success", "Đã đổi mật khẩu.");
            response.sendRedirect(request.getContextPath() + "/account/profile");
        } catch (ValidationException e) {
            request.setAttribute("errors", e.getErrors());
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }
}
