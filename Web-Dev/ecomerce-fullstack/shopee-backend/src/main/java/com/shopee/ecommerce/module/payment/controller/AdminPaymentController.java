package com.shopee.ecommerce.module.payment.controller;

import com.shopee.ecommerce.module.payment.dto.response.PaymentStatusResponse;
import com.shopee.ecommerce.module.payment.dto.response.RefundResponse;
import com.shopee.ecommerce.module.payment.service.PaymentService;
import com.shopee.ecommerce.security.UserPrincipal;
import com.shopee.ecommerce.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin payment management endpoints.
 *
 * Base path: /api/admin/payments
 * Requires ROLE_ADMIN.
 *
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  POST /api/admin/payments/{orderId}/refund      — full refund       │
 * │  POST /api/admin/payments/{orderId}/reconcile   — force sync        │
 * └─────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Tag(name = "Admin: Payments", description = "Admin payment management — refunds and reconciliation")
public class AdminPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{orderId}/refund")
    @Operation(
        summary     = "Issue a full refund",
        description = "Calls the PayPay refund API to refund the full captured amount. " +
                      "Transitions the order to REFUNDED status."
    )
    public ResponseEntity<ApiResponse<RefundResponse>> refund(
            @PathVariable UUID orderId,
            @RequestParam @NotBlank @Size(max = 500) String reason,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        RefundResponse response = paymentService.refundPayment(orderId, reason, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Refund issued successfully", response));
    }

    @PostMapping("/{orderId}/reconcile")
    @Operation(
        summary     = "Force-reconcile payment with PayPay",
        description = "Queries PayPay directly and updates our DB if the status diverged. " +
                      "Use when a webhook was missed and the payment appears stuck."
    )
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> reconcile(
            @PathVariable UUID orderId,
            @RequestParam String merchantPaymentId
    ) {
        PaymentStatusResponse status = paymentService.reconcilePayment(merchantPaymentId);
        return ResponseEntity.ok(ApiResponse.success("Payment reconciled", status));
    }
}
