package com.clothingshop.controller;

import com.clothingshop.dto.AuthUser;
import com.clothingshop.dto.FlashMessage;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.service.CartService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Shopping cart (FR-09). GET {@code /cart} shows it; POST {@code /cart/add} (productId, quantity) adds a row from the
 * product page; POST {@code /cart/update} (qty_&lt;productId&gt; for each line) changes quantities; POST
 * {@code /cart/remove} (productId) removes a line. {@code CustomerAreaFilter} lets only signed-in customers in.
 */
@WebServlet({"/cart", "/cart/add", "/cart/update", "/cart/remove"})
public class CartServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static final String VIEW = "/WEB-INF/views/cart/view.jsp";
    private static final String QTY_PREFIX = "qty_";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!"/cart".equals(request.getServletPath())) {
            response.sendRedirect(request.getContextPath() + "/cart");
            return;
        }
        request.setAttribute("cart", cartService().view(userId(request)));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        switch (request.getServletPath()) {
            case "/cart/add" -> add(request, response);
            case "/cart/update" -> update(request, response);
            case "/cart/remove" -> remove(request, response);
            default -> response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    /** Back to the product page with a message, so the customer can keep shopping. */
    private void add(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer productId = parseInt(request.getParameter("productId"));
        if (productId == null) {
            FlashMessage.put(request.getSession(), "danger", "Vui lòng chọn màu và kích thước.");
            response.sendRedirect(request.getContextPath() + "/shop");
            return;
        }
        try {
            Integer quantity = parseInt(request.getParameter("quantity"));
            int inCart = cartService().addItem(userId(request), productId, quantity == null ? 0 : quantity);
            FlashMessage.put(request.getSession(), "success", "Đã thêm vào giỏ hàng (trong giỏ: " + inCart + ").");
        } catch (BusinessException e) {
            FlashMessage.put(request.getSession(), "danger", e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/product?id=" + productId);
    }

    /** Applies every quantity that is valid and reports the others. */
    private void update(HttpServletRequest request, HttpServletResponse response) throws IOException {
        CartService cart = cartService();
        int userId = userId(request);
        List<String> errors = new ArrayList<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            Integer productId = name.startsWith(QTY_PREFIX) ? parseInt(name.substring(QTY_PREFIX.length())) : null;
            if (productId == null) {
                continue;
            }
            Integer quantity = parseInt(request.getParameter(name));
            try {
                cart.updateQuantity(userId, productId, quantity == null ? 0 : quantity);
            } catch (BusinessException e) {
                errors.add(e.getMessage());
            }
        }
        if (errors.isEmpty()) {
            FlashMessage.put(request.getSession(), "success", "Đã cập nhật giỏ hàng.");
        } else {
            FlashMessage.put(request.getSession(), "warning", String.join(" ", errors));
        }
        response.sendRedirect(request.getContextPath() + "/cart");
    }

    private void remove(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer productId = parseInt(request.getParameter("productId"));
        if (productId != null) {
            cartService().removeItem(userId(request), productId);
            FlashMessage.put(request.getSession(), "success", "Đã xóa sản phẩm khỏi giỏ hàng.");
        }
        response.sendRedirect(request.getContextPath() + "/cart");
    }

    private CartService cartService() {
        return AppContextListener.cartService(getServletContext());
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
