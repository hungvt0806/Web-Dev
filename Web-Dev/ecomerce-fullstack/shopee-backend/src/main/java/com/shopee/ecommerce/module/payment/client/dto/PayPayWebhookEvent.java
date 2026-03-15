package com.shopee.ecommerce.module.payment.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PayPay webhook event payload.
 *
 * PayPay delivers webhooks as POST requests to the URL registered in the
 * Merchant Dashboard. Each event carries:
 *
 *   {
 *     "notification_type": "Payment",
 *     "notification_date": 1640000000000,
 *     "id": "unique-event-id",
 *     "data": { ... payment details ... }
 *   }
 *
 * The HTTP request also carries three PayPay-specific headers used for
 * HMAC-SHA256 signature verification:
 *   x-paypay-request-id   — random UUID per delivery
 *   x-paypay-timestamp    — Unix epoch seconds
 *   x-paypay-signature    — Base64(HMAC-SHA256(secret, canonical_string))
 *
 * Signature canonical string:
 *   {x-paypay-timestamp}\n{HTTP_METHOD}\n{URI_PATH}\n{content-type}\n{SHA256_BODY}
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayPayWebhookEvent {

    /** "Payment" | "Refund" | "Cancellation" */
    @JsonProperty("notification_type")
    private String notificationType;

    /** Unix epoch milliseconds of the event. */
    @JsonProperty("notification_date")
    private Long notificationDate;

    /** PayPay's unique event ID — used for deduplication. */
    @JsonProperty("id")
    private String id;

    @JsonProperty("data")
    private PayPayWebhookData data;

    // ── Nested ────────────────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayPayWebhookData {

        @JsonProperty("merchantPaymentId")
        private String merchantPaymentId;

        @JsonProperty("paymentId")
        private String paymentId;

        /** CREATED | AUTHORIZED | COMPLETED | FAILED | CANCELED | EXPIRED | REFUNDED */
        @JsonProperty("status")
        private String status;

        @JsonProperty("requestedAt")
        private Long requestedAt;

        @JsonProperty("acceptedAt")
        private Long acceptedAt;

        @JsonProperty("amount")
        private AmountDto amount;

        @JsonProperty("paymentMethods")
        private java.util.List<PaymentMethodDto> paymentMethods;

        @Data @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
        public static class AmountDto {
            @JsonProperty("amount")   private long   amount;
            @JsonProperty("currency") private String currency;
        }

        @Data @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
        public static class PaymentMethodDto {
            @JsonProperty("type")   private String type;
            @JsonProperty("amount") private AmountDto amount;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean isPaymentEvent()      { return "Payment".equalsIgnoreCase(notificationType); }
    public boolean isRefundEvent()       { return "Refund".equalsIgnoreCase(notificationType); }
    public boolean isCancellationEvent() { return "Cancellation".equalsIgnoreCase(notificationType); }

    public String getMerchantPaymentId() {
        return data != null ? data.getMerchantPaymentId() : null;
    }

    public String getPayPayStatus() {
        return data != null ? data.getStatus() : null;
    }
}
