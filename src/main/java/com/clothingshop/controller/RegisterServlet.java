package com.clothingshop.controller;

import com.clothingshop.dto.AuthUser;
import com.clothingshop.dto.RegistrationForm;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.service.AccountService;
import com.clothingshop.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Customer registration form (FR-01 steps 1 – 3). */
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static final String VIEW = "/WEB-INF/views/auth/register.jsp";
    static final String SENT_EMAIL = "registeredEmail";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getSession().getAttribute(AuthUser.SESSION_KEY) != null) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RegistrationForm form = new RegistrationForm(request.getParameter("userName"), request.getParameter("password"),
                request.getParameter("confirmPassword"), request.getParameter("email"), request.getParameter("fullName"),
                request.getParameter("gender"), request.getParameter("dob"), request.getParameter("phone"));
        AccountService accounts = AppContextListener.accountService(getServletContext());
        try {
            String email = accounts.register(form, WebUtil.absoluteUrl(request, "/register/verify"));
            request.getSession().setAttribute(SENT_EMAIL, email);
            response.sendRedirect(request.getContextPath() + "/register/sent");
        } catch (ValidationException e) {
            request.setAttribute("errors", e.getErrors());
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (BusinessException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }
}
