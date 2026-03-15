package com.shopee.ecommerce.module.payment.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

// ═══════════════════════════════════════════════════════════════════════════
//  OUTBOUND — what we POST to PayPay
// ═══════════════════════════════════════════════════════════════════════════

// ═══════════════════════════════════════════════════════════════════════════
//  INBOUND — what PayPay returns from the create-payment call
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Wrapper around every PayPay API response.
 * Schema: { "resultInfo": {...}, "data": {...} }
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
class PayPayApiResponse<T> {

    @JsonProperty("resultInfo")
    private ResultInfo resultInfo;

    @JsonProperty("data")
    private T data;

    @Data @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResultInfo {
        @JsonProperty("code")    private String code;
        @JsonProperty("message") private String message;
        @JsonProperty("codeId")  private String codeId;

        public boolean isSuccess() {
            return "SUCCESS".equalsIgnoreCase(code);
        }
    }
}

/**
 * The {@code data} object inside a successful create-payment response.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
class PayPayCreatePaymentData {

    @JsonProperty("merchantPaymentId") private String merchantPaymentId;
    @JsonProperty("paymentId")         private String paymentId;
    @JsonProperty("status")            private String status;

    /** URL to redirect the user to — either QR page or deep-link. */
    @JsonProperty("url")               private String url;

    @JsonProperty("deeplink")          private String deeplink;
    @JsonProperty("expiryDate")        private Long   expiryDate;   // unix epoch seconds
}

/**
 * The {@code data} object returned when querying payment status (GET /v2/codes/{id}/payment).
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
class PayPayPaymentDetails {

    @JsonProperty("merchantPaymentId") private String merchantPaymentId;
    @JsonProperty("paymentId")         private String paymentId;
    @JsonProperty("status")            private String status;      // COMPLETED, FAILED, …

    @JsonProperty("acceptedAt")        private Long   acceptedAt;
    @JsonProperty("requestedAt")       private Long   requestedAt;

    @JsonProperty("amount")            private PayPayCreatePaymentRequest.PayPayAmount amount;

    @JsonProperty("paymentMethods")    private java.util.List<PaymentMethodDetail> paymentMethods;

    @Data @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentMethodDetail {
        @JsonProperty("type")   private String type;
        @JsonProperty("amount") private PayPayCreatePaymentRequest.PayPayAmount amount;
    }
}

/**
 * Refund request payload — POST /v2/refunds.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class PayPayRefundRequest {
    @JsonProperty("merchantRefundId")  private String merchantRefundId;
    @JsonProperty("paymentId")         private String paymentId;
    @JsonProperty("amount")            private PayPayCreatePaymentRequest.PayPayAmount amount;
    @JsonProperty("requestedAt")       private long   requestedAt;
    @JsonProperty("reason")            private String reason;
}
