package com.clothingshop.dto;

import com.clothingshop.entity.ProductCategory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Everything the product page shows (FR-08): the common information, the variants on sale and the reviews of every row.
 * JSP pages read records through their components only, so derived values are components too.
 *
 * @param onSale          a discount lowers the price now
 * @param discountPercent percentage of the discount in effect, or {@code null}
 * @param colors          distinct colors of the variants, in the order the variants were created
 * @param sizes           distinct sizes of the variants, in the order the variants were created
 * @param selectedVariant the variant chosen when the page opens (the row in the link when it is on sale)
 * @param averageRating   average of every rating, one decimal, or {@code null} when there is no review
 */
public record ProductDetail(String name, ProductCategory category, String description, BigDecimal basePrice,
                            BigDecimal currentPrice, boolean onSale, BigDecimal discountPercent, List<Variant> variants,
                            List<String> colors, List<String> sizes, Variant selectedVariant,
                            BigDecimal averageRating, List<ReviewItem> reviews) {

    /** One row of the product: a color + size with its own image and stock. */
    public record Variant(int id, String color, String size, String imgUrl, int stockQuantity) {
    }

    /** A review with the reviewer's full name. */
    public record ReviewItem(String authorName, int rating, String comment, LocalDateTime createdAt) {
    }
}
