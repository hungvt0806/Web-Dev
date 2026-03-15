package com.shopee.ecommerce.module.payment.webhook;

import com.shopee.ecommerce.module.payment.config.PayPayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * Verifies PayPay webhook request signatures.
 *
 * PayPay uses HMAC-SHA256 to sign each webhook delivery.
 *
 * Canonical string format (all parts joined with newlines):
 * ┌────────────────────────────────────────────────────────────────┐
 * │  {x-paypay-timestamp}                                          │
 * │  {HTTP_METHOD}          (always "POST" for webhooks)           │
 * │  {URI_PATH}             (e.g. /api/payments/webhook/paypay)    │
 * │  {Content-Type}         (e.g. application/json)                │
 * │  {SHA-256 of raw body}  (lowercase hex or Base64-URL)          │
 * └────────────────────────────────────────────────────────────────┘
 *
 * Then: signature = Base64( HMAC-SHA256( webhookSecret, canonicalString ) )
 *
 * Compare with the value in the {@code x-paypay-signature} request header.
 *
 * Reference: https://developer.paypay.ne.jp/products/docs/webhookdeveloperguide
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayPaySignatureVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    /** Maximum age of a webhook request before we reject it as a replay. */
    private static final long MAX_TIMESTAMP_SKEW_SECONDS = 300; // 5 minutes

    private final PayPayProperties props;

    /**
     * Verify the HMAC signature of an inbound PayPay webhook.
     *
     * @param requestBody  raw HTTP request body bytes (do NOT decode before passing)
     * @param timestamp    value of the {@code x-paypay-timestamp} header
     * @param requestId    value of the {@code x-paypay-request-id} header (for logging)
     * @param signature    value of the {@code x-paypay-signature} header
     * @param httpMethod   HTTP method (should always be "POST")
     * @param uriPath      request URI path, e.g. "/api/payments/webhook/paypay"
     * @param contentType  Content-Type header value
     * @return true if the signature is valid and the timestamp is within the skew window
     */
    public boolean verify(byte[] requestBody,
                          String timestamp,
                          String requestId,
                          String signature,
                          String httpMethod,
                          String uriPath,
                          String contentType) {
        try {
            // 1. Reject replays: timestamp must be recent
            if (!isTimestampFresh(timestamp)) {
                log.warn("PayPay webhook rejected: stale timestamp={} requestId={}", timestamp, requestId);
                return false;
            }

            // 2. Build canonical string
            String bodyHash       = sha256Hex(requestBody);
            String canonical      = buildCanonicalString(timestamp, httpMethod, uriPath, contentType, bodyHash);

            // 3. Compute expected HMAC
            String expected = hmacSha256Base64(props.getWebhookSecret(), canonical);

            // 4. Constant-time comparison to prevent timing attacks
            boolean valid = MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));

            if (!valid) {
                log.warn("PayPay webhook signature mismatch for requestId={}", requestId);
            }
            return valid;

        } catch (Exception e) {
            log.error("PayPay signature verification error for requestId={}: {}", requestId, e.getMessage());
            return false;
        }
    }

    /**
     * Compute SHA-256 over the raw body bytes and return lowercase hex.
     * This is the body hash component in the canonical string.
     */
    public String computeBodyHash(byte[] body) {
        return sha256Hex(body);
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private String buildCanonicalString(String timestamp, String method,
                                         String path, String contentType,
                                         String bodyHash) {
        return String.join("\n", timestamp, method.toUpperCase(), path, contentType, bodyHash);
    }

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

    private String hmacSha256Base64(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(raw);
    }

    private boolean isTimestampFresh(String timestampStr) {
        try {
            long ts    = Long.parseLong(timestampStr);
            long now   = Instant.now().getEpochSecond();
            long delta = Math.abs(now - ts);
            return delta <= MAX_TIMESTAMP_SKEW_SECONDS;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
