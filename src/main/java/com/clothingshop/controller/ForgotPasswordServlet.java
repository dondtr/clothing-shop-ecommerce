package com.clothingshop.controller;

import com.clothingshop.service.PasswordResetService;
import com.clothingshop.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Forgotten password, steps 1 – 2 (FR-03): asks for the email and sends the reset link (E9). */
@WebServlet("/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static final String VIEW = "/WEB-INF/views/auth/forgot-password.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (WebUtil.currentUser(request) != null) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AppContextListener.passwordResetService(getServletContext())
                .requestReset(request.getParameter("email"), WebUtil.absoluteUrl(request, "/reset-password"));
        request.setAttribute("sent", PasswordResetService.MSG_SENT);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
