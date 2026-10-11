package com.clothingshop.dto;

import com.clothingshop.entity.ProductCategory;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One product (all rows sharing a name) as a tile of the shop page (FR-07 step 3).
 *
 * <p>JSP pages read records through their components only (Tomcat 11, Expression Language 6), so every value a page
 * needs is a component, including derived ones such as {@code onSale}.
 *
 * @param id              the row the tile links to; its image is shown on the tile
 * @param onSale          a discount lowers the price now, so the base price is shown struck through
 * @param discountPercent percentage of the discount in effect, or {@code null}
 * @param soldOut         every row of the product has stockQuantity = 0 ("Hết hàng")
 * @param newestCreatedAt createdAt of the newest row, used to list new products first
 */
public record ProductSummary(int id, String name, ProductCategory category, String imgUrl, BigDecimal basePrice,
                             BigDecimal currentPrice, boolean onSale, BigDecimal discountPercent, boolean soldOut,
                             LocalDateTime newestCreatedAt) {
}
