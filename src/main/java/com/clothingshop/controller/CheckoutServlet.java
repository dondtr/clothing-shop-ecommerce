package com.clothingshop.controller;

import com.clothingshop.dto.AddressForm;
import com.clothingshop.dto.FlashMessage;
import com.clothingshop.entity.PaymentMethod;
import com.clothingshop.exception.BusinessException;
import com.clothingshop.exception.ValidationException;
import com.clothingshop.service.CheckoutService;
import com.clothingshop.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Checkout (FR-12, FR-14, FR-15): GET {@code /checkout} shows the cart lines, address, voucher and amounts; POST with
 * {@code action=preview} recomputes them; POST with {@code action=place} places the order. {@code CustomerAreaFilter}
 * lets only signed-in customers in.
 */
@WebServlet({"/checkout", "/checkout/success"})
public class CheckoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static final String VIEW = "/WEB-INF/views/checkout/form.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if ("/checkout/success".equals(request.getServletPath())) {
            request.setAttribute("orderId", WebUtil.parseInt(request.getParameter("id")));
            request.getRequestDispatcher("/WEB-INF/views/checkout/success.jsp").forward(request, response);
            return;
        }
        int customerId = WebUtil.currentUser(request).getId();
        CheckoutService.Request all = new CheckoutService.Request(null, null, null, null, null, PaymentMethod.COD, null);
        show(request, response, service().preview(customerId, all));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int customerId = WebUtil.currentUser(request).getId();
        CheckoutService.Request form = form(request, "place".equals(request.getParameter("action")));
        if (form.expectedTotal() != null) {
            try {
                int orderId = service().place(customerId, form);
                FlashMessage.put(request.getSession(), "success", "Đặt hàng thành công. Mã đơn của bạn là #" + orderId + ".");
                response.sendRedirect(request.getContextPath() + "/checkout/success?id=" + orderId);
                return;
            } catch (ValidationException e) {
                request.setAttribute("errors", e.getErrors());
            } catch (BusinessException e) {
                request.setAttribute("error", e.getMessage());
            }
        }
        show(request, response, service().preview(customerId, withoutTotal(form)));
    }

    private void show(HttpServletRequest request, HttpServletResponse response, Object view) throws ServletException, IOException {
        request.setAttribute("checkout", view);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static CheckoutService.Request form(HttpServletRequest request, boolean placing) {
        Set<Integer> ids = new LinkedHashSet<>();
        String[] values = request.getParameterValues("productId");
        if (values != null) {
            for (String v : values) {
                Integer id = WebUtil.parseInt(v);
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        String choice = request.getParameter("addressChoice");
        Integer addressId = "new".equals(choice) ? null : WebUtil.parseInt(choice);
        AddressForm newAddress = addressId == null && "new".equals(choice)
                ? new AddressForm(request.getParameter("province"), request.getParameter("ward"), request.getParameter("street"))
                : null;
        PaymentMethod method = "VNPAY".equals(request.getParameter("method")) ? PaymentMethod.VNPAY : PaymentMethod.COD;
        BigDecimal expected = null;
        if (placing) {
            try {
                expected = new BigDecimal(request.getParameter("expectedTotal"));
            } catch (NumberFormatException | NullPointerException e) {
                expected = BigDecimal.valueOf(-1);   // never equal to a real total: the customer sees the new amounts
            }
        }
        return new CheckoutService.Request(ids, addressId, newAddress, request.getParameter("note"),
                request.getParameter("voucherCode"), method, expected);
    }

    private static CheckoutService.Request withoutTotal(CheckoutService.Request r) {
        return new CheckoutService.Request(r.productIds(), r.addressId(), r.newAddress(), r.note(), r.voucherCode(), r.method(), null);
    }

    private CheckoutService service() {
        return AppContextListener.service(getServletContext(), CheckoutService.class);
    }
}
