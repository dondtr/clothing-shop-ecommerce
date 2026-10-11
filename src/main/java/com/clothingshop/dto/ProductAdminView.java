package com.clothingshop.dto;

import com.clothingshop.entity.ProductCategory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A product (all rows sharing a name) as staff manage it (FR-31 – FR-34).
 *
 * @param stamp     versions of every row when the page was opened ("id:version,…"); a save based on an older stamp is
 *                  refused (section 3.1)
 * @param canDelete no row of the product was ever ordered or reviewed (BR-15)
 */
public record ProductAdminView(int firstId, String name, ProductCategory category, String description, BigDecimal basePrice,
                               boolean active, LocalDateTime createdAt, List<Row> rows, String stamp, boolean canDelete,
                               List<Campaign> campaigns) {

    /**
     * A variant row.
     *
     * @param sold the row is in an order (or reviewed): color, size cannot change and it cannot be deleted (BR-15)
     */
    public record Row(int id, String color, String size, int stockQuantity, String imgUrl, boolean sold, long version) {
    }

    /** A discount campaign: the Discount rows of the product sharing a start date (FR-34). */
    public record Campaign(BigDecimal discountValue, LocalDateTime startDate, LocalDateTime endDate, String state,
                           boolean editable) {
    }

    /** One product in the staff list. */
    public record Summary(int firstId, String name, ProductCategory category, BigDecimal basePrice, boolean active,
                          int variants, int stock, String imgUrl) {
    }
}
