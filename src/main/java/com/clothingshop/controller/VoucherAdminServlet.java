package com.clothingshop.controller;

import com.clothingshop.dto.FlashMessage;
import com.clothingshop.entity.DiscountType;
import com.clothingshop.entity.EligibleCustomer;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.service.VoucherAdminService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Vouchers, manager only (FR-35, FR-36): {@code /admin/vouchers} lists them with filters; {@code /admin/vouchers/new} and
 * {@code /admin/vouchers/edit?code=} show the form; POST {@code /admin/vouchers/save} validates and shows a confirmation
 * with every value (Q27), then saves when {@code confirm=1}.
 */
@WebServlet({"/admin/vouchers", "/admin/vouchers/new", "/admin/vouchers/edit", "/admin/vouchers/save"})
public class VoucherAdminServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        VoucherAdminService vouchers = service();
        switch (request.getServletPath()) {
            case "/admin/vouchers" -> {
                EligibleCustomer eligible = null;
                try {
                    String e = request.getParameter("eligible");
                    eligible = e == null || e.isBlank() ? null : EligibleCustomer.valueOf(e);
                } catch (IllegalArgumentException ignored) {
                    eligible = null;
                }
                request.setAttribute("vouchers", vouchers.list(request.getParameter("q"), eligible, request.getParameter("state")));
                request.setAttribute("eligibles", EligibleCustomer.values());
                request.getRequestDispatcher("/WEB-INF/views/staff/vouchers.jsp").forward(request, response);
            }
            case "/admin/vouchers/new" -> showForm(request, response);
            case "/admin/vouchers/edit" -> {
                try {
                    request.setAttribute("editing", vouchers.get(request.getParameter("code")));
                } catch (BusinessException e) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                showForm(request, response);
            }
            default -> response.sendRedirect(request.getContextPath() + "/admin/vouchers");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        VoucherAdminService vouchers = service();
        String edited = request.getParameter("editCode");
        boolean editing = edited != null && !edited.isBlank();
        VoucherAdminService.Form form = new VoucherAdminService.Form(request.getParameter("code"), request.getParameter("discountType"),
                request.getParameter("discountValue"), request.getParameter("quantity"), request.getParameter("eligibleCustomer"),
                request.getParameter("minOrderValue"), request.getParameter("maxDiscount"), request.getParameter("startDate"),
                request.getParameter("endDate"));
        try {
            if (editing) {
                request.setAttribute("editing", vouchers.get(edited));
            }
            if (!"1".equals(request.getParameter("confirm"))) {
                request.setAttribute("confirmation", vouchers.validate(form, editing ? edited : null));
            } else {
                if (editing) {
                    vouchers.update(edited, form);
                    FlashMessage.put(request.getSession(), "success", "Đã lưu voucher " + edited + ".");
                } else {
                    vouchers.create(form);
                    FlashMessage.put(request.getSession(), "success", "Đã tạo voucher " + form.code().trim().toUpperCase() + ".");
                }
                response.sendRedirect(request.getContextPath() + "/admin/vouchers");
                return;
            }
        } catch (ValidationException e) {
            request.setAttribute("errors", e.getErrors());
        } catch (BusinessException e) {
            FlashMessage.put(request.getSession(), "danger", e.getMessage());
            response.sendRedirect(request.getContextPath() + (editing
                    ? "/admin/vouchers/edit?code=" + URLEncoder.encode(edited, StandardCharsets.UTF_8) : "/admin/vouchers"));
            return;
        }
        showForm(request, response);
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("types", DiscountType.values());
        request.setAttribute("eligibles", EligibleCustomer.values());
        request.getRequestDispatcher("/WEB-INF/views/staff/voucher-form.jsp").forward(request, response);
    }

    private VoucherAdminService service() {
        return AppContextListener.service(getServletContext(), VoucherAdminService.class);
    }
}
