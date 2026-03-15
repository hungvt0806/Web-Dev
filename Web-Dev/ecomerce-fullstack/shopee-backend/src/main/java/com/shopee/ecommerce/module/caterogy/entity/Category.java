package com.shopee.ecommerce.module.category.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Category entity — self-referencing adjacency list tree.
 *
 * Supports unlimited nesting depth (root → Electronics → Smartphones → Flagship).
 * Use recursive CTEs in PostgreSQL for breadcrumb queries.
 *
 * Relationship to products: One category can have many products.
 * Products always belong to the leaf category (most specific).
 */
@Entity
@Table(
    name = "categories",
    indexes = {
        @Index(name = "idx_cat_slug",      columnList = "slug",      unique = true),
        @Index(name = "idx_cat_parent_id", columnList = "parent_id"),
        @Index(name = "idx_cat_active",    columnList = "is_active")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Hierarchy ─────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    /**
     * Direct children — NOT loaded by default.
     * Loaded explicitly only in admin tree-building queries.
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Category> children = new ArrayList<>();

    // ── Fields ────────────────────────────────────────────────────────────────

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** URL-safe identifier, e.g. "smartphones", "mens-clothing". */
    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private short sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean isRoot() {
        return parent == null;
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }
}
