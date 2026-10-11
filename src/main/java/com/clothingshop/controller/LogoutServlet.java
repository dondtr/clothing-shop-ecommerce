package com.clothingshop.controller;

import com.clothingshop.dto.FlashMessage;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Sign-out (FR-02 step 5), POST only so that a link on another site cannot sign the user out. The cart is kept by
 * account in server memory (specification section 3.4), so it survives sign-out.
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        FlashMessage.put(request.getSession(), "info", "Bạn đã đăng xuất.");
        response.sendRedirect(request.getContextPath() + "/");
    }
}
