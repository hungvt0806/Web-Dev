package com.shopee.ecommerce.module.category.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Category response DTO.
 *
 * The {@code children} field is populated only in tree-mode responses
 * (e.g. GET /api/categories/tree) and omitted in flat listing responses.
 *
 * JsonInclude(NON_EMPTY) means empty children lists are excluded from JSON.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponse {

    private Long   id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private Short  sortOrder;
    private boolean active;

    /** Parent summary — omitted for root categories. */
    private ParentInfo parent;

    /** Nested children — only in tree responses. */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Builder.Default
    private List<CategoryResponse> children = new ArrayList<>();

    /** Product count in this category (approximate, for admin). */
    private Long productCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter @Builder
    public static class ParentInfo {
        private Long   id;
        private String name;
        private String slug;
    }
}
