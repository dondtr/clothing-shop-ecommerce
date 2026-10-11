package com.clothingshop.controller;

import com.clothingshop.dto.ProductFilter;
import com.clothingshop.entity.ProductCategory;
import com.clothingshop.service.CatalogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

/**
 * Shop page: products by category, search by name, price range (after discount) and color (FR-07).
 * Query parameters: category, q, color, minPrice, maxPrice, page.
 */
@WebServlet("/shop")
public class ShopServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static final String VIEW = "/WEB-INF/views/shop/list.jsp";

    private static final int NAME_MAX = 200;    // Product.name VARCHAR(200)
    private static final int COLOR_MAX = 50;    // Product.color VARCHAR(50)
    private static final BigDecimal PRICE_MAX = new BigDecimal("9999999999");   // Product.basePrice DECIMAL(12,2)

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ProductCategory category = parseCategory(request.getParameter("category"));
        String query = clip(request.getParameter("q"), NAME_MAX);
        String color = clip(request.getParameter("color"), COLOR_MAX);
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        try {
            minPrice = parsePrice(request.getParameter("minPrice"));
            maxPrice = parsePrice(request.getParameter("maxPrice"));
            if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", "Khoảng giá không hợp lệ: nhập số tiền từ 0 trở lên, giá từ không lớn hơn giá đến.");
            minPrice = null;
            maxPrice = null;
        }
        ProductFilter filter = new ProductFilter(category, query, color, minPrice, maxPrice, parsePage(request.getParameter("page")));

        CatalogService catalog = AppContextListener.catalogService(getServletContext());
        request.setAttribute("filter", filter);
        request.setAttribute("result", catalog.search(filter));
        request.setAttribute("colors", catalog.colors());
        request.setAttribute("categories", ProductCategory.values());
        request.setAttribute("pageQuery", pageQuery(filter));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    /** An unknown category (for example a hand-edited link) shows every category. */
    private static ProductCategory parseCategory(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ProductCategory.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Search text is not stored, so text longer than the column is shortened instead of rejected. */
    private static String clip(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    /** A whole, non-negative amount of VND; {@code null} when empty. */
    private static BigDecimal parsePrice(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            BigDecimal price = new BigDecimal(value.trim());
            if (price.signum() < 0 || price.compareTo(PRICE_MAX) > 0 || price.stripTrailingZeros().scale() > 0) {
                throw new IllegalArgumentException();
            }
            return price.setScale(0, RoundingMode.UNNECESSARY);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static int parsePage(String value) {
        try {
            return value == null ? 1 : Math.max(1, Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** The current filter as a query string without {@code page}, for the pagination links. */
    private static String pageQuery(ProductFilter filter) {
        StringJoiner query = new StringJoiner("&");
        if (filter.category() != null) {
            query.add("category=" + filter.category().name());
        }
        if (filter.query() != null) {
            query.add("q=" + URLEncoder.encode(filter.query(), StandardCharsets.UTF_8));
        }
        if (filter.color() != null) {
            query.add("color=" + URLEncoder.encode(filter.color(), StandardCharsets.UTF_8));
        }
        if (filter.minPrice() != null) {
            query.add("minPrice=" + filter.minPrice().toPlainString());
        }
        if (filter.maxPrice() != null) {
            query.add("maxPrice=" + filter.maxPrice().toPlainString());
        }
        return query.toString();
    }
}
