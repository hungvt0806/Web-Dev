package com.shopee.ecommerce.module.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Persisted product image — one row per uploaded image.
 *
 * This normalised table coexists with the product.image_urls JSONB array.
 * The JSONB array is used for fast read queries (catalog listing), while
 * this table holds the authoritative ordered list for admin management.
 *
 * sort_order controls display sequence; thumbnail is index 0.
 */
@Entity
@Table(
    name = "product_images",
    indexes = @Index(name = "idx_pimg_product_id", columnList = "product_id")
)
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private short sortOrder = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
