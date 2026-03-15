package com.shopee.ecommerce.module.product.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shopee.ecommerce.module.product.entity.Product.ProductStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Product data for admin/seller panel.
 * Includes status and audit fields that should not be exposed to buyers.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminProductResponse {

    private Long          id;
    private String        name;
    private String        slug;
    private String        shortDescription;
    private String        description;

    private BigDecimal    basePrice;
    private BigDecimal    originalPrice;
    private String        currency;

    private String        thumbnailUrl;
    private List<String>  imageUrls;
    private List<String>  tags;

    private BigDecimal    ratingAvg;
    private int           ratingCount;
    private int           soldCount;
    private int           totalStock;

    private Integer       weightGrams;

    /** Included only in admin responses to avoid leaking status to buyers. */
    private ProductStatus status;
    private boolean       featured;

    private List<ProductDetailResponse.VariantResponse> variants;
    private ProductSummaryResponse.CategoryInfo         category;
    private ProductDetailResponse.SellerInfo            seller;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}