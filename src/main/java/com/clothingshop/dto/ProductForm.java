package com.clothingshop.dto;

import java.util.List;

/**
 * Values of the product forms (FR-31), exactly as submitted: the common information and, when creating, the variants.
 *
 * @param category  one of the fixed categories (Q08)
 * @param basePrice whole VND, a multiple of 1.000 (BR-21)
 */
public record ProductForm(String name, String category, String description, String basePrice, boolean active,
                          List<Variant> variants) {

    /**
     * One variant: color and size typed freely (Q08), its own stock and image.
     *
     * @param stockQuantity a whole number ≥ 0
     */
    public record Variant(String color, String size, String stockQuantity, String imgUrl) {
    }
}
