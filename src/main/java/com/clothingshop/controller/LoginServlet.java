package com.clothingshop.controller;

import com.clothingshop.dto.AuthUser;
import com.clothingshop.dto.FlashMessage;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Sign-in with a user name or an email (FR-02). */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static final String VIEW = "/WEB-INF/views/auth/login.jsp";

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
        try {
            AuthUser user = AppContextListener.accountService(getServletContext())
                    .login(request.getParameter("login"), request.getParameter("password"));
            request.changeSessionId();   // a new session id after sign-in prevents session fixation
            request.getSession().setAttribute(AuthUser.SESSION_KEY, user);
            FlashMessage.put(request.getSession(), "success", "Xin chào " + user.getFullName() + ", bạn đã đăng nhập thành công.");
            String next = WebUtil.safeRedirectPath(request.getParameter("next"), "/");
            response.sendRedirect(request.getContextPath() + next);
        } catch (BusinessException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }
}
