package com.shopee.ecommerce.module.order.entity;

import com.shopee.ecommerce.module.user.entity.User;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Order aggregate root.
 *
 * Design decisions:
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │ UUID primary key — prevents order ID enumeration attacks             │
 * │ order_number  — human-readable reference, e.g. "ORD-20240115-A3F2"  │
 * │ shipping_address — JSONB snapshot; survives user address changes     │
 * │ StatusHistory — append-only audit log; never updated or deleted      │
 * │ Prices stored as NUMERIC(15,2) — never float                         │
 * │ total_amount = subtotal + shipping_fee - discount_amount             │
 * └──────────────────────────────────────────────────────────────────────┘
 */
@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_order_number",     columnList = "order_number",    unique = true),
        @Index(name = "idx_order_buyer",      columnList = "buyer_id"),
        @Index(name = "idx_order_status",     columnList = "status"),
        @Index(name = "idx_order_created",    columnList = "created_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    // ── Identity ──────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private java.util.UUID id;

    /**
     * Human-readable order reference shown to the buyer and used in support tickets.
     * Format: ORD-{yyyyMMdd}-{6 random uppercase alphanumeric chars}
     * Generated in the service layer, not here.
     */
    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    // ── Relationships ─────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    /**
     * Order items. CascadeType.ALL so items are persisted / deleted with the order.
     * Loaded LAZY — most queries don't need all items.
     */
    @OneToMany(
            mappedBy      = "order",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
            fetch         = FetchType.LAZY
    )
    @org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    /**
     * Append-only status audit log.
     * One row per transition — never updated or deleted.
     */
    @OneToMany(
            mappedBy  = "order",
            cascade   = CascadeType.ALL,
            fetch     = FetchType.LAZY
    )
    @OrderBy("createdAt ASC")
    @org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
    @Builder.Default
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    // ── Shipping ──────────────────────────────────────────────────────────────

    /**
     * Snapshot of the shipping address at order placement.
     * Stored as JSONB so it survives any subsequent address edits.
     *
     * Schema: { "fullName", "phone", "addressLine1", "addressLine2",
     *           "city", "province", "postalCode", "country" }
     */
    @Type(JsonType.class)
    @Column(name = "shipping_address", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> shippingAddress;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "shipping_carrier", length = 100)
    private String shippingCarrier;

    @Column(name = "estimated_delivery_date")
    private LocalDateTime estimatedDeliveryDate;

    // ── Financials ────────────────────────────────────────────────────────────

    /** Sum of all order item line totals (before shipping and discounts). */
    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "shipping_fee", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** subtotal + shippingFee − discountAmount. */
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "JPY";

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    // ── Status ────────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    /** Optional buyer-facing note (e.g. delivery instructions). */
    @Column(name = "buyer_note", columnDefinition = "TEXT")
    private String buyerNote;

    /** Internal note written by seller or support (not shown to buyer). */
    @Column(name = "internal_note", columnDefinition = "TEXT")
    private String internalNote;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    // ── Timestamps per status ─────────────────────────────────────────────────

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Helpers ───────────────────────────────────────────────────────────────

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void addStatusHistory(OrderStatusHistory entry) {
        statusHistory.add(entry);
        entry.setOrder(this);
    }

    public int getTotalQuantity() {
        return items.stream().mapToInt(OrderItem::getQuantity).sum();
    }

    public boolean isOwnedBy(java.util.UUID userId) {
        return buyer != null && buyer.getId().equals(userId);
    }

    public boolean isCancellableByBuyer() {
        return status.isBuyerCancellable();
    }

    /**
     * Recompute total_amount from the current subtotal, shippingFee, and discountAmount.
     * Called whenever any financial field is updated.
     */
    public void recalculateTotal() {
        this.totalAmount = subtotal
                .add(shippingFee)
                .subtract(discountAmount);
    }
}
