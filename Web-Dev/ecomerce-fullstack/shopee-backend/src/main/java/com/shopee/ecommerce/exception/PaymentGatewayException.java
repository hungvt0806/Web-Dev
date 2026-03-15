package com.shopee.ecommerce.exception;

/**
 * Thrown when communication with an external payment gateway fails,
 * or when the gateway returns an unexpected / error response.
 *
 * Examples:
 *  - Network timeout calling PayPay API
 *  - PayPay returns resultInfo.code != "SUCCESS"
 *  - Malformed response body that cannot be parsed
 *  - HMAC signature generation failure
 */
public class PaymentGatewayException extends RuntimeException {

    /** Gateway error with a descriptive message. */
    public PaymentGatewayException(String message) {
        super(message);
    }

    /** Gateway error wrapping a lower-level cause (e.g. IOException, JsonException). */
    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Re-throw with only the original cause (rarely used, kept for completeness). */
    public PaymentGatewayException(Throwable cause) {
        super(cause);
    }
}