package com.shopee.ecommerce.module.product.repository;

import com.shopee.ecommerce.module.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // ── Public catalog queries ────────────────────────────────────────────────

    @Query("""
        SELECT p FROM Product p
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.seller
        WHERE p.slug = :slug
          AND p.status = com.shopee.ecommerce.module.product.entity.Product$ProductStatus.ACTIVE
        """)
    Optional<Product> findActiveBySlug(@Param("slug") String slug);

    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.seller
        LEFT JOIN FETCH p.variants v
        WHERE p.id = :id
          AND p.status = com.shopee.ecommerce.module.product.entity.Product$ProductStatus.ACTIVE
        """)
    Optional<Product> findActiveByIdWithDetails(@Param("id") Long id);

    @Query("""
        SELECT p FROM Product p
        WHERE p.status = com.shopee.ecommerce.module.product.entity.Product$ProductStatus.ACTIVE
          AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(p.shortDescription) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<Product> searchActive(@Param("q") String query, Pageable pageable);

    @Query("""
        SELECT p FROM Product p
        WHERE p.status = com.shopee.ecommerce.module.product.entity.Product$ProductStatus.ACTIVE
          AND p.category.id IN :categoryIds
        """)
    Page<Product> findActiveByCategoryIds(
            @Param("categoryIds") List<Long> categoryIds, Pageable pageable);

    @Query("""
        SELECT p FROM Product p
        LEFT JOIN FETCH p.category
        WHERE p.status = com.shopee.ecommerce.module.product.entity.Product$ProductStatus.ACTIVE
          AND p.featured = TRUE
        ORDER BY p.createdAt DESC
        """)
    List<Product> findFeatured(Pageable pageable);

    @Query("""
        SELECT p FROM Product p
        WHERE p.status = com.shopee.ecommerce.module.product.entity.Product$ProductStatus.ACTIVE
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND p.basePrice BETWEEN :minPrice AND :maxPrice
        """)
    Page<Product> findByPriceRange(
            @Param("categoryId")  Long categoryId,
            @Param("minPrice")    BigDecimal minPrice,
            @Param("maxPrice")    BigDecimal maxPrice,
            Pageable pageable);

    Page<Product> findBySellerIdAndStatus(
            UUID sellerId,
            Product.ProductStatus status,
            Pageable pageable);

    @Query("""
        SELECT p FROM Product p
        WHERE p.status = com.shopee.ecommerce.module.product.entity.Product$ProductStatus.ACTIVE
          AND p.category.id = :categoryId
          AND p.id <> :excludeId
        ORDER BY p.ratingAvg DESC
        """)
    List<Product> findRelated(
            @Param("categoryId") Long categoryId,
            @Param("excludeId")  Long excludeId,
            Pageable pageable);

    // ── Existence checks ──────────────────────────────────────────────────────

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    // ── Admin queries ─────────────────────────────────────────────────────────

    @Query("""
        SELECT p FROM Product p
        LEFT JOIN FETCH p.category
        WHERE (:sellerId IS NULL OR p.seller.id = :sellerId)
        """)
    Page<Product> findAllForAdmin(
            @Param("sellerId") UUID sellerId, Pageable pageable);

    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.seller
        LEFT JOIN FETCH p.variants
        WHERE p.id = :id
        """)
    Optional<Product> findByIdWithDetails(@Param("id") Long id);

    // ── Statistics & stock ────────────────────────────────────────────────────

    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = 'ACTIVE'")
    long countActive();

    @Modifying
    @Query("UPDATE Product p SET p.soldCount = p.soldCount + :qty WHERE p.id = :id")
    int incrementSoldCount(@Param("id") Long id, @Param("qty") int qty);

    /**
     * Atomically decrement totalStock — prevents overselling products without variants.
     * Returns 1 if updated, 0 if stock was insufficient (race condition guard).
     */
    @Modifying
    @Query("""
        UPDATE Product p
        SET p.totalStock = p.totalStock - :qty
        WHERE p.id = :id AND p.totalStock >= :qty
        """)
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);

    /**
     * Restore totalStock when an order is cancelled.
     */
    @Modifying
    @Query("UPDATE Product p SET p.totalStock = p.totalStock + :qty WHERE p.id = :id")
    int incrementStock(@Param("id") Long id, @Param("qty") int qty);
}