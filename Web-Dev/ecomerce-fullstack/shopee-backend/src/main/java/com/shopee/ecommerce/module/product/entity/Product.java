package com.shopee.ecommerce.module.product.entity;

import com.shopee.ecommerce.module.category.entity.Category;
import com.shopee.ecommerce.module.user.entity.User;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Product entity — the core of the catalog.
 *
 * Design decisions:
 * ┌──────────────────────────────────────────────────────────────────┐
 * │ • image_urls stored as JSONB string array for flexible ordering   │
 * │ • tags stored as JSONB string array for GIN-indexed search        │
 * │ • rating_avg/count are denormalized for query performance         │
 * │   → updated by a DB trigger when a review is inserted/updated     │
 * │ • status enum controls visibility (DRAFT → ACTIVE → PAUSED)      │
 * │ • total_stock denormalized from product_variants via DB trigger   │
 * └──────────────────────────────────────────────────────────────────┘
 */
@Entity
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_prod_slug",        columnList = "slug",        unique = true),
        @Index(name = "idx_prod_seller",       columnList = "seller_id"),
        @Index(name = "idx_prod_category",     columnList = "category_id"),
        @Index(name = "idx_prod_status",       columnList = "status"),
        @Index(name = "idx_prod_price",        columnList = "base_price"),
        @Index(name = "idx_prod_rating",       columnList = "rating_avg"),
        @Index(name = "idx_prod_featured",     columnList = "is_featured"),
        @Index(name = "idx_prod_created",      columnList = "created_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    // ── Identity ──────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 500)
    private String name;

    /** URL slug — unique, auto-generated from name in service layer. */
    @Column(name = "slug", nullable = false, unique = true, length = 500)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_description", length = 1000)
    private String shortDescription;

    // ── Pricing ───────────────────────────────────────────────────────────────

    /**
     * Current selling price. Always use NUMERIC/BigDecimal — never float/double
     * for monetary values to avoid floating-point precision errors.
     */
    @Column(name = "base_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal basePrice;

    /** Original / crossed-out price. null means no discount display. */
    @Column(name = "original_price", precision = 15, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "JPY";

    // ── Inventory ─────────────────────────────────────────────────────────────

    /** Aggregate stock — synced from product_variants by DB trigger. */
    @Column(name = "total_stock", nullable = false)
    @Builder.Default
    private int totalStock = 0;

    @Column(name = "sold_count", nullable = false)
    @Builder.Default
    private int soldCount = 0;

    // ── Media ─────────────────────────────────────────────────────────────────

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    /**
     * Ordered list of image URLs stored as a JSONB string array.
     * ["https://cdn.example.com/img1.jpg", "https://cdn.example.com/img2.jpg"]
     */
    @Type(JsonType.class)
    @Column(name = "image_urls", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    // ── Search ────────────────────────────────────────────────────────────────

    /**
     * Free-form tags for filtering/faceting.
     * Stored as JSONB; PostgreSQL GIN-indexed for fast array containment queries.
     * e.g. ["sale", "new", "trending", "summer"]
     */
    @Type(JsonType.class)
    @Column(name = "tags", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    // ── Ratings (denormalized) ────────────────────────────────────────────────

    /** Aggregate rating — updated by DB trigger on reviews table. */
    @Column(name = "rating_avg", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(name = "rating_count", nullable = false)
    @Builder.Default
    private int ratingCount = 0;

    // ── Logistics ─────────────────────────────────────────────────────────────

    /** Weight in grams — used for shipping fee calculation. */
    @Column(name = "weight_grams")
    private Integer weightGrams;

    // ── Relationships ─────────────────────────────────────────────────────────

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    // ── Variants ─────────────────────────────────────────────────────────────

    @JsonIgnore
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    // ── Status ────────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.DRAFT;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private boolean featured = false;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Enums ─────────────────────────────────────────────────────────────────

    public enum ProductStatus {
        /** Not yet published — only visible to seller/admin. */
        DRAFT,
        /** Published and visible to all users. */
        ACTIVE,
        /** Temporarily hidden by seller. */
        PAUSED,
        /** Soft-deleted — invisible to all users and search. */
        DELETED
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean isAvailable() {
        return status == ProductStatus.ACTIVE && totalStock > 0;
    }

    public boolean hasDiscount() {
        return originalPrice != null && originalPrice.compareTo(basePrice) > 0;
    }

    public int getDiscountPercent() {
        if (!hasDiscount()) return 0;
        return originalPrice.subtract(basePrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(originalPrice, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    public void addVariant(ProductVariant variant) {
        variants.add(variant);
        variant.setProduct(this);
    }

    public void removeVariant(ProductVariant variant) {
        variants.remove(variant);
        variant.setProduct(null);
    }
}
