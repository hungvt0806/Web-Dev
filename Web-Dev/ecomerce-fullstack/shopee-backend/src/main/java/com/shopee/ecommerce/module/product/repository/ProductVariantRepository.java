package com.shopee.ecommerce.module.product.repository;

import com.shopee.ecommerce.module.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdAndActiveTrue(Long productId);

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    /** Atomically decrement stock — prevents overselling. */
    @Modifying
    @Query("""
        UPDATE ProductVariant v
        SET v.stock = v.stock - :qty
        WHERE v.id = :id AND v.stock >= :qty
        """)
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE ProductVariant v SET v.stock = v.stock + :qty WHERE v.id = :id")
    int incrementStock(@Param("id") Long id, @Param("qty") int qty);

    @Query("SELECT COALESCE(SUM(v.stock), 0) FROM ProductVariant v WHERE v.product.id = :productId AND v.active = TRUE")
    int sumStockByProductId(@Param("productId") Long productId);
}