package com.clothingshop.dto;

/**
 * One product of the wishlist page (FR-10 step 4).
 *
 * @param product the product as on the shop page; {@code product.id()} is one of its rows
 * @param active  the product is still sold; products no longer sold stay in the list but cannot be bought (FR-10 rules)
 */
public record WishlistEntry(ProductSummary product, boolean active) {
}
