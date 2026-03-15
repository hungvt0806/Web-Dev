package com.shopee.ecommerce.module.product.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lightweight product card for catalog/search results.
 * Excludes variants, full description, and seller details to keep
 * the payload small when rendering a grid of 20–60 cards.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductSummaryResponse {

    private Long id;
    private String name;
    private String slug;
    private String shortDescription;

    private BigDecimal basePrice;
    private BigDecimal originalPrice;
    private String currency;

    /**
     * Pre-computed discount percentage (0 if no discount).
     */
    private int discountPercent;

    private String thumbnailUrl;

    private BigDecimal ratingAvg;
    private int ratingCount;
    private int soldCount;
    private int totalStock;

    private boolean available;
    private boolean featured;

    private CategoryInfo category;

    private LocalDateTime createdAt;

    // ── Nested ────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class CategoryInfo {
        private Long id;
        private String name;
        private String slug;
    }
}
