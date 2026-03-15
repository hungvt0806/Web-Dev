package com.shopee.ecommerce.module.product.dto.request;

import com.shopee.ecommerce.module.product.entity.Product.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// ═══════════════════════════════════════════════════════════════════════════
//  CreateProductRequest
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Payload for POST /api/admin/products.
 *
 * A product can be created with DRAFT status (no variants required yet)
 * or ACTIVE status (at least one variant with price/stock required).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 500, message = "Name must be 3–500 characters")
    private String name;

    @Size(max = 1000, message = "Short description max 1000 characters")
    private String shortDescription;

    private String description;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 13, fraction = 2, message = "Price must be a valid monetary amount")
    private BigDecimal basePrice;

    @DecimalMin(value = "0.0", inclusive = false, message = "Original price must be greater than 0")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal originalPrice;

    private Long categoryId;

    @Size(max = 20, message = "Maximum 20 tags allowed")
    @Builder.Default
    private List<@NotBlank @Size(max = 50) String> tags = new ArrayList<>();

    @PositiveOrZero(message = "Weight must be 0 or positive")
    private Integer weightGrams;

    @Builder.Default
    @Valid
    private List<VariantRequest> variants = new ArrayList<>();

    /** Initial status — defaults to DRAFT if not provided. */
    private ProductStatus status;

    private Boolean featured;

    // ── Nested: variant ───────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantRequest {

        @NotBlank(message = "SKU is required")
        @Size(max = 100)
        private String sku;

        /** e.g. {"color": "Red", "size": "M"} */
        private Map<String, String> attributes;

        @NotNull(message = "Variant price is required")
        @DecimalMin(value = "0.0", inclusive = false)
        @Digits(integer = 13, fraction = 2)
        private BigDecimal price;

        @NotNull(message = "Stock quantity is required")
        @Min(value = 0, message = "Stock cannot be negative")
        private Integer stock;

        private String imageUrl;
    }
}
