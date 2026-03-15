package com.shopee.ecommerce.module.order.repository;

import com.shopee.ecommerce.module.order.entity.Order;
import com.shopee.ecommerce.module.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository
        extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    // ── Buyer: order history ──────────────────────────────────────────────────

    /**
     * Paginated order history for a buyer — excludes items for performance.
     * Items are loaded separately on the detail page.
     */
    @Query("""
        SELECT o FROM Order o
        WHERE o.buyer.id = :buyerId
        ORDER BY o.createdAt DESC
        """)
    Page<Order> findByBuyerId(@Param("buyerId") UUID buyerId, Pageable pageable);

    /**
     * History filtered by status (e.g. buyer views "Active Orders" tab).
     */
    @Query("""
        SELECT o FROM Order o
        WHERE o.buyer.id = :buyerId
          AND o.status   = :status
        ORDER BY o.createdAt DESC
        """)
    Page<Order> findByBuyerIdAndStatus(
            @Param("buyerId") UUID buyerId,
            @Param("status")  OrderStatus status,
            Pageable pageable);

    // ── Buyer: order detail ───────────────────────────────────────────────────

    /**
     * Fetch one order with all items and status history in a single query.
     * Used for the order detail page — buyer must own the order.
     */
    @Query("""
    SELECT DISTINCT o FROM Order o
    LEFT JOIN FETCH o.items
    WHERE o.id        = :id
      AND o.buyer.id  = :buyerId
    """)
    Optional<Order> findByIdAndBuyerIdWithDetails(
            @Param("id")      UUID id,
            @Param("buyerId") UUID buyerId);

    /**
     * Find by human-readable order number (for "Track my order" flow).
     */
    @Query("""
    SELECT DISTINCT o FROM Order o
    LEFT JOIN FETCH o.items
    WHERE o.orderNumber = :orderNumber
      AND o.buyer.id    = :buyerId
    """)
    Optional<Order> findByOrderNumberAndBuyerId(
            @Param("orderNumber") String orderNumber,
            @Param("buyerId")     UUID buyerId);

    // ── Admin: all orders ─────────────────────────────────────────────────────

    @Query("""
        SELECT o FROM Order o
        LEFT JOIN FETCH o.buyer
        WHERE (:status IS NULL OR o.status = :status)
        ORDER BY o.createdAt DESC
        """)
    Page<Order> findAllForAdmin(
            @Param("status") OrderStatus status,
            Pageable pageable);

    /**
     * Admin: full detail of any order (no buyer constraint).
     */
    @Query("""
    SELECT DISTINCT o FROM Order o
    LEFT JOIN FETCH o.buyer
    LEFT JOIN FETCH o.items
    WHERE o.id = :id
    """)
    Optional<Order> findByIdWithDetails(@Param("id") UUID id);

    // ── Existence / counts ────────────────────────────────────────────────────

    boolean existsByOrderNumber(String orderNumber);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);

    // ───────────────────────────────────────────────────────
    @Query("""
    SELECT DISTINCT o FROM Order o
    LEFT JOIN FETCH o.statusHistory
    WHERE o.id = :id
    """)
    Optional<Order> findByIdWithStatusHistory(@Param("id") UUID id);


    // ── Analytics queries ─────────────────────────────────────────────────────

    @Query("""
        SELECT o.status, COUNT(o)
        FROM Order o
        GROUP BY o.status
        """)
    List<Object[]> countGroupedByStatus();

    @Query("""
        SELECT COALESCE(SUM(o.totalAmount), 0)
        FROM Order o
        WHERE o.status = 'DELIVERED'
          AND o.createdAt BETWEEN :from AND :to
        """)
    java.math.BigDecimal sumRevenueBetween(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);
}
