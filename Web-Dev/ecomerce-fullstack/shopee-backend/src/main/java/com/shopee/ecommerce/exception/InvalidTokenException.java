package com.shopee.ecommerce.exception;


// ─────────────────────────────────────────────────────────────────────────────
//  InvalidTokenException — 401
// ─────────────────────────────────────────────────────────────────────────────

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}