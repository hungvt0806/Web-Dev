package com.shopee.ecommerce.exception;


// ─────────────────────────────────────────────────────────────────────────────
//  BusinessException — 422 Unprocessable Entity
// ─────────────────────────────────────────────────────────────────────────────

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}