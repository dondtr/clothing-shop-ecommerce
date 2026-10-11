package com.clothingshop.dto;

/** Values of an address form (FR-06), exactly as submitted (Q06: province, ward, street). */
public record AddressForm(String province, String ward, String street) {
}
