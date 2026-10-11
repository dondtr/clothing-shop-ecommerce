package com.clothingshop.dto;

/**
 * A saved delivery address (FR-06).
 *
 * @param text    "street, ward, province", as it will be written on an order
 * @param version current version, sent back with the edit form (section 3.1)
 */
public record AddressView(int id, String province, String ward, String street, boolean defaultAddress, String text,
                          long version) {
}
