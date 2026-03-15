package com.shopee.ecommerce.module.payment.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopee.ecommerce.exception.PaymentGatewayException;
import com.shopee.ecommerce.module.payment.client.dto.PayPayCreatePaymentRequest;
import com.shopee.ecommerce.module.payment.config.PayPayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Low-level HTTP client for the PayPay REST API.
 *
 * Responsibilities:
 *  1. Build HMAC-signed Authorization headers per PayPay spec
 *  2. POST to /v2/codes          — create a QR/web-cashier payment
 *  3. GET  /v2/codes/{id}/payment — poll payment status
 *  4. POST to /v2/refunds        — issue a refund
 *  5. DELETE /v2/codes/{id}      — cancel a pending payment
 *
 * PayPay authentication uses a custom HMAC-SHA256 scheme:
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │  Authorization: hmac OPA {api_key}:{epoch}:{nonce}:{signature}      │
 * │                                                                      │
 * │  signature = Base64(                                                 │
 * │    HMAC-SHA256(                                                      │
 * │      apiSecret,                                                      │
 * │      "{epoch}\n{nonce}\n{method}\n{uri}\n{content-type}\n{body-hash}"│
 * │    )                                                                 │
 * │  )                                                                   │
 * └──────────────────────────────────────────────────────────────────────┘
 *
 * Reference: https://developer.paypay.ne.jp/products/docs/getstarted
 */
@Slf4j
@Component
public class PayPayHttpClient {

    private static final String HMAC_ALGORITHM   = "HmacSHA256";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT  = Duration.ofSeconds(15);

    private final PayPayProperties props;
    private final ObjectMapper     objectMapper;
    private final HttpClient       httpClient;

    public PayPayHttpClient(PayPayProperties props, ObjectMapper objectMapper) {
        this.props        = props;
        this.objectMapper = objectMapper;
        this.httpClient   = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CREATE PAYMENT  (POST /v2/codes)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Create a new QR/web-cashier payment on PayPay.
     *
     * @param request payment creation payload
     * @return parsed response data containing the redirect URL
     * @throws PaymentGatewayException if PayPay returns a non-success code
     */
    public PayPayCreateResponse createPayment(PayPayCreatePaymentRequest request) {
        String path = "/v2/codes";
        try {
            String body     = objectMapper.writeValueAsString(request);
            String url      = props.getApiBaseUrl() + path;
            String authHeader = buildAuthHeader("POST", path, body,
                    MediaType.APPLICATION_JSON_VALUE);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization",  authHeader)
                    .header("Content-Type",   MediaType.APPLICATION_JSON_VALUE)
                    .header("X-ASSUME-MERCHANT", props.getMerchantId())
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            log.info("PayPay createPayment: status={} merchantPaymentId={}",
                    response.statusCode(), request.getMerchantPaymentId());

            return parseCreateResponse(response);

        } catch (PaymentGatewayException e) {
            throw e;
        } catch (Exception e) {
            log.error("PayPay createPayment network error: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to connect to PayPay: " + e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  GET PAYMENT DETAILS  (GET /v2/codes/{merchantPaymentId}/payment)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Fetch the current state of a payment from PayPay.
     * Used to reconcile status when a webhook was missed.
     */
    public PayPayPaymentStatusResponse getPaymentDetails(String merchantPaymentId) {
        String path = "/v2/codes/" + merchantPaymentId + "/payment";
        try {
            String url        = props.getApiBaseUrl() + path;
            String authHeader = buildAuthHeader("GET", path, "", MediaType.APPLICATION_JSON_VALUE);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization",  authHeader)
                    .header("Content-Type",   MediaType.APPLICATION_JSON_VALUE)
                    .header("X-ASSUME-MERCHANT", props.getMerchantId())
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            log.debug("PayPay getPaymentDetails: status={} merchantPaymentId={}",
                    response.statusCode(), merchantPaymentId);

            return parseStatusResponse(response);

        } catch (PaymentGatewayException e) {
            throw e;
        } catch (Exception e) {
            log.error("PayPay getPaymentDetails error for {}: {}", merchantPaymentId, e.getMessage(), e);
            throw new PaymentGatewayException("Failed to fetch PayPay payment details", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CANCEL PAYMENT  (DELETE /v2/codes/{merchantPaymentId})
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Cancel a pending payment request that has not yet been completed.
     */
    public void cancelPayment(String merchantPaymentId) {
        String path = "/v2/codes/" + merchantPaymentId;
        try {
            String url        = props.getApiBaseUrl() + path;
            String authHeader = buildAuthHeader("DELETE", path, "", MediaType.APPLICATION_JSON_VALUE);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization",  authHeader)
                    .header("Content-Type",   MediaType.APPLICATION_JSON_VALUE)
                    .header("X-ASSUME-MERCHANT", props.getMerchantId())
                    .DELETE()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.warn("PayPay cancelPayment non-success: status={} body={}",
                        response.statusCode(), response.body());
            } else {
                log.info("PayPay payment cancelled: merchantPaymentId={}", merchantPaymentId);
            }

        } catch (Exception e) {
            // Log but don't throw — a failed cancel is not critical
            log.error("PayPay cancelPayment error for {}: {}", merchantPaymentId, e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  REFUND  (POST /v2/refunds)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Issue a refund for a completed payment.
     *
     * @param paymentId     PayPay's own payment ID (from PayPay, not ours)
     * @param amountYen     amount to refund in integer yen
     * @param reason        human-readable reason stored on the refund record
     */
    public PayPayRefundResponse refundPayment(String paymentId, long amountYen, String reason) {
        String path = "/v2/refunds";
        try {
            String merchantRefundId = "REF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

            Map<String, Object> refundBody = new LinkedHashMap<>();
            refundBody.put("merchantRefundId", merchantRefundId);
            refundBody.put("paymentId",        paymentId);
            refundBody.put("amount", Map.of("amount", amountYen, "currency", "JPY"));
            refundBody.put("requestedAt", Instant.now().getEpochSecond());
            refundBody.put("reason",      reason != null ? reason : "Customer refund");

            String body       = objectMapper.writeValueAsString(refundBody);
            String url        = props.getApiBaseUrl() + path;
            String authHeader = buildAuthHeader("POST", path, body, MediaType.APPLICATION_JSON_VALUE);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization",  authHeader)
                    .header("Content-Type",   MediaType.APPLICATION_JSON_VALUE)
                    .header("X-ASSUME-MERCHANT", props.getMerchantId())
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            log.info("PayPay refund: status={} paymentId={} amount=¥{}",
                    response.statusCode(), paymentId, amountYen);

            return parseRefundResponse(response, merchantRefundId);

        } catch (PaymentGatewayException e) {
            throw e;
        } catch (Exception e) {
            log.error("PayPay refund error for paymentId={}: {}", paymentId, e.getMessage(), e);
            throw new PaymentGatewayException("PayPay refund failed", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HMAC AUTH HEADER BUILDER
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Build the PayPay HMAC-SHA256 Authorization header.
     *
     * Algorithm:
     *  epoch     = current Unix epoch seconds
     *  nonce     = random 8-char alphanumeric
     *  bodyHash  = "noBody" when body is empty, else SHA-256 hex of body bytes
     *  canonical = epoch + "\n" + nonce + "\n" + METHOD + "\n" + PATH + "\n"
     *              + contentType + "\n" + bodyHash
     *  signature = Base64(HMAC-SHA256(apiSecret, canonical))
     *  header    = "hmac OPA " + apiKey + ":" + epoch + ":" + nonce + ":" + signature
     */
    String buildAuthHeader(String method, String path, String body, String contentType) {
        try {
            String epoch   = String.valueOf(Instant.now().getEpochSecond());
            String nonce   = randomNonce();
            String bodyHash = (body == null || body.isEmpty()) ? "noBody" : sha256Hex(body);

            String canonical = String.join("\n",
                    epoch, nonce, method.toUpperCase(), path, contentType, bodyHash);

            String signature = hmacBase64(props.getApiSecret(), canonical);

            return "hmac OPA " + props.getApiKey() + ":" + epoch + ":" + nonce + ":" + signature;

        } catch (Exception e) {
            throw new PaymentGatewayException("Failed to build PayPay auth header", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  RESPONSE PARSERS
    // ═══════════════════════════════════════════════════════════════════════

    private PayPayCreateResponse parseCreateResponse(HttpResponse<String> response) throws Exception {
        if (response.statusCode() >= 500) {
            throw new PaymentGatewayException(
                    "PayPay server error: HTTP " + response.statusCode());
        }
        Map<String, Object> body = objectMapper.readValue(
                response.body(), new TypeReference<>() {});

        Map<String, Object> resultInfo = castMap(body.get("resultInfo"));
        String code = (String) resultInfo.get("code");

        if (!"SUCCESS".equalsIgnoreCase(code)) {
            String msg = (String) resultInfo.getOrDefault("message", "Unknown PayPay error");
            throw new PaymentGatewayException("PayPay rejected payment creation: " + msg);
        }

        Map<String, Object> data = castMap(body.get("data"));
        return PayPayCreateResponse.builder()
                .merchantPaymentId((String) data.get("merchantPaymentId"))
                .paymentId((String) data.getOrDefault("paymentId", ""))
                .status((String) data.getOrDefault("status", "CREATED"))
                .url((String) data.get("url"))
                .deeplink((String) data.get("deeplink"))
                .expiryDate(toLong(data.get("expiryDate")))
                .build();
    }

    private PayPayPaymentStatusResponse parseStatusResponse(HttpResponse<String> response) throws Exception {
        if (response.statusCode() == 404) {
            throw new PaymentGatewayException("PayPay payment not found");
        }
        Map<String, Object> body = objectMapper.readValue(
                response.body(), new TypeReference<>() {});
        Map<String, Object> resultInfo = castMap(body.get("resultInfo"));
        String code = (String) resultInfo.get("code");

        if (!"SUCCESS".equalsIgnoreCase(code)) {
            String msg = (String) resultInfo.getOrDefault("message", "Unknown PayPay error");
            throw new PaymentGatewayException("PayPay status query failed: " + msg);
        }

        Map<String, Object> data = castMap(body.get("data"));
        return PayPayPaymentStatusResponse.builder()
                .merchantPaymentId((String) data.get("merchantPaymentId"))
                .paymentId((String) data.getOrDefault("paymentId", ""))
                .status((String) data.getOrDefault("status", "UNKNOWN"))
                .acceptedAt(toLong(data.get("acceptedAt")))
                .build();
    }

    private PayPayRefundResponse parseRefundResponse(HttpResponse<String> response,
                                                      String merchantRefundId) throws Exception {
        if (response.statusCode() >= 400) {
            throw new PaymentGatewayException(
                    "PayPay refund failed: HTTP " + response.statusCode() + " — " + response.body());
        }
        return PayPayRefundResponse.builder()
                .merchantRefundId(merchantRefundId)
                .success(response.statusCode() < 300)
                .build();
    }

    // ── Crypto helpers ────────────────────────────────────────────────────────

    private String sha256Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private String hmacBase64(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        return Base64.getEncoder().encodeToString(
                mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private String randomNonce() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object o) {
        if (o instanceof Map<?, ?> m) return (Map<String, Object>) m;
        throw new PaymentGatewayException("Unexpected PayPay response structure");
    }

    private long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return 0L; }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  RESPONSE VALUE OBJECTS  (package-private, no need to export)
    // ═══════════════════════════════════════════════════════════════════════

    @lombok.Value @lombok.Builder
    public static class PayPayCreateResponse {
        String merchantPaymentId;
        String paymentId;
        String status;
        String url;
        String deeplink;
        long   expiryDate;
    }

    @lombok.Value @lombok.Builder
    public static class PayPayPaymentStatusResponse {
        String merchantPaymentId;
        String paymentId;
        String status;
        long   acceptedAt;
    }

    @lombok.Value @lombok.Builder
    public static class PayPayRefundResponse {
        String  merchantRefundId;
        boolean success;
    }
}
