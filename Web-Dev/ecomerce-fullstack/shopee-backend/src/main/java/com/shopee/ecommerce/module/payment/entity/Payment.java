package com.shopee.ecommerce.module.payment.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Persisted record of a payment transaction.
 *
 * Design decisions:
 * ┌───────────────────────────────────────────────────────────────────────┐
 * │ One Payment row per payment attempt — if the user cancels and retries │
 * │ a new row is created (the old one is marked CANCELLED).               │
 * │                                                                       │
 * │ merchant_payment_id  — our UUID sent to PayPay as the idempotency key │
 * │ paypay_payment_id    — PayPay's own internal ID (from their response) │
 * │ Both IDs are kept for reconciliation.                                  │
 * │                                                                       │
 * │ raw_webhook_payload  — the full PayPay webhook body stored as JSONB   │
 * │ for audit / replay without hitting the PayPay API again.              │
 * │                                                                       │
 * │ HMAC signature is verified before this row is ever written.           │
 * └───────────────────────────────────────────────────────────────────────┘
 */
@Entity
@Table(
    name = "payments",
    indexes = {
        @Index(name = "idx_pay_order_id",            columnList = "order_id"),
        @Index(name = "idx_pay_merchant_payment_id",  columnList = "merchant_payment_id", unique = true),
        @Index(name = "idx_pay_paypay_payment_id",    columnList = "paypay_payment_id"),
        @Index(name = "idx_pay_status",               columnList = "status"),
        @Index(name = "idx_pay_created",              columnList = "created_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    // ── Identity ──────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Our UUID sent to PayPay as the idempotency / merchant reference key.
     * Format: PAY-{orderId shortened}-{random suffix}
     * Unique — prevents duplicate charge if our callback fires twice.
     */
    @Column(name = "merchant_payment_id", nullable = false, unique = true, length = 64)
    private String merchantPaymentId;

    /**
     * PayPay's internal payment ID returned in their API response.
     * Used to look up the payment in PayPay's dashboard.
     * Null until PayPay has processed the request.
     */
    @Column(name = "paypay_payment_id", length = 128)
    private String paypayPaymentId;

    // ── Order link ────────────────────────────────────────────────────────────

    /**
     * The order this payment is for.
     * Stored as a plain UUID column (no FK constraint) so the payment
     * module can compile independently of the order module.
     * The service layer enforces the business relationship.
     */
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    // ── Financials ────────────────────────────────────────────────────────────

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "JPY";

    // ── Status ────────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    // ── PayPay URLs ───────────────────────────────────────────────────────────

    /** The redirect URL PayPay gives us — sent to the frontend to open PayPay. */
    @Column(name = "payment_url", columnDefinition = "TEXT")
    private String paymentUrl;

    /** Where PayPay redirects the user after success/failure. */
    @Column(name = "redirect_url", columnDefinition = "TEXT")
    private String redirectUrl;

    /** Our webhook endpoint URL that PayPay POSTs events to. */
    @Column(name = "webhook_url", columnDefinition = "TEXT")
    private String webhookUrl;

    // ── PayPay metadata ───────────────────────────────────────────────────────

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;   // e.g. "PAY_EASY", "WEB_CASHIER"

    @Column(name = "payment_request_expires_at")
    private LocalDateTime paymentRequestExpiresAt;

    /**
     * Full raw webhook payload stored as JSONB for audit and manual replay.
     * Never trust fields from this blob — use the verified and deserialized DTO instead.
     */
    @Type(JsonType.class)
    @Column(name = "raw_webhook_payload", columnDefinition = "jsonb")
    private Map<String, Object> rawWebhookPayload;

    /**
     * Idempotency guard: the SHA-256 hash of the webhook payload body.
     * Duplicate webhooks are rejected if this hash already exists.
     */
    @Column(name = "webhook_payload_hash", length = 64)
    private String webhookPayloadHash;

    // ── Timestamps ────────────────────────────────────────────────────────────

    @Column(name = "authorized_at")
    private LocalDateTime authorizedAt;

    @Column(name = "captured_at")
    private LocalDateTime capturedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean isCompleted() {
        return status == PaymentStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == PaymentStatus.FAILED || status == PaymentStatus.CANCELLED;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isPending() {
        return status == PaymentStatus.INITIATED || status == PaymentStatus.AWAITING_CAPTURE;
    }
}
