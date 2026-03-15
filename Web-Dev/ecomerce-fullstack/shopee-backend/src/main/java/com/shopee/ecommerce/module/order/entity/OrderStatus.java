package com.shopee.ecommerce.module.order.entity;

import java.util.Set;

/**
 * Order lifecycle state machine.
 *
 * Allowed transitions:
 *
 *   PENDING ──────────────────────────────────────────────── CANCELLED
 *      │
 *   AWAITING_PAYMENT ─────────────────────────────────────── CANCELLED
 *      │
 *     PAID ─────────────────────────────────────────────────────────────────┐
 *      │                                                                     │
 *   PROCESSING ──────────────────────────────────────────── CANCELLED       │
 *      │                                                                     │
 *   SHIPPED ─────────────────────────────────────────────────────────────── REFUNDED
 *      │
 *   DELIVERED ───────────────────────────────────────────────────────────── REFUNDED
 *
 * Rules enforced by {@link com.shopee.ecommerce.module.order.statemachine.OrderStateMachine}:
 *  - Only PENDING and AWAITING_PAYMENT can be cancelled by the buyer.
 *  - PAID / PROCESSING / SHIPPED can be cancelled only by the seller or admin.
 *  - DELIVERED → REFUNDED requires a refund request workflow (future scope).
 *  - Terminal states (DELIVERED, CANCELLED, REFUNDED) have no outgoing transitions.
 */
public enum OrderStatus {

    /** Order created but payment not yet initiated. */
    PENDING(false) {
        @Override public Set<OrderStatus> allowedNext() {
            return Set.of(AWAITING_PAYMENT, CANCELLED);
        }
    },

    /** Payment initiated (e.g. PayPay QR code generated) but not confirmed. */
    AWAITING_PAYMENT(false) {
        @Override public Set<OrderStatus> allowedNext() {
            return Set.of(PAID, CANCELLED);
        }
    },

    /** Payment confirmed by the payment gateway webhook. */
    PAID(false) {
        @Override public Set<OrderStatus> allowedNext() {
            return Set.of(PROCESSING, CANCELLED, REFUNDED);
        }
    },

    /** Seller is preparing the shipment. */
    PROCESSING(false) {
        @Override public Set<OrderStatus> allowedNext() {
            return Set.of(SHIPPED, CANCELLED, REFUNDED);
        }
    },

    /** Carrier has picked up the parcel. */
    SHIPPED(false) {
        @Override public Set<OrderStatus> allowedNext() {
            return Set.of(DELIVERED, REFUNDED);
        }
    },

    /** Parcel delivered to the buyer. */
    DELIVERED(true) {
        @Override public Set<OrderStatus> allowedNext() {
            return Set.of(REFUNDED);
        }
    },

    /** Order cancelled — no fulfillment will occur. */
    CANCELLED(true) {
        @Override public Set<OrderStatus> allowedNext() {
            return Set.of();
        }
    },

    /** Order refunded (partial or full). */
    REFUNDED(true) {
        @Override public Set<OrderStatus> allowedNext() {
            return Set.of();
        }
    };

    // ── Metadata ──────────────────────────────────────────────────────────────

    /** True for terminal states — no further transitions allowed. */
    private final boolean terminal;

    OrderStatus(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean isTerminal() { return terminal; }

    /** Returns the set of valid next states from this state. */
    public abstract Set<OrderStatus> allowedNext();

    /** Returns true if transitioning to {@code next} is a valid move. */
    public boolean canTransitionTo(OrderStatus next) {
        return allowedNext().contains(next);
    }

    /** True if the buyer can cancel (without seller/admin involvement). */
    public boolean isBuyerCancellable() {
        return this == PENDING || this == AWAITING_PAYMENT;
    }

    /** True if a payment is still outstanding. */
    public boolean requiresPayment() {
        return this == PENDING || this == AWAITING_PAYMENT;
    }

    /** True if stock was already reserved for this order. */
    public boolean hasReservedStock() {
        return this != CANCELLED && this != PENDING;
    }

    /** Display label for the frontend status badge. */
    public String displayLabel() {
        return switch (this) {
            case PENDING           -> "Order Placed";
            case AWAITING_PAYMENT  -> "Awaiting Payment";
            case PAID              -> "Payment Confirmed";
            case PROCESSING        -> "Preparing Shipment";
            case SHIPPED           -> "On the Way";
            case DELIVERED         -> "Delivered";
            case CANCELLED         -> "Cancelled";
            case REFUNDED          -> "Refunded";
        };
    }
}
