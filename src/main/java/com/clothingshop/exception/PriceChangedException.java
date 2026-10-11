package com.clothingshop.exception;

/**
 * The total computed when the order is placed differs from the total the customer confirmed (a price, discount or voucher
 * changed in between); the order is not created and the customer sees the new amounts first.
 */
public class PriceChangedException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public PriceChangedException() {
        super("Giá hoặc ưu đãi vừa thay đổi. Vui lòng kiểm tra lại tổng tiền mới rồi xác nhận mua hàng.");
    }
}
