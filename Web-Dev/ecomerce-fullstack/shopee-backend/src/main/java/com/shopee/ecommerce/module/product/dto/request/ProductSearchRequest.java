package com.shopee.ecommerce.module.product.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;

/**
 * Query parameters for GET /api/products.
 *
 * Bound via @ModelAttribute — all fields are optional.
 * The service layer translates this into a {@link com.shopee.ecommerce.module.product.specification.ProductSpecification}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchRequest {

    /** Free text search against name and short description. */
    private String keyword;

    /** Filter to this category and all its descendants. */
    private Long categoryId;

    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum price cannot be negative")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal minPrice;

    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 13, fraction = 2)
    private BigDecimal maxPrice;

    @DecimalMin(value = "1.0") @DecimalMax(value = "5.0")
    private BigDecimal minRating;

    /** When true, only return products with totalStock > 0. */
    private Boolean inStockOnly;

    /** When true, only return featured products. */
    private Boolean featuredOnly;

    // ── Pagination ────────────────────────────────────────────────────────────

    @Min(value = 0) @Builder.Default
    private int page = 0;

    @Min(value = 1) @Max(value = 100) @Builder.Default
    private int size = 20;

    /**
     * Sort field. Allowed values: price, rating, sold, newest, name.
     * Default: newest.
     */
    @Builder.Default
    private String sortBy = "newest";

    @Builder.Default
    private String sortDir = "desc";

    // ── Helpers ───────────────────────────────────────────────────────────────

    public Sort toSort() {
        Sort.Direction dir = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        String column = switch (sortBy.toLowerCase()) {
            case "price"  -> "basePrice";
            case "rating" -> "ratingAvg";
            case "sold"   -> "soldCount";
            case "name"   -> "name";
            default       -> "createdAt";
        };
        return Sort.by(dir, column);
    }
}
