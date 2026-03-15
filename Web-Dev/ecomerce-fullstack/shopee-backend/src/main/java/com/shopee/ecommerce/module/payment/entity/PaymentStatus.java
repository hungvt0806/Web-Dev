package com.shopee.ecommerce.module.payment.entity;

/**
 * Payment lifecycle states.
 *
 * State transitions:
 *
 *  INITIATED ──► AWAITING_CAPTURE ──► COMPLETED ──► REFUNDED
 *      │                │
 *      └──► FAILED      └──► FAILED
 *           CANCELLED        EXPIRED
 *           EXPIRED
 *
 * INITIATED        — Payment record created, PayPay redirect URL generated.
 * AWAITING_CAPTURE — User approved in PayPay app; authorization received.
 *                    Capture call needed to move money.
 * COMPLETED        — Payment fully captured. Order can be fulfilled.
 * FAILED           — PayPay declined or error occurred.
 * CANCELLED        — Buyer cancelled on the PayPay payment page.
 * EXPIRED          — Payment request TTL elapsed before buyer completed.
 * REFUNDED         — Full or partial refund processed.
 */
public enum PaymentStatus {

    INITIATED(false),
    AWAITING_CAPTURE(false),
    COMPLETED(true),
    FAILED(true),
    CANCELLED(true),
    EXPIRED(true),
    REFUNDED(true);

    private final boolean terminal;

    PaymentStatus(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean isTerminal() { return terminal; }

    /** PayPay's own state strings mapped to our enum. */
    public static PaymentStatus fromPayPayState(String paypayState) {
        return switch (paypayState == null ? "" : paypayState.toUpperCase()) {
            case "CREATED",
                 "AUTHORIZED"    -> AWAITING_CAPTURE;
            case "COMPLETED"     -> COMPLETED;
            case "FAILED"        -> FAILED;
            case "CANCELED"      -> CANCELLED;
            case "EXPIRED"       -> EXPIRED;
            case "REFUNDED"      -> REFUNDED;
            default              -> INITIATED;
        };
    }
}
