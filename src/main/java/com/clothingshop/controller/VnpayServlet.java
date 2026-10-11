package com.clothingshop.controller;

import com.clothingshop.service.PaymentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * VNPAY callbacks (FR-13): {@code /payment/vnpay-ipn} is called by VNPAY's server and decides the result (register this
 * address as the IPN URL in the VNPAY merchant portal); {@code /payment/vnpay-return} is where the customer's browser
 * comes back and only shows the state of the payment, checking again while it is still waiting.
 */
@WebServlet({"/payment/vnpay-ipn", "/payment/vnpay-return"})
public class VnpayServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PaymentService payments = AppContextListener.service(getServletContext(), PaymentService.class);
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, v.length == 0 ? "" : v[0]));
        if ("/payment/vnpay-ipn".equals(request.getServletPath())) {
            PaymentService.IpnResponse result;
            try {
                result = payments.handleIpn(params);
            } catch (RuntimeException e) {
                log("VNPAY IPN failed", e);
                result = new PaymentService.IpnResponse("99", "Unknown error");
            }
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(result.json());
            return;
        }
        request.setAttribute("status", payments.returnStatus(params));
        request.setAttribute("orderId", params.get("vnp_TxnRef"));
        request.getRequestDispatcher("/WEB-INF/views/checkout/vnpay-return.jsp").forward(request, response);
    }
}
