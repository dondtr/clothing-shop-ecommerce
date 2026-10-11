package com.clothingshop.controller;

import com.clothingshop.dto.AuthUser;
import com.clothingshop.dto.FlashMessage;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.service.WishlistService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Wishlist (FR-10). GET {@code /wishlist} lists it; POST {@code /wishlist/add} and {@code /wishlist/remove} take a
 * {@code productId} (a row of the product) and go back to the product page, or to the wishlist when
 * {@code from=wishlist}. {@code CustomerAreaFilter} lets only signed-in customers in.
 */
@WebServlet({"/wishlist", "/wishlist/add", "/wishlist/remove"})
public class WishlistServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static final String VIEW = "/WEB-INF/views/account/wishlist.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!"/wishlist".equals(request.getServletPath())) {
            response.sendRedirect(request.getContextPath() + "/wishlist");
            return;
        }
        request.setAttribute("entries", wishlistService().list(userId(request)));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean add = "/wishlist/add".equals(request.getServletPath());
        if (!add && !"/wishlist/remove".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        Integer productId = parseInt(request.getParameter("productId"));
        if (productId == null) {
            response.sendRedirect(request.getContextPath() + "/wishlist");
            return;
        }
        try {
            if (add) {
                boolean added = wishlistService().add(userId(request), productId);
                FlashMessage.put(request.getSession(), added ? "success" : "info",
                        added ? "Đã thêm vào danh sách yêu thích." : "Sản phẩm đã có trong danh sách yêu thích.");
            } else {
                wishlistService().remove(userId(request), productId);
                FlashMessage.put(request.getSession(), "success", "Đã bỏ khỏi danh sách yêu thích.");
            }
        } catch (BusinessException e) {
            FlashMessage.put(request.getSession(), "danger", e.getMessage());
        }
        boolean backToList = "wishlist".equals(request.getParameter("from"));
        response.sendRedirect(request.getContextPath() + (backToList ? "/wishlist" : "/product?id=" + productId));
    }

    private WishlistService wishlistService() {
        return AppContextListener.wishlistService(getServletContext());
    }

    private static int userId(HttpServletRequest request) {
        return ((AuthUser) request.getSession().getAttribute(AuthUser.SESSION_KEY)).getId();
    }

    private static Integer parseInt(String value) {
        try {
            return value == null ? null : Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
