package com.shopee.ecommerce.exception;

/**
 * Thrown when attempting to checkout or process an empty cart.
 */
public class EmptyCartException extends RuntimeException {

    public EmptyCartException(String message) {
        super(message);
    }
}