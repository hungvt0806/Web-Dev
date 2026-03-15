package com.shopee.ecommerce.module.product.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Product variant — a single purchasable SKU.
 *
 * Each product can have many variants representing different combinations
 * of attributes like size, color, storage capacity, etc.
 *
 * Examples:
 *  - "Red / Size M"  → attributes: {"color": "Red",  "size": "M"}
 *  - "128GB / Black" → attributes: {"storage": "128GB", "color": "Black"}
 *
 * If a product has no meaningful variants, create one default variant
 * with empty attributes and copy the product's base_price.
 */
@Entity
@Table(
    name = "product_variants",
    indexes = {
        @Index(name = "idx_pv_product_id", columnList = "product_id"),
        @Index(name = "idx_pv_sku",        columnList = "sku", unique = true)
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Stock Keeping Unit — unique across all products. */
    @Column(name = "sku", nullable = false, unique = true, length = 100)
    private String sku;

    /**
     * Flexible variant attributes stored as JSONB.
     * Key = attribute name (e.g. "color"), value = attribute value (e.g. "Red").
     */
    @Type(JsonType.class)
    @Column(name = "attributes", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, String> attributes = new HashMap<>();

    /** Variant-specific price — overrides the product's base_price when set. */
    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "stock", nullable = false)
    @Builder.Default
    private int stock = 0;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean isInStock() {
        return active && stock > 0;
    }

    public boolean canFulfill(int qty) {
        return isInStock() && stock >= qty;
    }
}
