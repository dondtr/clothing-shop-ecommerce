package com.clothingshop.controller;

import com.clothingshop.service.DashboardService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** First page of the staff area: work waiting for employees and the latest orders. */
@WebServlet("/staff")
public class StaffDashboardServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("dashboard", AppContextListener.service(getServletContext(), DashboardService.class).load());
        request.getRequestDispatcher("/WEB-INF/views/staff/dashboard.jsp").forward(request, response);
    }
}
