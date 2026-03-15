package com.shopee.ecommerce.module.product.repository;

import com.shopee.ecommerce.module.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderBySortOrderAsc(Long productId);

    long countByProductId(Long productId);

    @Modifying
    @Query("DELETE FROM ProductImage i WHERE i.product.id = :productId")
    void deleteAllByProductId(@Param("productId") Long productId);
}