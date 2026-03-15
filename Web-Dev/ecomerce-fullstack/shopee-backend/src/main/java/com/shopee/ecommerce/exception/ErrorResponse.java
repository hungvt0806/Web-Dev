package com.shopee.ecommerce.exception;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
// ─────────────────────────────────────────────────────────────────────────────
//  ErrorResponse — the JSON body for all error responses
// ─────────────────────────────────────────────────────────────────────────────

public class ErrorResponse {
    private int                 status;
    private String              error;
    private String              message;
    private String              path;
    private LocalDateTime       timestamp;
    private Map<String, String> errors;
}