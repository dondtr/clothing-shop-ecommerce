package com.clothingshop.controller;

import com.clothingshop.dto.AuthUser;
import com.clothingshop.dto.ProductDetail;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Product page (FR-08): {@code /product?id=<row id>}. Any row of a product opens the whole product with that row's
 * color and size selected; a product with no row on sale answers 404.
 */
@WebServlet("/product")
public class ProductServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static final String VIEW = "/WEB-INF/views/shop/product.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Optional<ProductDetail> detail = parseId(request.getParameter("id"))
                .flatMap(id -> AppContextListener.catalogService(getServletContext()).findDetail(id));
        if (detail.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        request.setAttribute("product", detail.get());
        AuthUser user = (AuthUser) request.getSession().getAttribute(AuthUser.SESSION_KEY);
        if (user != null && user.isCustomer()) {
            request.setAttribute("inWishlist",
                    AppContextListener.wishlistService(getServletContext()).contains(user.getId(), detail.get().name()));
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static Optional<Integer> parseId(String value) {
        try {
            return value == null ? Optional.empty() : Optional.of(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
