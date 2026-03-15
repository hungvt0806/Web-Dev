package com.shopee.ecommerce.module.payment.controller;

import com.shopee.ecommerce.module.payment.dto.request.InitiatePaymentRequest;
import com.shopee.ecommerce.module.payment.dto.response.InitiatePaymentResponse;
import com.shopee.ecommerce.module.payment.dto.response.PaymentStatusResponse;
import com.shopee.ecommerce.module.payment.service.PaymentService;
import com.shopee.ecommerce.security.UserPrincipal;
import com.shopee.ecommerce.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Buyer-facing payment controller.
 *
 * Base path: /api/payments
 * All endpoints require JWT authentication.
 *
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │  POST /api/payments/initiate         — start PayPay payment          │
 * │  GET  /api/payments/status/{orderId} — poll payment status           │
 * └──────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/payments")
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "PayPay payment initiation and status")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/payments/initiate
     *
     * Creates a PayPay payment request for the specified order.
     * Returns a {@code paymentUrl} and {@code deeplink} to redirect the buyer.
     *
     * Frontend flow:
     *  1. Buyer clicks "Pay with PayPay"
     *  2. Frontend calls POST /api/payments/initiate with { orderId }
     *  3. On desktop: redirect to paymentUrl (PayPay web cashier)
     *     On mobile:  open deeplink (PayPay app)
     *  4. After PayPay returns the buyer, poll GET /api/payments/status/{orderId}
     *     to confirm payment completed
     */
    @PostMapping("/initiate")
    @Operation(
        summary     = "Initiate a PayPay payment",
        description = "Creates a PayPay payment request for the given order. " +
                      "Returns a paymentUrl (for web) and deeplink (for mobile) " +
                      "to redirect the buyer to PayPay."
    )
    public ResponseEntity<ApiResponse<InitiatePaymentResponse>> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        InitiatePaymentResponse response = paymentService.initiatePayment(
                request.getOrderId(), principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Payment initiated", response));
    }

    /**
     * GET /api/payments/status/{orderId}
     *
     * Returns the current payment status for an order.
     * On PENDING status, this also queries PayPay live to catch missed webhooks.
     * The frontend should poll this every 3–5 seconds after the buyer returns
     * from the PayPay payment page.
     */
    @GetMapping("/status/{orderId}")
    @Operation(
        summary     = "Get payment status",
        description = "Returns the current payment status. " +
                      "If still pending, queries PayPay live for the latest state. " +
                      "Poll this after the buyer returns from PayPay."
    )
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatus(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PaymentStatusResponse status = paymentService.getPaymentStatus(orderId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
