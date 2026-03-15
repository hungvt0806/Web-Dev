package com.shopee.ecommerce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Type-safe binding for all custom `app.*` properties.
 * Validated at startup via @ConfigurationPropertiesScan.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final OAuth2 oauth2 = new OAuth2();
    private final Payment payment = new Payment();
    private final Upload upload = new Upload();

    // ── JWT ───────────────────────────────────────────────────
    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long accessTokenExpiryMs;
        private long refreshTokenExpiryMs;
    }

    // ── CORS ──────────────────────────────────────────────────
    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins;
    }

    // ── OAuth2 ────────────────────────────────────────────────
    @Getter
    @Setter
    public static class OAuth2 {
        private List<String> authorizedRedirectUris;
    }

    // ── Payment ───────────────────────────────────────────────
    @Getter
    @Setter
    public static class Payment {
        private final PayPay paypay = new PayPay();
        private final Stripe stripe = new Stripe();

        @Getter
        @Setter
        public static class PayPay {
            private String apiKey;
            private String apiSecret;
            private String merchantId;
            private boolean sandbox;
            private String baseUrl;
        }

        @Getter
        @Setter
        public static class Stripe {
            private String apiKey;
            private String webhookSecret;
            private String currency;
        }
    }

    // ── File Upload ───────────────────────────────────────────
    @Getter
    @Setter
    public static class Upload {
        private String dir;
        private String maxFileSize;
        private List<String> allowedTypes;
    }
}
