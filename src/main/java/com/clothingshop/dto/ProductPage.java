package com.clothingshop.dto;

import java.util.List;

/**
 * One page of shop results.
 *
 * @param page       1-based page number actually shown (an out-of-range request is moved to the nearest page)
 * @param totalItems number of products matching the filter, over all pages
 */
public record ProductPage(List<ProductSummary> items, int page, int totalPages, int totalItems) {

    /** Cuts page {@code page} of {@code pageSize} items out of {@code all}. */
    public static ProductPage of(List<ProductSummary> all, int page, int pageSize) {
        int totalPages = Math.max(1, (all.size() + pageSize - 1) / pageSize);
        int shown = Math.min(Math.max(page, 1), totalPages);
        int from = (shown - 1) * pageSize;
        List<ProductSummary> items = List.copyOf(all.subList(from, Math.min(all.size(), from + pageSize)));
        return new ProductPage(items, shown, totalPages, all.size());
    }
}
