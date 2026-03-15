package com.shopee.ecommerce.module.order.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * A single line item within an order.
 *
 * SNAPSHOT DESIGN — all product fields are copied at order-placement time:
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │  product_name, product_image, sku, unit_price, variant_attributes   │
 * │  are all snapshotted so the order record remains accurate even if    │
 * │  the product is later edited, repriced, or deleted.                  │
 * │                                                                      │
 * │  The product_id / variant_id FK columns are kept as nullable         │
 * │  soft references for analytics — they are NOT relied on for          │
 * │  rendering the order history UI (use the snapshot fields instead).   │
 * └──────────────────────────────────────────────────────────────────────┘
 */
@Entity
@Table(
    name = "order_items",
    indexes = {
        @Index(name = "idx_oi_order_id",   columnList = "order_id"),
        @Index(name = "idx_oi_product_id", columnList = "product_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Parent order ──────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // ── Soft references (nullable — product may be deleted later) ─────────────

    /** Nullable FK — kept for analytics joins only. */
    @Column(name = "product_id")
    private Long productId;

    /** Nullable FK. */
    @Column(name = "variant_id")
    private Long variantId;

    // ── Snapshots (always populated, immutable after creation) ────────────────

    @Column(name = "product_name", nullable = false, length = 500)
    private String productName;

    @Column(name = "product_image", columnDefinition = "TEXT")
    private String productImage;

    @Column(name = "sku", length = 100)
    private String sku;

    /**
     * Variant attributes at the moment of purchase.
     * e.g. {"color": "Red", "size": "M"}
     * Null for products with no variants.
     */
    @Type(JsonType.class)
    @Column(name = "variant_attributes", columnDefinition = "jsonb")
    private Map<String, String> variantAttributes;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    /** unit_price × quantity — pre-computed and stored for reporting queries. */
    @Column(name = "line_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotal;

    // ── Review tracking ───────────────────────────────────────────────────────

    /** Set to true once the buyer submits a review for this item. */
    @Column(name = "is_reviewed", nullable = false)
    @Builder.Default
    private boolean reviewed = false;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Compute and store the line total.
     * Must be called before persisting when unit_price or quantity is set/changed.
     */
    public void computeLineTotal() {
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Returns a human-readable description combining name and variant attributes.
     * e.g. "iPhone 15 Pro (Color: Red, Storage: 256GB)"
     */
    public String getDisplayName() {
        if (variantAttributes == null || variantAttributes.isEmpty()) {
            return productName;
        }
        StringBuilder attrs = new StringBuilder();
        variantAttributes.forEach((k, v) -> {
            if (!attrs.isEmpty()) attrs.append(", ");
            attrs.append(capitalize(k)).append(": ").append(v);
        });
        return productName + " (" + attrs + ")";
    }

    private static String capitalize(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
