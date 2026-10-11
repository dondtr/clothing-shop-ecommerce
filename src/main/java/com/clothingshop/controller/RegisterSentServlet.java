package com.clothingshop.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/** Page shown after a successful registration: asks the customer to open the verification email. */
@WebServlet("/register/sent")
public class RegisterSentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        Object email = session.getAttribute(RegisterServlet.SENT_EMAIL);
        if (email == null) {
            response.sendRedirect(request.getContextPath() + "/register");
            return;
        }
        session.removeAttribute(RegisterServlet.SENT_EMAIL);
        request.setAttribute("email", email);
        request.getRequestDispatcher("/WEB-INF/views/auth/register-sent.jsp").forward(request, response);
    }
}
