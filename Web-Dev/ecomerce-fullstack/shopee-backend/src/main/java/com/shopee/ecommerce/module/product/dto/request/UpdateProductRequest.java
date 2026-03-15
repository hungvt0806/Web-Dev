package com.shopee.ecommerce.module.product.dto.request;

import com.shopee.ecommerce.module.product.entity.Product.ProductStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payload for PUT /api/admin/products/{id}.
 *
 * All fields are optional (patch semantics) — only non-null fields are applied.
 * Variants are managed separately via PATCH /api/admin/products/{id}/variants.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {

    @Size(min = 3, max = 500)
    private String name;

    @Size(max = 1000)
    private String shortDescription;

    private String description;

    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 13, fraction = 2)
    private BigDecimal basePrice;

    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 13, fraction = 2)
    private BigDecimal originalPrice;

    private Long categoryId;

    @Size(max = 20)
    private List<@NotBlank @Size(max = 50) String> tags;

    @PositiveOrZero
    private Integer weightGrams;

    private ProductStatus status;

    private Boolean featured;
}
