package com.shopee.ecommerce.module.product.specification;

import com.shopee.ecommerce.module.product.entity.Product;
import com.shopee.ecommerce.module.product.entity.Product.ProductStatus;
import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Composable JPA Specifications for dynamic product filtering.
 *
 * Usage in service layer:
 * <pre>
 * Specification<Product> spec = ProductSpecification.build(filter);
 * Page<Product> page = productRepository.findAll(spec, pageable);
 * </pre>
 *
 * Each static method is a single predicate that can be combined with
 * {@code Specification.where(a).and(b).and(c)} for AND semantics,
 * or {@code .or()} for OR semantics.
 *
 * Why Specifications instead of @Query?
 *  - The filter criteria are optional and combinable at runtime
 *  - @Query would require 2^N methods for N optional filters
 *  - Specifications stay type-safe and testable in isolation
 */
@UtilityClass
public class ProductSpecification {

    // ── Individual predicates ─────────────────────────────────────────────────

    /** Only ACTIVE products (public catalog). */
    public static Specification<Product> isActive() {
        return (root, query, cb) ->
                cb.equal(root.get("status"), ProductStatus.ACTIVE);
    }

    /** Exact status match — for admin queries. */
    public static Specification<Product> hasStatus(ProductStatus status) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }

    /** Full-text search on name and shortDescription using ILIKE. */
    public static Specification<Product> nameContains(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) return null;
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")),             pattern),
                cb.like(cb.lower(root.get("shortDescription")), pattern)
            );
        };
    }

    /** Filter to a specific category (exact match — no descendants). */
    public static Specification<Product> inCategory(Long categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) return null;
            return cb.equal(root.get("category").get("id"), categoryId);
        };
    }

    /** Filter to a list of categories (used with recursive descendant lookup). */
    public static Specification<Product> inCategories(List<Long> categoryIds) {
        return (root, query, cb) -> {
            if (CollectionUtils.isEmpty(categoryIds)) return null;
            return root.get("category").get("id").in(categoryIds);
        };
    }

    /** Minimum price inclusive. */
    public static Specification<Product> priceGreaterThanOrEqual(BigDecimal min) {
        return (root, query, cb) -> {
            if (min == null) return null;
            return cb.greaterThanOrEqualTo(root.get("basePrice"), min);
        };
    }

    /** Maximum price inclusive. */
    public static Specification<Product> priceLessThanOrEqual(BigDecimal max) {
        return (root, query, cb) -> {
            if (max == null) return null;
            return cb.lessThanOrEqualTo(root.get("basePrice"), max);
        };
    }

    /** Minimum average rating. */
    public static Specification<Product> ratingAtLeast(BigDecimal minRating) {
        return (root, query, cb) -> {
            if (minRating == null) return null;
            return cb.greaterThanOrEqualTo(root.get("ratingAvg"), minRating);
        };
    }

    /** Featured products only. */
    public static Specification<Product> isFeatured() {
        return (root, query, cb) -> cb.isTrue(root.get("featured"));
    }

    /** Products with stock > 0 (in-stock filter). */
    public static Specification<Product> inStock() {
        return (root, query, cb) -> cb.greaterThan(root.get("totalStock"), 0);
    }

    /** Products belonging to a specific seller. */
    public static Specification<Product> bySeller(java.util.UUID sellerId) {
        return (root, query, cb) -> {
            if (sellerId == null) return null;
            return cb.equal(root.get("seller").get("id"), sellerId);
        };
    }

    // ── Composite builder ─────────────────────────────────────────────────────

    /**
     * Build a single {@link Specification} from a {@link ProductFilterRequest}.
     *
     * All null / blank criteria are ignored (treated as "no filter for this field").
     * Criteria are combined with AND.
     *
     * @param filter  the incoming filter request (may have nulls)
     * @param categoryIds pre-resolved descendant IDs (pass null to skip)
     * @param adminMode   if true, does NOT add the isActive() constraint
     */
    public static Specification<Product> build(
            ProductFilterRequest filter,
            List<Long> categoryIds,
            boolean adminMode
    ) {
        Specification<Product> spec = Specification.where(null);

        // Status
        if (!adminMode) {
            spec = spec.and(isActive());
        } else if (filter.getStatus() != null) {
            spec = spec.and(hasStatus(filter.getStatus()));
        }

        // Text search
        if (StringUtils.hasText(filter.getKeyword())) {
            spec = spec.and(nameContains(filter.getKeyword()));
        }

        // Category (prefer the pre-resolved descendant list)
        if (!CollectionUtils.isEmpty(categoryIds)) {
            spec = spec.and(inCategories(categoryIds));
        } else if (filter.getCategoryId() != null) {
            spec = spec.and(inCategory(filter.getCategoryId()));
        }

        // Price range
        if (filter.getMinPrice() != null) spec = spec.and(priceGreaterThanOrEqual(filter.getMinPrice()));
        if (filter.getMaxPrice() != null) spec = spec.and(priceLessThanOrEqual(filter.getMaxPrice()));

        // Rating
        if (filter.getMinRating() != null) spec = spec.and(ratingAtLeast(filter.getMinRating()));

        // In-stock only
        if (Boolean.TRUE.equals(filter.getInStockOnly())) spec = spec.and(inStock());

        // Featured only
        if (Boolean.TRUE.equals(filter.getFeaturedOnly())) spec = spec.and(isFeatured());

        // Seller
        if (filter.getSellerId() != null) spec = spec.and(bySeller(filter.getSellerId()));

        return spec;
    }

    // ── Embedded filter request ───────────────────────────────────────────────

    /**
     * Value object holding all optional filter criteria.
     * Passed from controller → service → specification builder.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ProductFilterRequest {
        private String      keyword;
        private Long        categoryId;
        private BigDecimal  minPrice;
        private BigDecimal  maxPrice;
        private BigDecimal  minRating;
        private Boolean     inStockOnly;
        private Boolean     featuredOnly;
        private java.util.UUID sellerId;
        private ProductStatus  status;       // admin-only
    }
}
