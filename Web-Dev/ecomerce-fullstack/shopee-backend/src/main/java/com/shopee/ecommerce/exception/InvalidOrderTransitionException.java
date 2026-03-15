package com.shopee.ecommerce.exception;

/**
 * Thrown when an order status transition is not allowed
 * by the {@code OrderStateMachine}.
 *
 * Example:
 *   Order #ORD-001 cannot transition from DELIVERED to PENDING.
 *   Allowed next states: []
 */
public class InvalidOrderTransitionException extends RuntimeException {

    public InvalidOrderTransitionException(String message) {
        super(message);
    }

    public InvalidOrderTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}