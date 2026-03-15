package com.shopee.ecommerce.module.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

// ── CreateCategoryRequest ─────────────────────────────────────────────────────
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 255)
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(min = 2, max = 255)
    private String slug;

    private String description;
    private String imageUrl;
    private Long   parentId;
    private Short  sortOrder;
}
