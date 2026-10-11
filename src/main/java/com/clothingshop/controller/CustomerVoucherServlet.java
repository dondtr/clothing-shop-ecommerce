package com.clothingshop.controller;

import com.clothingshop.service.VoucherService;
import com.clothingshop.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Vouchers the signed-in customer can use now (FR-39). */
@WebServlet("/vouchers")
public class CustomerVoucherServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("vouchers", AppContextListener.service(getServletContext(), VoucherService.class)
                .usableBy(WebUtil.currentUser(request).getId()));
        request.getRequestDispatcher("/WEB-INF/views/account/vouchers.jsp").forward(request, response);
    }
}
