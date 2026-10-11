package com.clothingshop.controller;

import com.clothingshop.entity.Customer;
import com.clothingshop.exception.BusinessException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Target of the verification link in email E8 (FR-01 steps 4 – 5). */
@WebServlet("/register/verify")
public class VerifyEmailServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            Customer customer = AppContextListener.accountService(getServletContext()).verifyEmail(request.getParameter("token"));
            request.setAttribute("verifiedUserName", customer.getUserName());
        } catch (BusinessException e) {
            request.setAttribute("error", e.getMessage());
        }
        request.getRequestDispatcher("/WEB-INF/views/auth/verify-result.jsp").forward(request, response);
    }
}
