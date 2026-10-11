package com.clothingshop.controller;

import com.clothingshop.dto.FlashMessage;
import com.clothingshop.dto.ProductForm;
import com.clothingshop.entity.ProductCategory;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.service.ProductAdminService;
import com.clothingshop.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Product management (FR-31 – FR-33): list, create with variants, edit common information and sale status, add and
 * edit variants, update stock. Deleting variants or products is under {@code /admin/products/*} (manager only, Q36).
 * Products are addressed by any of their row ids, as in the shop.
 */
@WebServlet({"/staff/products", "/staff/products/new", "/staff/products/edit", "/staff/products/create",
        "/staff/products/update", "/staff/products/active", "/staff/products/variant/add", "/staff/products/variant/update",
        "/staff/products/stock", "/admin/products/variant/delete", "/admin/products/delete"})
public class StaffProductServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        switch (request.getServletPath()) {
            case "/staff/products" -> {
                request.setAttribute("products", service().list(request.getParameter("q")));
                request.getRequestDispatcher("/WEB-INF/views/staff/products.jsp").forward(request, response);
            }
            case "/staff/products/new" -> showNew(request, response);
            case "/staff/products/edit" -> {
                Integer id = WebUtil.parseInt(request.getParameter("id"));
                try {
                    showEdit(request, response, id == null ? -1 : id);
                } catch (BusinessException e) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }
            default -> response.sendRedirect(request.getContextPath() + "/staff/products");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ProductAdminService products = service();
        Integer id = WebUtil.parseInt(request.getParameter("id"));
        String stamp = request.getParameter("stamp");
        int target = id == null ? -1 : id;
        try {
            switch (request.getServletPath()) {
                case "/staff/products/create" -> {
                    target = products.create(form(request, true));
                    flash(request, "Đã thêm mặt hàng.");
                }
                case "/staff/products/update" -> {
                    products.updateCommon(target, form(request, false), stamp);
                    flash(request, "Đã lưu thông tin chung cho mọi biến thể.");
                }
                case "/staff/products/active" -> {
                    boolean on = "true".equals(request.getParameter("active"));
                    products.setActive(target, on, stamp);
                    flash(request, on ? "Mặt hàng đã chuyển sang đang bán." : "Mặt hàng đã chuyển sang không bán; khách không còn thấy ở cửa hàng.");
                }
                case "/staff/products/variant/add" -> {
                    target = products.addVariant(target, variant(request, ""), stamp);
                    flash(request, "Đã thêm biến thể.");
                }
                case "/staff/products/variant/update" -> {
                    products.updateVariant(target, variant(request, ""), WebUtil.parseLong(request.getParameter("version")));
                    flash(request, "Đã lưu biến thể.");
                }
                case "/staff/products/stock" -> {
                    products.updateStock(target, request.getParameter("quantity"), "add".equals(request.getParameter("mode")),
                            WebUtil.parseLong(request.getParameter("version")));
                    flash(request, "Đã cập nhật tồn kho.");
                }
                case "/admin/products/variant/delete" -> {
                    int other = WebUtil.parseInt(request.getParameter("back")) == null ? target : WebUtil.parseInt(request.getParameter("back"));
                    products.deleteVariant(target);
                    flash(request, "Đã xóa biến thể.");
                    target = other;
                }
                case "/admin/products/delete" -> {
                    products.deleteProduct(target);
                    flash(request, "Đã xóa mặt hàng.");
                    response.sendRedirect(request.getContextPath() + "/staff/products");
                    return;
                }
                default -> {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    return;
                }
            }
        } catch (ValidationException e) {
            request.setAttribute("errors", e.getErrors());
            request.setAttribute("formTarget", request.getServletPath());
            request.setAttribute("formRow", id);
            if ("/staff/products/create".equals(request.getServletPath())) {
                showNew(request, response);
            } else {
                showEdit(request, response, target);
            }
            return;
        } catch (BusinessException e) {
            FlashMessage.put(request.getSession(), "danger", e.getMessage());
            if ("/staff/products/create".equals(request.getServletPath())) {
                response.sendRedirect(request.getContextPath() + "/staff/products/new");
                return;
            }
        }
        response.sendRedirect(request.getContextPath() + "/staff/products/edit?id=" + target);
    }

    private void showNew(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("categories", ProductCategory.values());
        request.setAttribute("images", images(request));
        request.getRequestDispatcher("/WEB-INF/views/staff/product-new.jsp").forward(request, response);
    }

    private void showEdit(HttpServletRequest request, HttpServletResponse response, int id) throws ServletException, IOException {
        request.setAttribute("product", service().get(id));
        request.setAttribute("categories", ProductCategory.values());
        request.setAttribute("images", images(request));
        request.getRequestDispatcher("/WEB-INF/views/staff/product-edit.jsp").forward(request, response);
    }

    /** Images shipped with the application, suggested in the image field (any http(s) address can be typed too). */
    private Set<String> images(HttpServletRequest request) {
        Set<String> paths = request.getServletContext().getResourcePaths("/assets/images/");
        Set<String> result = new TreeSet<>();
        if (paths != null) {
            paths.stream().filter(p -> p.matches("(?i).+\\.(jpe?g|png|webp|gif)$")).forEach(p -> result.add(p.substring(1)));
        }
        return result;
    }

    private static ProductForm form(HttpServletRequest request, boolean withVariants) {
        List<ProductForm.Variant> variants = new ArrayList<>();
        if (withVariants) {
            String[] colors = request.getParameterValues("color");
            String[] sizes = request.getParameterValues("size");
            String[] stocks = request.getParameterValues("stockQuantity");
            String[] images = request.getParameterValues("imgUrl");
            int n = colors == null ? 0 : colors.length;
            for (int i = 0; i < n; i++) {
                String color = colors[i];
                String size = at(sizes, i);
                if ((color == null || color.isBlank()) && (size == null || size.isBlank())) {
                    continue;   // an empty extra row of the form
                }
                variants.add(new ProductForm.Variant(color, size, at(stocks, i), at(images, i)));
            }
        }
        return new ProductForm(request.getParameter("name"), request.getParameter("category"), request.getParameter("description"),
                request.getParameter("basePrice"), "on".equals(request.getParameter("active")) || "true".equals(request.getParameter("active")),
                variants);
    }

    private static ProductForm.Variant variant(HttpServletRequest request, String prefix) {
        return new ProductForm.Variant(request.getParameter(prefix + "color"), request.getParameter(prefix + "size"),
                request.getParameter(prefix + "stockQuantity"), request.getParameter(prefix + "imgUrl"));
    }

    private static String at(String[] values, int i) {
        return values == null || i >= values.length ? null : values[i];
    }

    private static void flash(HttpServletRequest request, String message) {
        FlashMessage.put(request.getSession(), "success", message);
    }

    private ProductAdminService service() {
        return AppContextListener.service(getServletContext(), ProductAdminService.class);
    }
}
