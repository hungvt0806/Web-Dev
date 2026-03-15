package com.shopee.ecommerce.module.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopee.ecommerce.module.payment.client.dto.PayPayWebhookEvent;
import com.shopee.ecommerce.module.payment.service.PaymentService;
import com.shopee.ecommerce.module.payment.webhook.PayPaySignatureVerifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * Receives and processes PayPay webhook events.
 *
 * Base path: /api/payments/webhook
 *
 * Security model:
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │  This endpoint is PUBLIC (no JWT required) — PayPay cannot send a   │
 * │  JWT token. Instead, every request is authenticated by verifying    │
 * │  the HMAC-SHA256 signature in the x-paypay-signature header.        │
 * │                                                                      │
 * │  This endpoint must be excluded from Spring Security's JWT filter   │
 * │  and CSRF protection in SecurityConfig.                             │
 * └──────────────────────────────────────────────────────────────────────┘
 *
 * Idempotency:
 *   PayPay retries webhook delivery for ~24 hours until they receive HTTP 200.
 *   We compute SHA-256 of the raw body and reject duplicates in the service layer.
 *   This controller ALWAYS returns HTTP 200 for verified requests (even duplicates)
 *   so PayPay stops retrying.
 *
 * Error handling:
 *   - Signature invalid  → 401 (PayPay will retry — misconfiguration alert)
 *   - Body unparseable   → 400
 *   - Processing error   → 200 (we log internally; retrying won't help for transient issues)
 */
@Slf4j
@RestController
@RequestMapping("/api/payments/webhook")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "PayPay payment webhook receiver")
public class PaymentWebhookController {

    private final PayPaySignatureVerifier signatureVerifier;
    private final PaymentService          paymentService;
    private final ObjectMapper            objectMapper;

    /**
     * POST /api/payments/webhook/paypay
     *
     * PayPay sends events here with these headers:
     *   x-paypay-request-id  — unique delivery ID
     *   x-paypay-timestamp   — Unix epoch seconds
     *   x-paypay-signature   — HMAC-SHA256 signature
     */
    @PostMapping("/paypay")
    @Operation(
        summary     = "PayPay webhook receiver",
        description = "Receives payment status notifications from PayPay. " +
                      "All requests are HMAC-SHA256 verified before processing."
    )
    public ResponseEntity<Map<String, String>> handlePayPayWebhook(
            @RequestBody byte[] rawBody,
            @RequestHeader(value = "x-paypay-request-id",  required = false) String requestId,
            @RequestHeader(value = "x-paypay-timestamp",   required = false) String timestamp,
            @RequestHeader(value = "x-paypay-signature",   required = false) String signature,
            @RequestHeader(value = "Content-Type",         defaultValue = "application/json") String contentType,
            jakarta.servlet.http.HttpServletRequest servletRequest
    ) {
        String requestPath = servletRequest.getRequestURI();

        log.debug("PayPay webhook received: requestId={} timestamp={} path={}",
                requestId, timestamp, requestPath);

        // ── 1. Validate required security headers ─────────────────────────────
        if (isBlank(timestamp) || isBlank(signature)) {
            log.warn("PayPay webhook missing security headers: requestId={}", requestId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Missing required PayPay security headers"));
        }

        // ── 2. Verify HMAC-SHA256 signature ───────────────────────────────────
        boolean signatureValid = signatureVerifier.verify(
                rawBody, timestamp, requestId, signature,
                "POST", requestPath, contentType);

        if (!signatureValid) {
            log.warn("PayPay webhook REJECTED — invalid signature: requestId={}", requestId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Signature verification failed"));
        }

        // ── 3. Parse body ──────────────────────────────────────────────────────
        PayPayWebhookEvent event;
        try {
            event = objectMapper.readValue(rawBody, PayPayWebhookEvent.class);
        } catch (Exception e) {
            log.error("PayPay webhook body parse error: requestId={} error={}", requestId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Could not parse webhook body"));
        }

        log.info("PayPay webhook verified: requestId={} type={} merchantPaymentId={} status={}",
                requestId,
                event.getNotificationType(),
                event.getMerchantPaymentId(),
                event.getPayPayStatus());

        // ── 4. Compute idempotency hash ────────────────────────────────────────
        String payloadHash = sha256Hex(rawBody);

        // ── 5. Delegate to service (signature is guaranteed valid here) ────────
        try {
            if (event.isPaymentEvent()) {
                paymentService.handleWebhook(event, payloadHash);
            } else if (event.isRefundEvent()) {
                log.info("PayPay refund webhook received for merchantPaymentId={} — handled by refund flow",
                        event.getMerchantPaymentId());
                // Refund webhooks are informational — the refund was already initiated by us.
                // Just acknowledge.
            } else {
                log.info("Unhandled PayPay webhook type={} requestId={}", event.getNotificationType(), requestId);
            }
        } catch (Exception e) {
            // IMPORTANT: return 200 so PayPay doesn't retry.
            // Our own monitoring/alerting will catch the error.
            log.error("Error processing PayPay webhook requestId={} merchantPaymentId={}: {}",
                    requestId, event.getMerchantPaymentId(), e.getMessage(), e);
        }

        // ── 6. Always ACK with 200 so PayPay stops retrying ───────────────────
        return ResponseEntity.ok(Map.of("result", "SUCCESS"));
    }

    /**
     * GET /api/payments/webhook/paypay/health
     * Simple liveness endpoint — lets you verify the webhook URL is reachable
     * before registering it in the PayPay Merchant Dashboard.
     */
    @GetMapping("/paypay/health")
    @Operation(summary = "Webhook endpoint health check")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "endpoint", "/api/payments/webhook/paypay"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
