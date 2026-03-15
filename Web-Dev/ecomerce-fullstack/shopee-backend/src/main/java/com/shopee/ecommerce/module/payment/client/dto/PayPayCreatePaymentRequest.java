package com.shopee.ecommerce.module.payment.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Payload for POST /v2/codes (QR-code / web-cashier payment creation).
 * <p>
 * PayPay API reference:
 * https://developer.paypay.ne.jp/products/docs/webpayment
 * <p>
 * Field naming follows PayPay's camelCase convention.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PayPayCreatePaymentRequest {

    /**
     * Our unique payment reference — used as idempotency key.
     */
    @JsonProperty("merchantPaymentId")
    private String merchantPaymentId;

    /**
     * Amount object — PayPay requires amount + currency as a nested object.
     */
    @JsonProperty("amount")
    private PayPayAmount amount;

    /**
     * Unix epoch seconds — when this payment request expires.
     */
    @JsonProperty("requestedAt")
    private long requestedAt;

    /**
     * How long the QR/deep-link is valid, in seconds.
     */
    @JsonProperty("expiryAt")
    private long expiryAt;

    /**
     * URL our server receives the result webhook at.
     */
    @JsonProperty("redirectUrl")
    private String redirectUrl;

    /**
     * HTTP method PayPay calls our redirectUrl with ("GET").
     */
    @JsonProperty("redirectType")
    @Builder.Default
    private String redirectType = "WEB_LINK";

    /**
     * Human-readable order description shown on the PayPay payment screen.
     */
    @JsonProperty("orderDescription")
    private String orderDescription;

    /**
     * Line items — shown in the PayPay payment breakdown.
     */
    @JsonProperty("orderItems")
    private List<PayPayOrderItem> orderItems;

    /**
     * Metadata we echo back — useful for audit but not required.
     */
    @JsonProperty("storeInfo")
    private String storeInfo;

    // ── Nested ────────────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PayPayAmount {
        @JsonProperty("amount")
        private long amount;   // integer yen (no decimals for JPY)
        @JsonProperty("currency")
        private String currency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PayPayOrderItem {
        @JsonProperty("name")
        private String name;
        @JsonProperty("quantity")
        private int quantity;
        @JsonProperty("unitPrice")
        private PayPayAmount unitPrice;
    }
}
