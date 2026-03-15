package com.shopee.ecommerce.module.order.statemachine;

import com.shopee.ecommerce.exception.InvalidOrderTransitionException;
import com.shopee.ecommerce.module.order.entity.Order;
import com.shopee.ecommerce.module.order.entity.OrderStatus;
import com.shopee.ecommerce.module.order.entity.OrderStatusHistory;
import com.shopee.ecommerce.module.order.entity.OrderStatusHistory.ActorType;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Order state machine — the single source of truth for status transitions.
 *
 * Every status change in the system MUST go through {@link #transition}.
 * No code should set {@code order.setStatus(...)} directly.
 *
 * Responsibilities:
 *  1. Validate that the requested transition is allowed
 *  2. Update the order status
 *  3. Stamp the relevant timestamp field (paidAt, shippedAt, …)
 *  4. Append an immutable row to the status history log
 */
@Slf4j
@UtilityClass
public class OrderStateMachine {

    /**
     * Perform a status transition on an Order.
     *
     * @param order      the order to transition
     * @param to         the target status
     * @param actorId    UUID of the user who triggered the change (null for SYSTEM)
     * @param actorType  SYSTEM | BUYER | SELLER | ADMIN
     * @param note       optional human-readable note appended to the history entry
     * @throws InvalidOrderTransitionException if the transition is not allowed
     */
    public static void transition(Order order, OrderStatus to,
                                  UUID actorId, ActorType actorType,
                                  String note) {
        OrderStatus from = order.getStatus();

        if (from == to) {
            log.debug("Order {}: no-op transition {} → {} (skipped)", order.getOrderNumber(), from, to);
            return;
        }

        if (!from.canTransitionTo(to)) {
            throw new InvalidOrderTransitionException(
                    "Order " + order.getOrderNumber() +
                    " cannot transition from " + from + " to " + to +
                    ". Allowed next states: " + from.allowedNext());
        }

        // Apply the transition
        order.setStatus(to);
        stampTimestamp(order, to);

        // Append audit entry
        OrderStatusHistory entry = OrderStatusHistory.builder()
                .fromStatus(from)
                .toStatus(to)
                .actorId(actorId)
                .actorType(actorType)
                .note(note)
                .build();
        order.addStatusHistory(entry);

        log.info("Order {}: {} → {} by {} ({})",
                order.getOrderNumber(), from, to, actorType, actorId);
    }

    /**
     * Convenience overload for SYSTEM-triggered transitions (webhooks, schedulers).
     */
    public static void systemTransition(Order order, OrderStatus to, String note) {
        transition(order, to, null, ActorType.SYSTEM, note);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Stamp the appropriate timestamp field when entering a state. */
    private static void stampTimestamp(Order order, OrderStatus status) {
        LocalDateTime now = LocalDateTime.now();
        switch (status) {
            case PAID       -> order.setPaidAt(now);
            case SHIPPED    -> order.setShippedAt(now);
            case DELIVERED  -> order.setDeliveredAt(now);
            case CANCELLED  -> order.setCancelledAt(now);
            default         -> { /* no dedicated timestamp for other states */ }
        }
    }
}
