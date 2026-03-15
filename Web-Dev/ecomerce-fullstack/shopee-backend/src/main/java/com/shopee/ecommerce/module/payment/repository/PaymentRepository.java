package com.shopee.ecommerce.module.payment.repository;

import com.shopee.ecommerce.module.payment.entity.Payment;
import com.shopee.ecommerce.module.payment.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // ── Primary lookups ───────────────────────────────────────────────────────

    Optional<Payment> findByMerchantPaymentId(String merchantPaymentId);

    Optional<Payment> findByPaypayPaymentId(String paypayPaymentId);

    /** Latest payment for a given order — used when reloading payment state. */
    @Query("""
        SELECT p FROM Payment p
        WHERE p.orderId = :orderId
        ORDER BY p.createdAt DESC
        LIMIT 1
        """)
    Optional<Payment> findLatestByOrderId(@Param("orderId") UUID orderId);

    /** All payments for an order (may have multiple if user retried). */
    List<Payment> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    // ── Idempotency guard ─────────────────────────────────────────────────────

    /**
     * Returns true if a webhook with this payload hash has already been processed.
     * Used to reject duplicate webhook deliveries.
     */
    boolean existsByWebhookPayloadHash(String hash);

    // ── Buyer lookup ──────────────────────────────────────────────────────────

    Page<Payment> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId, Pageable pageable);

    // ── Admin / ops queries ───────────────────────────────────────────────────

    @Query("""
        SELECT p FROM Payment p
        WHERE p.status = :status
        ORDER BY p.createdAt DESC
        """)
    Page<Payment> findByStatus(@Param("status") PaymentStatus status, Pageable pageable);

    /**
     * Stale pending payments that have passed their expiry — for the cleanup scheduler.
     */
    @Query("""
        SELECT p FROM Payment p
        WHERE p.status IN ('INITIATED', 'AWAITING_CAPTURE')
          AND p.expiresAt IS NOT NULL
          AND p.expiresAt < :now
        """)
    List<Payment> findExpiredPendingPayments(@Param("now") LocalDateTime now);

    /**
     * Bulk-expire stale records — called by the nightly cleanup job.
     */
    @Modifying
    @Query("""
        UPDATE Payment p SET p.status = 'EXPIRED'
        WHERE p.status IN ('INITIATED', 'AWAITING_CAPTURE')
          AND p.expiresAt < :threshold
        """)
    int markExpiredBefore(@Param("threshold") LocalDateTime threshold);

    // ── Revenue analytics ─────────────────────────────────────────────────────

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.status = 'COMPLETED'
          AND p.capturedAt BETWEEN :from AND :to
        """)
    java.math.BigDecimal sumCapturedBetween(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status")
    long countByStatus(@Param("status") PaymentStatus status);
}
