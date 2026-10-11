package com.clothingshop.controller;

import com.clothingshop.dto.AddressForm;
import com.clothingshop.dto.AddressView;
import com.clothingshop.dto.FlashMessage;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.service.AddressService;
import com.clothingshop.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Delivery addresses (FR-06): {@code /account/addresses} lists them with forms to add, edit, set the default and delete.
 * Deleting the default address goes through {@code /account/addresses/remove-default}, where the customer chooses
 * another address or creates a new one as the default (Q62, SD13).
 */
@WebServlet({"/account/addresses", "/account/addresses/add", "/account/addresses/update", "/account/addresses/default",
        "/account/addresses/remove", "/account/addresses/remove-default"})
public class AddressServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static final String VIEW = "/WEB-INF/views/account/addresses.jsp";
    static final String REMOVE_DEFAULT_VIEW = "/WEB-INF/views/account/remove-default-address.jsp";
    private static final String LIST = "/account/addresses";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        switch (request.getServletPath()) {
            case LIST -> showList(request, response);
            case "/account/addresses/remove-default" -> showRemoveDefault(request, response, WebUtil.parseInt(request.getParameter("id")));
            default -> response.sendRedirect(request.getContextPath() + LIST);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int customerId = WebUtil.currentUser(request).getId();
        Integer id = WebUtil.parseInt(request.getParameter("id"));
        AddressService addresses = service();
        try {
            switch (request.getServletPath()) {
                case "/account/addresses/add" -> {
                    addresses.add(customerId, form(request));
                    flash(request, "Đã thêm địa chỉ.");
                }
                case "/account/addresses/update" -> {
                    addresses.update(customerId, required(id), form(request), WebUtil.parseLong(request.getParameter("version")));
                    flash(request, "Đã lưu địa chỉ.");
                }
                case "/account/addresses/default" -> {
                    addresses.setDefault(customerId, required(id));
                    flash(request, "Đã đổi địa chỉ mặc định.");
                }
                case "/account/addresses/remove" -> {
                    if (isDefault(customerId, required(id))) {
                        response.sendRedirect(request.getContextPath() + "/account/addresses/remove-default?id=" + id);
                        return;
                    }
                    addresses.remove(customerId, id);
                    flash(request, "Đã xóa địa chỉ.");
                }
                case "/account/addresses/remove-default" -> {
                    if ("new".equals(request.getParameter("choice"))) {
                        addresses.removeDefault(customerId, required(id), form(request));
                        flash(request, "Đã xóa và tạo địa chỉ mặc định mới.");
                    } else {
                        Integer newDefaultId = WebUtil.parseInt(request.getParameter("newDefaultId"));
                        if (newDefaultId == null) {
                            throw new BusinessException("Vui lòng chọn địa chỉ mặc định mới hoặc tạo địa chỉ mới");
                        }
                        addresses.removeDefault(customerId, required(id), newDefaultId);
                        flash(request, "Đã xóa và đổi địa chỉ mặc định.");
                    }
                }
                default -> {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    return;
                }
            }
        } catch (ValidationException e) {
            request.setAttribute("errors", e.getErrors());
            request.setAttribute("form", form(request));
            request.setAttribute("formTarget", request.getServletPath());
            request.setAttribute("formId", id);
            if ("/account/addresses/remove-default".equals(request.getServletPath())) {
                showRemoveDefault(request, response, id);
            } else {
                showList(request, response);
            }
            return;
        } catch (BusinessException e) {
            FlashMessage.put(request.getSession(), "danger", e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + LIST);
    }

    private void showList(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("addresses", service().list(WebUtil.currentUser(request).getId()));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private void showRemoveDefault(HttpServletRequest request, HttpServletResponse response, Integer id)
            throws ServletException, IOException {
        List<AddressView> all = service().list(WebUtil.currentUser(request).getId());
        AddressView target = all.stream().filter(a -> id != null && a.id() == id && a.defaultAddress()).findFirst().orElse(null);
        if (target == null) {
            response.sendRedirect(request.getContextPath() + LIST);
            return;
        }
        request.setAttribute("target", target);
        request.setAttribute("others", all.stream().filter(a -> a.id() != target.id()).toList());
        request.getRequestDispatcher(REMOVE_DEFAULT_VIEW).forward(request, response);
    }

    private boolean isDefault(int customerId, int id) {
        return service().list(customerId).stream().anyMatch(a -> a.id() == id && a.defaultAddress());
    }

    private static AddressForm form(HttpServletRequest request) {
        return new AddressForm(request.getParameter("province"), request.getParameter("ward"), request.getParameter("street"));
    }

    private static int required(Integer id) {
        if (id == null) {
            throw new BusinessException("Địa chỉ không tồn tại");
        }
        return id;
    }

    private static void flash(HttpServletRequest request, String message) {
        FlashMessage.put(request.getSession(), "success", message);
    }

    private AddressService service() {
        return AppContextListener.addressService(getServletContext());
    }
}
