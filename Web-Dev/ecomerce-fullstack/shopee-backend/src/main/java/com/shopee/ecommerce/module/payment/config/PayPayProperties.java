package com.shopee.ecommerce.module.payment.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * PayPay SDK / API configuration.
 *
 * Bound from application.yml under the {@code paypay} prefix.
 *
 * application.yml example:
 * <pre>
 * paypay:
 *   api-key:          YOUR_API_KEY
 *   api-secret:       YOUR_API_SECRET
 *   merchant-id:      YOUR_MERCHANT_ID
 *   production-mode:  false          # false = sandbox
 *   payment-ttl-minutes: 30
 *   webhook-secret:   WEBHOOK_SIGNING_SECRET
 *   redirect-url:     https://yourapp.com/payment/result
 *   webhook-url:      https://yourapp.com/api/payments/webhook/paypay
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "paypay")
@Validated
@Getter
@Setter
public class PayPayProperties {

    @NotBlank(message = "PayPay API key must be configured")
    private String apiKey;

    @NotBlank(message = "PayPay API secret must be configured")
    private String apiSecret;

    @NotBlank(message = "PayPay merchant ID must be configured")
    private String merchantId;

    /**
     * false = sandbox (default), true = production.
     * PayPay sandbox: https://stg.cashier.paypay.ne.jp
     * PayPay production: https://www.paypay.ne.jp
     */
    private boolean productionMode = false;

    /**
     * How long the QR code / deep link stays valid before the buyer must retry.
     * PayPay maximum is 60 minutes; recommended: 30.
     */
    @Positive
    private int paymentTtlMinutes = 30;

    /**
     * HMAC secret used to verify webhook request signatures.
     * Set in PayPay Merchant Dashboard → Webhook settings.
     */
    @NotBlank(message = "PayPay webhook secret must be configured")
    private String webhookSecret;

    /**
     * URL the user is redirected to after completing/cancelling payment.
     * Must match a URL registered in the PayPay Merchant Dashboard.
     */
    @NotBlank
    private String redirectUrl;

    /**
     * URL PayPay POSTs webhook events to.
     * Must be publicly reachable; use ngrok for local development.
     */
    @NotBlank
    private String webhookUrl;

    // ── Derived helpers ───────────────────────────────────────────────────────

    public String getApiBaseUrl() {
        return productionMode
                ? "https://api.paypay.ne.jp"
                : "https://stg-api.paypay.ne.jp";
    }

    public String getCashierBaseUrl() {
        return productionMode
                ? "https://www.paypay.ne.jp"
                : "https://stg.cashier.paypay.ne.jp";
    }
}
