package com.clothingshop.dto;

import com.clothingshop.entity.PaymentMethod;
import java.math.BigDecimal;
import java.util.List;

/**
 * The checkout page (FR-12 steps 1 – 3): the cart lines to choose from, the delivery address, the recipient (always the
 * account holder, Q06) and the amounts of FR-15 for the chosen lines and voucher.
 *
 * @param addressId     the saved address chosen, or {@code null} when a new address is typed
 * @param newAddress    the typed address when {@code addressId} is {@code null}
 * @param voucherError  why the typed voucher cannot be used, or {@code null}
 * @param discount      voucher discount (BR-07), 0 without a usable voucher
 * @param total         product amount − discount + shipping fee (BR-04)
 * @param problems      what prevents placing the order as it is
 * @param canPlace      no problem and no voucher error, so "Xác nhận mua hàng" can be pressed
 * @param vnpayEnabled  VNPAY is configured on this server
 */
public record CheckoutView(List<Line> lines, List<AddressView> addresses, Integer addressId, AddressForm newAddress,
                           String recipientName, String recipientPhone, String note, String voucherCode,
                           String voucherError, PaymentMethod method, BigDecimal productAmount, BigDecimal discount,
                           BigDecimal shippingFee, BigDecimal total, List<String> problems, boolean canPlace,
                           boolean vnpayEnabled) {

    /** A cart line; only orderable lines can be selected. */
    public record Line(int productId, String name, String color, String size, String imgUrl, BigDecimal unitPrice,
                       int quantity, BigDecimal subtotal, CartView.Status status, boolean orderable, boolean selected) {
    }
}
