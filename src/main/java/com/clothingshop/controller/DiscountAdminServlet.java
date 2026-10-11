package com.clothingshop.controller;

import com.clothingshop.dto.FlashMessage;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.service.DiscountAdminService;
import com.clothingshop.service.ProductAdminService;
import com.clothingshop.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Discount campaigns of a product, manager only (FR-34): {@code /admin/discounts?id=} lists them; POST
 * {@code /admin/discounts/save} first validates and shows a confirmation with every value (Q68), then saves when
 * {@code confirm=1}. {@code campaign} (the start date) selects the campaign being edited.
 */
@WebServlet({"/admin/discounts", "/admin/discounts/save"})
public class DiscountAdminServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer id = WebUtil.parseInt(request.getParameter("id"));
        try {
            show(request, response, id == null ? -1 : id);
        } catch (BusinessException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer id = WebUtil.parseInt(request.getParameter("id"));
        int rowId = id == null ? -1 : id;
        DiscountAdminService discounts = AppContextListener.service(getServletContext(), DiscountAdminService.class);
        LocalDateTime campaign = parse(request.getParameter("campaign"));
        String value = request.getParameter("discountValue");
        String start = request.getParameter("startDate");
        String end = request.getParameter("endDate");
        try {
            if (!"1".equals(request.getParameter("confirm"))) {
                // FR-34 step 3: confirmation with every value before saving; the form stays editable
                request.setAttribute("confirmation", discounts.validate(rowId, value, start, end, campaign));
            } else if (campaign == null) {
                discounts.create(rowId, value, start, end);
                FlashMessage.put(request.getSession(), "success", "Đã tạo đợt giảm giá cho mọi biến thể của mặt hàng.");
                response.sendRedirect(request.getContextPath() + "/admin/discounts?id=" + rowId);
                return;
            } else {
                discounts.update(rowId, campaign, value, start, end);
                FlashMessage.put(request.getSession(), "success", "Đã lưu đợt giảm giá.");
                response.sendRedirect(request.getContextPath() + "/admin/discounts?id=" + rowId);
                return;
            }
        } catch (ValidationException e) {
            request.setAttribute("errors", e.getErrors());
        } catch (BusinessException e) {
            FlashMessage.put(request.getSession(), "danger", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/admin/discounts?id=" + rowId);
            return;
        }
        show(request, response, rowId);
    }

    private void show(HttpServletRequest request, HttpServletResponse response, int rowId) throws ServletException, IOException {
        request.setAttribute("product", AppContextListener.service(getServletContext(), ProductAdminService.class).get(rowId));
        request.getRequestDispatcher("/WEB-INF/views/staff/discounts.jsp").forward(request, response);
    }

    private static LocalDateTime parse(String value) {
        try {
            return value == null || value.isBlank() ? null : LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
