package com.clothingshop.dto;

import com.clothingshop.entity.ProductCategory;
import java.math.BigDecimal;

/**
 * Search criteria of the shop page (FR-07). Every criterion is optional ({@code null} = not filtered).
 *
 * @param query    part of the product name, compared without regard to letter case
 * @param minPrice lowest price after discount, inclusive
 * @param maxPrice highest price after discount, inclusive
 * @param page     1-based page number
 */
public record ProductFilter(ProductCategory category, String query, String color, BigDecimal minPrice,
                            BigDecimal maxPrice, int page) {
}
