package com.shopee.ecommerce.exception;

// ─────────────────────────────────────────────────────────────────────────────
//  ResourceAlreadyExistsException — 409 Conflict
// ─────────────────────────────────────────────────────────────────────────────
public class ResourceAlreadyExistsException extends RuntimeException {
    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
}