package com.shopee.ecommerce.module.payment.service;

import com.shopee.ecommerce.module.payment.client.dto.PayPayWebhookEvent;
import com.shopee.ecommerce.module.payment.dto.response.InitiatePaymentResponse;
import com.shopee.ecommerce.module.payment.dto.response.PaymentStatusResponse;
import com.shopee.ecommerce.module.payment.dto.response.RefundResponse;

import java.util.UUID;

/**
 * Payment service contract.
 *
 * Flow overview:
 *
 *  1. Buyer clicks "Pay with PayPay"
 *       → initiatePayment()
 *       → returns paymentUrl / deeplink
 *
 *  2. Browser/app opens PayPay, buyer approves
 *       → PayPay POSTs webhook to /api/payments/webhook/paypay
 *       → handleWebhook() called — verifies signature, transitions order
 *
 *  3. As a safety net, the frontend also polls:
 *       → getPaymentStatus()
 *       → if PayPay reports COMPLETED but webhook was missed, we catch up
 *
 *  4. Optional refund via admin panel:
 *       → refundPayment()
 */
public interface PaymentService {

    /**
     * Create a PayPay payment request for an order.
     *
     * Steps:
     *  1. Verify the order is owned by buyerId and in a payable state
     *  2. Reject if a COMPLETED payment already exists for this order
     *  3. Cancel any INITIATED/stale prior payment for this order
     *  4. Build and POST the PayPay create-payment request
     *  5. Persist a Payment row (status = INITIATED)
     *  6. Transition order → AWAITING_PAYMENT
     *  7. Return the PayPay redirect URL and deeplink
     *
     * @throws com.shopee.ecommerce.exception.ResourceNotFoundException   order not found / not owned
     * @throws com.shopee.ecommerce.exception.BusinessException           order already paid or cancelled
     * @throws com.shopee.ecommerce.exception.PaymentGatewayException     PayPay API error
     */
    InitiatePaymentResponse initiatePayment(UUID orderId, UUID buyerId);

    /**
     * Get the current status of a payment, first from DB then from PayPay if stale.
     *
     * This is the polling endpoint. If the DB shows a terminal status it is
     * returned immediately. If still pending, we query PayPay live to detect
     * missed webhooks.
     *
     * @throws com.shopee.ecommerce.exception.ResourceNotFoundException   payment not found
     * @throws com.shopee.ecommerce.exception.AccessDeniedException       payment not owned by buyer
     */
    PaymentStatusResponse getPaymentStatus(UUID orderId, UUID buyerId);

    /**
     * Process a verified PayPay webhook event.
     *
     * Called only after the HMAC signature has been successfully verified
     * by the webhook controller. This method handles business logic:
     *
     *  COMPLETED    → update Payment status, transition Order → PAID
     *  FAILED       → update Payment status, log failure reason
     *  CANCELED     → update Payment status
     *  EXPIRED      → update Payment status
     *
     * This method is idempotent — duplicate webhooks are rejected via
     * the payload hash check in PaymentRepository.
     *
     * @param event        deserialized and HMAC-verified webhook payload
     * @param payloadHash  SHA-256 of the raw request body (for dedup)
     */
    void handleWebhook(PayPayWebhookEvent event, String payloadHash);

    /**
     * Issue a full refund for a completed payment.
     *
     * @throws com.shopee.ecommerce.exception.ResourceNotFoundException  payment not found
     * @throws com.shopee.ecommerce.exception.BusinessException          payment not in COMPLETED state
     * @throws com.shopee.ecommerce.exception.PaymentGatewayException    PayPay refund API error
     */
    RefundResponse refundPayment(UUID orderId, String reason, UUID adminId);

    /**
     * Reconcile the payment status for an order by querying PayPay directly.
     * Used by the scheduler to catch up on missed webhooks.
     *
     * Returns the updated payment status after reconciliation.
     */
    PaymentStatusResponse reconcilePayment(String merchantPaymentId);
}
