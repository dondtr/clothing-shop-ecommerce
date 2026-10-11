package com.clothingshop.dto;

/**
 * An issued voucher in the manager's list (FR-36).
 *
 * @param state      "Chưa đến hạn", "Đang hiệu lực", "Hết lượt" or "Hết hạn"
 * @param autoIssued a monthly or holiday voucher, recognised by its code (FR-37, FR-38): never edited directly
 * @param editable   a manual voucher before its start date (BR-22, Q60)
 */
public record VoucherRow(VoucherView voucher, String state, boolean autoIssued, boolean editable) {
}
