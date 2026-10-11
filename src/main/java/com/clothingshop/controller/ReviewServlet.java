package com.clothingshop.controller;

import com.clothingshop.dto.FlashMessage;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.service.ReviewService;
import com.clothingshop.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Reviews (FR-29, FR-30): POST {@code /reviews/create} (productId = the row bought, rating, comment) from the order
 * page, POST {@code /reviews/delete} (reviewId) from the order or product page; both go back to {@code next}.
 * {@code CustomerAreaFilter} lets only signed-in customers in.
 */
@WebServlet({"/reviews/create", "/reviews/delete"})
public class ReviewServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int customerId = WebUtil.currentUser(request).getId();
        ReviewService reviews = AppContextListener.service(getServletContext(), ReviewService.class);
        try {
            if ("/reviews/create".equals(request.getServletPath())) {
                Integer productId = WebUtil.parseInt(request.getParameter("productId"));
                reviews.create(customerId, productId == null ? -1 : productId, request.getParameter("rating"),
                        request.getParameter("comment"));
                FlashMessage.put(request.getSession(), "success", "Cảm ơn bạn đã đánh giá sản phẩm.");
            } else {
                Integer reviewId = WebUtil.parseInt(request.getParameter("reviewId"));
                reviews.delete(customerId, reviewId == null ? -1 : reviewId);
                FlashMessage.put(request.getSession(), "success", "Đã xóa đánh giá.");
            }
        } catch (ValidationException e) {
            FlashMessage.put(request.getSession(), "danger", String.join(" ", e.getErrors().values()));
        } catch (BusinessException e) {
            FlashMessage.put(request.getSession(), "danger", e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + WebUtil.safeRedirectPath(request.getParameter("next"), "/orders"));
    }
}
