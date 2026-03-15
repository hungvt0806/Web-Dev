package com.shopee.ecommerce.module.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shopee.ecommerce.module.payment.entity.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// ─── InitiatePaymentResponse ──────────────────────────────────────────────────
// Returned when a payment is first created — contains the redirect URL.
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitiatePaymentResponse {
    private UUID paymentId;
    private String merchantPaymentId;
    private UUID orderId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;

    /**
     * URL to open in the user's browser / redirect to.
     * For mobile: use {@code deeplink} to open the PayPay app directly.
     */
    private String paymentUrl;

    /**
     * PayPay app deep-link — opens the PayPay app directly on mobile.
     */
    private String deeplink;

    /**
     * When this payment request expires (show a countdown timer).
     */
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
