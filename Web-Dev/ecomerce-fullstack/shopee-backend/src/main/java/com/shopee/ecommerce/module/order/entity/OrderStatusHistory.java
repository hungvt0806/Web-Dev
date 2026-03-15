package com.shopee.ecommerce.module.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Append-only record of every order status transition.
 *
 * One row is inserted per transition — rows are NEVER updated or deleted.
 * This gives buyers a full timeline ("Your order was shipped at 14:32 on Jan 15")
 * and gives support staff a complete audit trail for dispute resolution.
 *
 * actor_id / actor_type records WHO triggered the transition:
 *   SYSTEM  — automated (e.g. payment webhook confirmed payment)
 *   BUYER   — buyer cancelled the order
 *   SELLER  — seller marked as shipped
 *   ADMIN   — admin overrode the status
 */
@Entity
@Table(
    name = "order_status_history",
    indexes = @Index(name = "idx_osh_order_id", columnList = "order_id")
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private OrderStatus fromStatus;   // null for the initial PENDING entry

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private OrderStatus toStatus;

    /** Human-readable note shown in the order timeline. */
    @Column(name = "note", length = 500)
    private String note;

    /** UUID of the user who triggered this transition. Null for SYSTEM transitions. */
    @Column(name = "actor_id")
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 10)
    @Builder.Default
    private ActorType actorType = ActorType.SYSTEM;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum ActorType { SYSTEM, BUYER, SELLER, ADMIN }
}
