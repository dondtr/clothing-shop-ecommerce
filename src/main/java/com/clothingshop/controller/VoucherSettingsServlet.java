package com.clothingshop.controller;

import com.clothingshop.dto.FlashMessage;
import com.clothingshop.entity.CustomerLevel;
import com.clothingshop.entity.DiscountType;
import com.clothingshop.entity.EligibleCustomer;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.service.AutoVoucherService;
import com.clothingshop.service.Holiday;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Settings of the automatic vouchers, manager only (FR-37, FR-38): one form per membership level and per holiday
 * campaign; saving writes the JSON file and applies from the next issue (Q64).
 */
@WebServlet({"/admin/voucher-settings", "/admin/voucher-settings/monthly", "/admin/voucher-settings/holiday"})
public class VoucherSettingsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        show(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AutoVoucherService auto = AppContextListener.service(getServletContext(), AutoVoucherService.class);
        AutoVoucherService.Form form = new AutoVoucherService.Form("on".equals(request.getParameter("enabled")),
                request.getParameter("discountType"), request.getParameter("discountValue"), request.getParameter("quantity"),
                request.getParameter("minOrderValue"), request.getParameter("maxDiscount"), request.getParameter("eligibleCustomer"),
                request.getParameter("startDay"), request.getParameter("endDay"), request.getParameter("startOffsetDays"),
                request.getParameter("durationDays"));
        String key = request.getParameter("key");
        try {
            if ("/admin/voucher-settings/monthly".equals(request.getServletPath())) {
                auto.saveMonthly(CustomerLevel.valueOf(key), form);
            } else if ("/admin/voucher-settings/holiday".equals(request.getServletPath())) {
                auto.saveHoliday(Holiday.valueOf(key), form);
            } else {
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                return;
            }
            FlashMessage.put(request.getSession(), "success", "Đã lưu thiết lập; áp dụng từ lần phát hành sau.");
        } catch (ValidationException e) {
            request.setAttribute("errors", e.getErrors());
            request.setAttribute("formKey", key);
            show(request, response);
            return;
        } catch (IllegalArgumentException | NullPointerException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/admin/voucher-settings#" + key);
    }

    private void show(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AutoVoucherService auto = AppContextListener.service(getServletContext(), AutoVoucherService.class);
        request.setAttribute("settings", auto.settings());
        request.setAttribute("occurrences", auto.nextOccurrences());
        request.setAttribute("types", DiscountType.values());
        request.setAttribute("eligibles", EligibleCustomer.values());
        request.getRequestDispatcher("/WEB-INF/views/staff/voucher-settings.jsp").forward(request, response);
    }
}
