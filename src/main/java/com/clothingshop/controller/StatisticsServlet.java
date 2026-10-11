package com.clothingshop.controller;

import com.clothingshop.exception.ValidationException;
import com.clothingshop.service.StatisticsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Business statistics, manager only (FR-43): {@code /admin/statistics?from=yyyy-MM-dd&to=yyyy-MM-dd}. */
@WebServlet("/admin/statistics")
public class StatisticsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        StatisticsService statistics = AppContextListener.service(getServletContext(), StatisticsService.class);
        LocalDate[] period = statistics.defaultPeriod();
        LocalDate from = parse(request.getParameter("from"), period[0]);
        LocalDate to = parse(request.getParameter("to"), period[1]);
        try {
            request.setAttribute("report", statistics.report(from, to));
        } catch (ValidationException e) {
            request.setAttribute("errors", e.getErrors());
            request.setAttribute("report", statistics.report(period[0], period[1]));
        }
        LocalDate today = period[1];
        request.setAttribute("presets", List.of(
                new Preset("Tháng này", today.withDayOfMonth(1), today),
                new Preset("Tháng trước", today.minusMonths(1).withDayOfMonth(1), today.withDayOfMonth(1).minusDays(1)),
                new Preset("30 ngày qua", today.minusDays(29), today),
                new Preset("Năm nay", today.withDayOfYear(1), today)));
        request.getRequestDispatcher("/WEB-INF/views/staff/statistics.jsp").forward(request, response);
    }

    /** A ready-made period shown above the form. */
    public record Preset(String label, LocalDate from, LocalDate to) {
    }

    private static LocalDate parse(String value, LocalDate fallback) {
        try {
            return value == null || value.isBlank() ? fallback : LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }
}
