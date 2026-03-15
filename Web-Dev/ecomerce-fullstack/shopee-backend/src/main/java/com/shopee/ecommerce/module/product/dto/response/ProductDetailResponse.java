package com.shopee.ecommerce.module.product.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Complete product data for the detail page.
 * Includes variants, all images, seller info, and full description.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDetailResponse {

    private Long         id;
    private String       name;
    private String       slug;
    private String       description;
    private String       shortDescription;

    private BigDecimal   basePrice;
    private BigDecimal   originalPrice;
    private String       currency;
    private int          discountPercent;

    private String       thumbnailUrl;
    private List<String> imageUrls;
    private List<String> tags;

    private BigDecimal   ratingAvg;
    private int          ratingCount;
    private int          soldCount;
    private int          totalStock;

    private boolean      available;
    private boolean      featured;

    private Integer      weightGrams;

    private List<VariantResponse>        variants;
    private ProductSummaryResponse.CategoryInfo category;
    private SellerInfo                   seller;
    private List<ProductSummaryResponse> related; // up to 6

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Nested ───────────────────────────────────────────────────────────────

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VariantResponse {
        private Long                id;
        private String              sku;
        private Map<String, String> attributes;
        private BigDecimal          price;
        private int                 stock;
        private boolean             inStock;
        private String              imageUrl;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerInfo {
        private UUID   id;
        private String shopName;   // mapped from User.fullName in ProductMapper
        private String avatarUrl;
    }
}