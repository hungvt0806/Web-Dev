package com.shopee.ecommerce.module.order.repository;

import com.shopee.ecommerce.module.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(UUID orderId);

    /** Check if a buyer has actually purchased a product — gates review submission. */
    @Query("""
        SELECT CASE WHEN COUNT(oi) > 0 THEN TRUE ELSE FALSE END
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.buyer.id   = :buyerId
          AND oi.productId = :productId
          AND o.status     = 'DELIVERED'
        """)
    boolean existsDeliveredPurchaseByBuyer(
            @Param("buyerId")   UUID buyerId,
            @Param("productId") Long productId);

    /** Mark a single item as reviewed (called after review submission). */
    @Modifying
    @Query("UPDATE OrderItem oi SET oi.reviewed = true WHERE oi.id = :id")
    int markReviewed(@Param("id") Long id);

    /** Items not yet reviewed — used to prompt the buyer for reviews. */
    @Query("""
        SELECT oi FROM OrderItem oi
        JOIN oi.order o
        WHERE o.buyer.id   = :buyerId
          AND o.status     = 'DELIVERED'
          AND oi.reviewed  = false
        """)
    List<OrderItem> findUnreviewedItemsForBuyer(@Param("buyerId") UUID buyerId);
}
